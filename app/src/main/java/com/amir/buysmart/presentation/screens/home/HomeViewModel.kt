package com.amir.buysmart.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amir.buysmart.data.remote.GeminiLocationClassifier
import com.amir.buysmart.domain.model.ItemNotePresets
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingList
import com.amir.buysmart.domain.model.ShoppingLocation
import com.amir.buysmart.domain.repository.ItemRepository
import com.amir.buysmart.domain.repository.ListRepository
import com.amir.buysmart.domain.usecase.AddItemUseCase
import com.amir.buysmart.domain.usecase.DeleteItemUseCase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val activeList: ShoppingList? = null,
    val itemsByLocation: Map<ShoppingLocation, List<ShoppingItem>> = emptyMap(),
    val totalItems: Int = 0,
    val isLoading: Boolean = true,
    val isCreatingList: Boolean = false,
    // QuickAdd
    val quickAddName: String = "",
    val quickAddNote: String = "",
    val quickAddLocation: ShoppingLocation = ShoppingLocation.SUPERMARKET,
    val quickAddLocationManuallySet: Boolean = false,
    val quickAddSuggestions: List<String> = emptyList(),
    val quickAddPresetNotes: List<String> = emptyList(),
    val quickAddDuplicate: ShoppingItem? = null,
    // עריכת פריט
    val editingItem: ShoppingItem? = null,
    val editPresetNotes: List<String> = emptyList(),
    val editDuplicate: ShoppingItem? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val listRepository: ListRepository,
    private val addItemUseCase: AddItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val auth: FirebaseAuth,
    private val geminiClassifier: GeminiLocationClassifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val userId get() = auth.currentUser?.uid ?: ""
    private val displayName get() = auth.currentUser?.displayName
        ?: auth.currentUser?.email?.substringBefore("@") ?: ""

    private var itemsJob: Job? = null
    private var autoCreateAttempted = false
    private var locationJob: Job? = null

    init { loadActiveList() }

    private fun loadActiveList() {
        viewModelScope.launch {
            val storedListId = listRepository.getActiveListId(userId)
            listRepository.getUserLists(userId).collect { lists ->
                val target = lists.find { it.id == storedListId } ?: lists.firstOrNull()
                val prevId = _uiState.value.activeList?.id
                if (target != null) {
                    _uiState.update { it.copy(activeList = target, isLoading = false) }
                    if (prevId != target.id) {
                        if (storedListId != target.id) listRepository.setActiveList(userId, target.id)
                        observeItems(target.id)
                    }
                } else if (!autoCreateAttempted) {
                    autoCreateAttempted = true
                    _uiState.update { it.copy(isLoading = false) }
                    autoCreateList()
                }
            }
        }
    }

    private fun autoCreateList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingList = true) }
            try {
                val inviteCode = (100000..999999).random().toString()
                val list = listRepository.createList(
                    ShoppingList(name = "הרשימה שלנו", ownerId = userId, members = listOf(userId), inviteCode = inviteCode)
                )
                listRepository.setActiveList(userId, list.id)
                _uiState.update { it.copy(activeList = list, isCreatingList = false) }
                observeItems(list.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(isCreatingList = false) }
            }
        }
    }

    private fun observeItems(listId: String) {
        itemsJob?.cancel()
        itemsJob = viewModelScope.launch {
            itemRepository.getItemsForList(listId).collect { items ->
                val grouped = ShoppingLocation.entries.associateWith { loc ->
                    items.filter { it.location == loc }.sortedBy { it.isBought }
                }.filterValues { it.isNotEmpty() }
                _uiState.update { it.copy(itemsByLocation = grouped, totalItems = items.size) }
            }
        }
    }

    // ──── QuickAdd ────

    fun onQuickAddNameChange(name: String) {
        val hintsLoc = ItemNotePresets.suggestLocation(name)
        _uiState.update { state ->
            state.copy(
                quickAddName = name,
                quickAddPresetNotes = ItemNotePresets.getPresetNotes(name),
                quickAddLocation = when {
                    state.quickAddLocationManuallySet -> state.quickAddLocation
                    hintsLoc != null -> hintsLoc
                    else -> ShoppingLocation.SUPERMARKET
                }
            )
        }
        val listId = _uiState.value.activeList?.id ?: return
        if (name.length >= 2) {
            viewModelScope.launch {
                val s = itemRepository.searchItemNames(name, listId).filter { it != name }
                _uiState.update { it.copy(quickAddSuggestions = s) }
            }
        } else {
            _uiState.update { it.copy(quickAddSuggestions = emptyList()) }
        }

        locationJob?.cancel()
        if (name.length >= 3) {
            locationJob = viewModelScope.launch {
                val history = itemRepository.getHistory(name)
                if (history != null && !_uiState.value.quickAddLocationManuallySet) {
                    _uiState.update { state -> state.copy(
                        quickAddLocation = history.location,
                        quickAddNote = if (state.quickAddNote.isBlank() && history.note.isNotBlank()) history.note else state.quickAddNote
                    )}
                    return@launch
                }
                if (hintsLoc == null && !_uiState.value.quickAddLocationManuallySet) {
                    delay(600)
                    val loc = geminiClassifier.classify(name) ?: return@launch
                    if (!_uiState.value.quickAddLocationManuallySet) {
                        _uiState.update { it.copy(quickAddLocation = loc) }
                        itemRepository.saveHistory(name, loc, "", "")
                    }
                }
            }
        }
    }

    fun onQuickAddSuggestionSelected(name: String) {
        val hintsLoc = ItemNotePresets.suggestLocation(name)
        _uiState.update { it.copy(
            quickAddName = name,
            quickAddSuggestions = emptyList(),
            quickAddPresetNotes = ItemNotePresets.getPresetNotes(name),
            quickAddLocation = hintsLoc ?: it.quickAddLocation
        )}
        viewModelScope.launch {
            val history = itemRepository.getHistory(name)
            if (history != null && !_uiState.value.quickAddLocationManuallySet) {
                _uiState.update { state -> state.copy(
                    quickAddLocation = history.location,
                    quickAddNote = if (state.quickAddNote.isBlank() && history.note.isNotBlank()) history.note else state.quickAddNote
                )}
            }
        }
    }

    fun onQuickAddLocationChange(loc: ShoppingLocation) =
        _uiState.update { it.copy(quickAddLocation = loc, quickAddLocationManuallySet = true) }

    fun onQuickAddPresetNoteToggle(preset: String) {
        val parts = _uiState.value.quickAddNote
            .split(", ").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        if (parts.contains(preset)) parts.remove(preset) else parts.add(preset)
        _uiState.update { it.copy(quickAddNote = parts.joinToString(", ")) }
    }

    fun clearQuickAddSuggestions() = _uiState.update { it.copy(quickAddSuggestions = emptyList()) }

    fun quickAdd() {
        val state = _uiState.value
        val listId = state.activeList?.id ?: return
        if (state.quickAddName.isBlank()) return

        // בדיקת כפילות
        val duplicate = state.itemsByLocation.values.flatten()
            .firstOrNull { it.name.trim().equals(state.quickAddName.trim(), ignoreCase = true) }
        if (duplicate != null) {
            _uiState.update { it.copy(quickAddDuplicate = duplicate) }
            return
        }
        doQuickAdd(listId, state)
    }

    private fun doQuickAdd(listId: String, state: HomeUiState) {
        viewModelScope.launch {
            addItemUseCase(ShoppingItem(
                name = state.quickAddName.trim(),
                note = state.quickAddNote.trim(),
                location = state.quickAddLocation,
                type = ItemType.RECURRING,
                addedBy = userId,
                addedByName = displayName,
                listId = listId
            ))
            itemRepository.saveHistory(
                state.quickAddName.trim(),
                state.quickAddLocation,
                state.quickAddNote.trim(),
                ""
            )
            resetQuickAdd()
        }
    }

    fun increaseQuantityFromDuplicate() {
        val state = _uiState.value
        val duplicate = state.quickAddDuplicate ?: return
        val newQty = incrementQuantity(duplicate.quantity)
        viewModelScope.launch {
            itemRepository.updateItem(duplicate.copy(quantity = newQty))
            resetQuickAdd()
        }
    }

    fun addDespiteQuickAddDuplicate() {
        val state = _uiState.value
        val listId = state.activeList?.id ?: return
        _uiState.update { it.copy(quickAddDuplicate = null) }
        doQuickAdd(listId, state)
    }

    fun dismissQuickAddDuplicate() = _uiState.update { it.copy(quickAddDuplicate = null) }

    private fun resetQuickAdd() {
        _uiState.update { it.copy(
            quickAddName = "",
            quickAddNote = "",
            quickAddLocation = ShoppingLocation.SUPERMARKET,
            quickAddLocationManuallySet = false,
            quickAddSuggestions = emptyList(),
            quickAddPresetNotes = emptyList(),
            quickAddDuplicate = null
        )}
    }

    private fun incrementQuantity(current: String): String {
        if (current.isBlank()) return "2"
        return current.trim().toIntOrNull()?.plus(1)?.toString() ?: "2"
    }

    // ──── הצטרפות ────

    fun joinList(code: String) {
        viewModelScope.launch {
            val list = listRepository.joinListByCode(code, userId)
            if (list != null) {
                listRepository.setActiveList(userId, list.id)
                _uiState.update { it.copy(activeList = list) }
                observeItems(list.id)
            }
        }
    }

    fun deleteItem(itemId: String) {
        val listId = _uiState.value.activeList?.id ?: return
        viewModelScope.launch { deleteItemUseCase(itemId, listId) }
    }

    // ──── עריכת פריט ────

    fun startEditItem(item: ShoppingItem) = _uiState.update { it.copy(
        editingItem = item,
        editPresetNotes = ItemNotePresets.getPresetNotes(item.name)
    )}

    fun onEditNameChange(name: String) = _uiState.update { it.copy(
        editingItem = it.editingItem?.copy(name = name),
        editPresetNotes = ItemNotePresets.getPresetNotes(name)
    )}

    fun onEditQuantityChange(q: String) =
        _uiState.update { it.copy(editingItem = it.editingItem?.copy(quantity = q)) }

    fun onEditNoteChange(note: String) =
        _uiState.update { it.copy(editingItem = it.editingItem?.copy(note = note)) }

    fun onEditNotePresetToggle(preset: String) {
        val item = _uiState.value.editingItem ?: return
        val parts = item.note.split(", ").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        if (parts.contains(preset)) parts.remove(preset) else parts.add(preset)
        _uiState.update { it.copy(editingItem = item.copy(note = parts.joinToString(", "))) }
    }

    fun onEditLocationChange(loc: ShoppingLocation) =
        _uiState.update { it.copy(editingItem = it.editingItem?.copy(location = loc)) }

    fun onEditTypeChange(type: ItemType) =
        _uiState.update { it.copy(editingItem = it.editingItem?.copy(type = type)) }

    fun saveEdit() {
        val item = _uiState.value.editingItem ?: return
        _uiState.update { it.copy(editingItem = null, editPresetNotes = emptyList()) }
        viewModelScope.launch { itemRepository.updateItem(item) }
    }

    fun dismissEdit() = _uiState.update { it.copy(editingItem = null, editPresetNotes = emptyList()) }

}
