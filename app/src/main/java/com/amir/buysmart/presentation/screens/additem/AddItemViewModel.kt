package com.amir.buysmart.presentation.screens.additem

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amir.buysmart.data.remote.GeminiLocationClassifier
import com.amir.buysmart.data.remote.ImageUploader
import com.amir.buysmart.domain.model.ItemNotePresets
import com.amir.buysmart.domain.model.ItemPriority
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.LocationKey
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation
import com.amir.buysmart.domain.repository.ItemRepository
import com.amir.buysmart.domain.repository.ListRepository
import com.amir.buysmart.domain.usecase.AddItemUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddItemUiState(
    val name: String = "",
    val quantity: String = "",
    val note: String = "",
    val location: ShoppingLocation = ShoppingLocation.SUPERMARKET,
    val customLocation: String = "",
    val locationManuallySet: Boolean = false,
    val type: ItemType = ItemType.RECURRING,
    val priority: ItemPriority = ItemPriority.NORMAL,
    val suggestions: List<String> = emptyList(),
    val presetNotes: List<String> = emptyList(),
    val duplicateItem: ShoppingItem? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val customLocations: List<String> = emptyList(),
    /** URL הסופי של תמונה (אחרי העלאה) */
    val imageUrl: String = "",
    /** Uri מקומי בזמן העלאה — לתצוגה מקדימה */
    val pendingImageUri: Uri? = null,
    val isUploadingImage: Boolean = false
) {
    val selectedKey: LocationKey
        get() = if (customLocation.isNotBlank()) LocationKey.Custom(customLocation)
                else LocationKey.BuiltIn(location)
}

@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val addItemUseCase: AddItemUseCase,
    private val itemRepository: ItemRepository,
    private val listRepository: ListRepository,
    private val imageUploader: ImageUploader,
    private val auth: FirebaseAuth,
    private val geminiClassifier: GeminiLocationClassifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState: StateFlow<AddItemUiState> = _uiState

    private var listId = ""
    private var locationJob: Job? = null
    private var listObserveJob: Job? = null

    fun setListId(id: String) {
        listId = id
        listObserveJob?.cancel()
        listObserveJob = viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            listRepository.getUserLists(userId).collect { lists ->
                val list = lists.find { it.id == id }
                _uiState.update { it.copy(customLocations = list?.customLocations ?: emptyList()) }
            }
        }
    }

    fun onNameChange(name: String) {
        val hintsLoc = if (!_uiState.value.locationManuallySet)
            ItemNotePresets.suggestLocation(name) else null
        _uiState.update { state ->
            state.copy(
                name = name,
                location = if (!state.locationManuallySet) hintsLoc ?: state.location else state.location,
                presetNotes = ItemNotePresets.getPresetNotes(name)
            )
        }
        if (name.length >= 2 && listId.isNotBlank()) {
            viewModelScope.launch {
                val s = itemRepository.searchItemNames(name, listId).filter { it != name }
                _uiState.update { it.copy(suggestions = s) }
            }
        } else {
            _uiState.update { it.copy(suggestions = emptyList()) }
        }

        locationJob?.cancel()
        if (name.length >= 3) {
            locationJob = viewModelScope.launch {
                // hints סופיים — לא דורסים עם History או Gemini
                if (hintsLoc != null) return@launch
                val history = itemRepository.getHistory(name)
                if (history != null && !_uiState.value.locationManuallySet) {
                    _uiState.update { state -> state.copy(
                        location = history.location,
                        note = if (state.note.isBlank() && history.note.isNotBlank()) history.note else state.note,
                        quantity = if (state.quantity.isBlank() && history.quantity.isNotBlank()) history.quantity else state.quantity
                    )}
                    return@launch
                }
                if (!_uiState.value.locationManuallySet) {
                    delay(600)
                    val loc = geminiClassifier.classify(name) ?: return@launch
                    if (!_uiState.value.locationManuallySet) {
                        _uiState.update { it.copy(location = loc) }
                        itemRepository.saveHistory(name, loc, "", "")
                    }
                }
            }
        }
    }

    fun onSuggestionSelected(name: String) {
        val hintsLoc = if (!_uiState.value.locationManuallySet)
            ItemNotePresets.suggestLocation(name) else null
        _uiState.update { it.copy(
            name = name,
            suggestions = emptyList(),
            presetNotes = ItemNotePresets.getPresetNotes(name),
            location = hintsLoc ?: it.location
        )}
        viewModelScope.launch {
            val history = itemRepository.getHistory(name)
            if (history != null && !_uiState.value.locationManuallySet) {
                _uiState.update { state -> state.copy(
                    location = history.location,
                    note = if (state.note.isBlank() && history.note.isNotBlank()) history.note else state.note,
                    quantity = if (state.quantity.isBlank() && history.quantity.isNotBlank()) history.quantity else state.quantity
                )}
            }
        }
    }

    fun clearSuggestions() = _uiState.update { it.copy(suggestions = emptyList()) }
    fun onQuantityChange(q: String) = _uiState.update { it.copy(quantity = q) }
    fun onNoteChange(note: String) = _uiState.update { it.copy(note = note) }

    fun onPresetNoteToggle(preset: String) {
        val parts = _uiState.value.note
            .split(", ").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        if (parts.contains(preset)) parts.remove(preset) else parts.add(preset)
        _uiState.update { it.copy(note = parts.joinToString(", ")) }
    }

    fun onLocationKeyChange(key: LocationKey) {
        when (key) {
            is LocationKey.BuiltIn -> _uiState.update { it.copy(
                location = key.location,
                customLocation = "",
                locationManuallySet = true
            )}
            is LocationKey.Custom -> _uiState.update { it.copy(
                customLocation = key.name,
                locationManuallySet = true
            )}
        }
    }

    fun addCustomLocation(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank() || listId.isBlank()) return
        viewModelScope.launch {
            listRepository.addCustomLocation(listId, trimmed)
        }
    }

    fun removeCustomLocation(name: String) {
        if (listId.isBlank()) return
        viewModelScope.launch {
            listRepository.removeCustomLocation(listId, name)
        }
    }

    fun onImagePicked(context: Context, uri: Uri) {
        _uiState.update { it.copy(pendingImageUri = uri, isUploadingImage = true) }
        viewModelScope.launch {
            val url = imageUploader.uploadItemImage(context, listId, uri)
            _uiState.update { it.copy(
                imageUrl = url ?: it.imageUrl,
                isUploadingImage = false,
                pendingImageUri = if (url != null) null else it.pendingImageUri
            )}
        }
    }

    fun removeImage() {
        _uiState.update { it.copy(imageUrl = "", pendingImageUri = null, isUploadingImage = false) }
    }

    fun onTypeChange(type: ItemType) = _uiState.update { it.copy(type = type) }
    fun onPriorityChange(priority: ItemPriority) = _uiState.update { it.copy(priority = priority) }

    fun save(listId: String) {
        val state = _uiState.value
        if (state.name.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val duplicate = itemRepository.getItemByName(state.name.trim(), listId)
            if (duplicate != null) {
                _uiState.update { it.copy(isSaving = false, duplicateItem = duplicate) }
                return@launch
            }
            doSave(listId, state)
        }
    }

    fun saveAsDuplicate(listId: String) {
        val state = _uiState.value
        _uiState.update { it.copy(duplicateItem = null, isSaving = true) }
        viewModelScope.launch { doSave(listId, state) }
    }

    fun increaseQuantityOfDuplicate() {
        val duplicate = _uiState.value.duplicateItem ?: return
        val newQty = incrementQuantity(duplicate.quantity)
        viewModelScope.launch {
            itemRepository.updateItem(duplicate.copy(quantity = newQty))
            _uiState.update { it.copy(saved = true, duplicateItem = null) }
        }
    }

    private fun incrementQuantity(current: String): String {
        if (current.isBlank()) return "2"
        val match = Regex("^(\\d+)(.*)$").find(current.trim()) ?: return "2"
        val num = match.groupValues[1].toIntOrNull() ?: return "2"
        return "${num + 1}${match.groupValues[2]}"
    }

    fun dismissDuplicateDialog() = _uiState.update { it.copy(duplicateItem = null) }

    private suspend fun doSave(listId: String, state: AddItemUiState) {
        val displayName = auth.currentUser?.displayName
            ?: auth.currentUser?.email?.substringBefore("@") ?: ""
        addItemUseCase(ShoppingItem(
            name = state.name.trim(),
            quantity = state.quantity.trim(),
            note = state.note.trim(),
            location = state.location,
            customLocation = state.customLocation,
            type = state.type,
            priority = state.priority,
            addedBy = auth.currentUser?.uid ?: "",
            addedByName = displayName,
            listId = listId,
            imageUrl = state.imageUrl
        ))
        // saveHistory רק לקטגוריה מובנית
        if (state.customLocation.isBlank()) {
            itemRepository.saveHistory(
                state.name.trim(),
                state.location,
                state.note.trim(),
                state.quantity.trim()
            )
        }
        _uiState.update { it.copy(saved = true, isSaving = false) }
    }
}
