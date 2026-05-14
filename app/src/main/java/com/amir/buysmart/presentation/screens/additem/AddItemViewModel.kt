package com.amir.buysmart.presentation.screens.additem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amir.buysmart.data.remote.GeminiLocationClassifier
import com.amir.buysmart.domain.model.ItemNotePresets
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation
import com.amir.buysmart.domain.repository.ItemRepository
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
    val locationManuallySet: Boolean = false,
    val type: ItemType = ItemType.RECURRING,
    val suggestions: List<String> = emptyList(),
    val presetNotes: List<String> = emptyList(),
    val duplicateItem: ShoppingItem? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val addItemUseCase: AddItemUseCase,
    private val itemRepository: ItemRepository,
    private val auth: FirebaseAuth,
    private val geminiClassifier: GeminiLocationClassifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState: StateFlow<AddItemUiState> = _uiState

    private var listId = ""
    private var locationJob: Job? = null

    fun setListId(id: String) { listId = id }

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
                val history = itemRepository.getHistory(name)
                if (history != null && !_uiState.value.locationManuallySet) {
                    _uiState.update { state -> state.copy(
                        location = history.location,
                        note = if (state.note.isBlank() && history.note.isNotBlank()) history.note else state.note,
                        quantity = if (state.quantity.isBlank() && history.quantity.isNotBlank()) history.quantity else state.quantity
                    )}
                    return@launch
                }
                if (hintsLoc == null && !_uiState.value.locationManuallySet) {
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

    fun onLocationChange(location: ShoppingLocation) =
        _uiState.update { it.copy(location = location, locationManuallySet = true) }

    fun onTypeChange(type: ItemType) = _uiState.update { it.copy(type = type) }

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
        val newQty = if (duplicate.quantity.isBlank()) "2"
                     else duplicate.quantity.trim().toIntOrNull()?.plus(1)?.toString() ?: "2"
        viewModelScope.launch {
            itemRepository.updateItem(duplicate.copy(quantity = newQty))
            _uiState.update { it.copy(saved = true, duplicateItem = null) }
        }
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
            type = state.type,
            addedBy = auth.currentUser?.uid ?: "",
            addedByName = displayName,
            listId = listId
        ))
        itemRepository.saveHistory(
            state.name.trim(),
            state.location,
            state.note.trim(),
            state.quantity.trim()
        )
        _uiState.update { it.copy(saved = true, isSaving = false) }
    }
}
