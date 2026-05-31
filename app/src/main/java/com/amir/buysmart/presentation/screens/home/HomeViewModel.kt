package com.amir.buysmart.presentation.screens.home

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
import com.amir.buysmart.domain.model.ShoppingList
import com.amir.buysmart.domain.model.ShoppingLocation
import com.amir.buysmart.domain.repository.ItemRepository
import com.amir.buysmart.domain.repository.ListRepository
import com.amir.buysmart.notification.ItemNotificationHelper
import com.amir.buysmart.domain.util.QuantityUtils
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
    val itemsByCategory: Map<LocationKey, List<ShoppingItem>> = emptyMap(),
    val pendingRefillItems: List<ShoppingItem> = emptyList(),
    val totalItems: Int = 0,
    val isLoading: Boolean = true,
    val isCreatingList: Boolean = false,
    // QuickAdd
    val quickAddName: String = "",
    val quickAddNote: String = "",
    val quickAddLocation: ShoppingLocation = ShoppingLocation.SUPERMARKET,
    val quickAddCustomLocation: String = "",
    val quickAddLocationManuallySet: Boolean = false,
    val quickAddPriority: ItemPriority = ItemPriority.NORMAL,
    val quickAddSuggestions: List<String> = emptyList(),
    val quickAddPresetNotes: List<String> = emptyList(),
    val quickAddDuplicate: ShoppingItem? = null,
    // עריכת פריט
    val editingItem: ShoppingItem? = null,
    val editPresetNotes: List<String> = emptyList(),
    val editDuplicate: ShoppingItem? = null,
    val editPendingImageUri: Uri? = null,
    val editIsUploadingImage: Boolean = false,
    // מחיקה אחרונה — לטובת undo
    val recentlyDeleted: ShoppingItem? = null,
    // הודעת שגיאה חד-פעמית להצגה ב-Snackbar
    val errorMessage: String? = null
) {
    val quickAddSelectedKey: LocationKey
        get() = if (quickAddCustomLocation.isNotBlank()) LocationKey.Custom(quickAddCustomLocation)
                else LocationKey.BuiltIn(quickAddLocation)

    val customLocations: List<String>
        get() = activeList?.customLocations ?: emptyList()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val listRepository: ListRepository,
    private val addItemUseCase: AddItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val imageUploader: ImageUploader,
    private val auth: FirebaseAuth,
    private val geminiClassifier: GeminiLocationClassifier,
    private val notificationHelper: ItemNotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val userId get() = auth.currentUser?.uid ?: ""
    private val displayName get() = auth.currentUser?.displayName
        ?: auth.currentUser?.email?.substringBefore("@") ?: ""

    private var itemsJob: Job? = null
    private var autoCreateAttempted = false
    private var locationJob: Job? = null
    // מזהי הפריטים הידועים — לזיהוי פריטים חדשים לצורך התראה. null = טרם נטען
    private var knownItemIds: Set<String>? = null

    init { loadActiveList() }

    private fun loadActiveList() {
        viewModelScope.launch {
            listRepository.getUserLists(userId).collect { lists ->
                val storedListId = listRepository.getActiveListId(userId)
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
                _uiState.update { it.copy(isCreatingList = false, errorMessage = "יצירת הרשימה נכשלה, נסה שוב") }
            }
        }
    }

    private fun observeItems(listId: String) {
        itemsJob?.cancel()
        knownItemIds = null
        itemsJob = viewModelScope.launch {
            itemRepository.getItemsForList(listId).collect { items ->
                notifyNewItems(items)
                val activeItems = items.filter { !it.pendingRefill }
                val grouped: Map<LocationKey, List<ShoppingItem>> = activeItems
                    .groupBy { LocationKey.fromItem(it) }
                    .mapValues { (_, list) ->
                        list.sortedWith(compareBy({ it.isBought }, { it.priority.ordinal }))
                    }
                val pending = items.filter { it.pendingRefill }
                _uiState.update { it.copy(
                    itemsByCategory = grouped,
                    pendingRefillItems = pending,
                    totalItems = activeItems.size
                )}
            }
        }
    }

    /** מציג התראה מקומית על פריטים שנוספו ע"י משתמש אחר מאז הסנכרון הקודם. */
    private fun notifyNewItems(items: List<ShoppingItem>) {
        val currentIds = items.map { it.id }.toSet()
        val known = knownItemIds
        if (known != null) {
            items.filter { it.id !in known && it.addedBy.isNotBlank() && it.addedBy != userId }
                .forEach { item ->
                    notificationHelper.showItemAdded(
                        title = _uiState.value.activeList?.name ?: "רשימת קניות",
                        body = "${item.addedByName.ifBlank { "מישהו" }} הוסיף: ${item.name}",
                        dedupeId = item.id.hashCode()
                    )
                }
        }
        knownItemIds = currentIds
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
                if (hintsLoc != null) return@launch
                val history = itemRepository.getHistory(name)
                if (history != null && !_uiState.value.quickAddLocationManuallySet) {
                    _uiState.update { state -> state.copy(
                        quickAddLocation = history.location,
                        quickAddNote = if (state.quickAddNote.isBlank() && history.note.isNotBlank()) history.note else state.quickAddNote
                    )}
                    return@launch
                }
                if (!_uiState.value.quickAddLocationManuallySet) {
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

    fun onQuickAddLocationKeyChange(key: LocationKey) {
        when (key) {
            is LocationKey.BuiltIn -> _uiState.update { it.copy(
                quickAddLocation = key.location,
                quickAddCustomLocation = "",
                quickAddLocationManuallySet = true
            )}
            is LocationKey.Custom -> _uiState.update { it.copy(
                quickAddCustomLocation = key.name,
                quickAddLocationManuallySet = true
            )}
        }
    }

    fun onQuickAddPriorityChange(priority: ItemPriority) =
        _uiState.update { it.copy(quickAddPriority = priority) }

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

        val duplicate = state.itemsByCategory.values.flatten()
            .firstOrNull { it.name.trim().equals(state.quickAddName.trim(), ignoreCase = true) }
        if (duplicate != null) {
            _uiState.update { it.copy(quickAddDuplicate = duplicate) }
            return
        }
        doQuickAdd(listId, state)
    }

    private fun doQuickAdd(listId: String, state: HomeUiState) {
        viewModelScope.launch {
            try {
                addItemUseCase(ShoppingItem(
                    name = state.quickAddName.trim(),
                    note = state.quickAddNote.trim(),
                    location = state.quickAddLocation,
                    customLocation = state.quickAddCustomLocation,
                    type = ItemType.RECURRING,
                    priority = state.quickAddPriority,
                    addedBy = userId,
                    addedByName = displayName,
                    listId = listId
                ))
                // saveHistory רק לקטגוריה מובנית — היסטוריה אינה מתאימה למותאמות אישית
                if (state.quickAddCustomLocation.isBlank()) {
                    itemRepository.saveHistory(
                        state.quickAddName.trim(),
                        state.quickAddLocation,
                        state.quickAddNote.trim(),
                        ""
                    )
                }
                resetQuickAdd()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "הוספת הפריט נכשלה, נסה שוב") }
            }
        }
    }

    fun increaseQuantityFromDuplicate() {
        val state = _uiState.value
        val duplicate = state.quickAddDuplicate ?: return
        val newQty = QuantityUtils.increment(duplicate.quantity)
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
            quickAddCustomLocation = "",
            quickAddLocationManuallySet = false,
            quickAddPriority = ItemPriority.NORMAL,
            quickAddSuggestions = emptyList(),
            quickAddPresetNotes = emptyList(),
            quickAddDuplicate = null
        )}
    }

    // ──── הצטרפות / עזיבה ────

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

    fun leaveList() {
        val list = _uiState.value.activeList ?: return
        viewModelScope.launch {
            itemsJob?.cancel()
            listRepository.leaveList(userId, list.id)
            autoCreateAttempted = false
            _uiState.update { it.copy(
                activeList = null,
                itemsByCategory = emptyMap(),
                pendingRefillItems = emptyList(),
                totalItems = 0,
                isLoading = true
            )}
        }
    }

    // ──── קטגוריות מותאמות אישית ────

    fun addCustomLocation(name: String) {
        val listId = _uiState.value.activeList?.id ?: return
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            listRepository.addCustomLocation(listId, trimmed)
        }
    }

    fun removeCustomLocation(name: String) {
        val listId = _uiState.value.activeList?.id ?: return
        viewModelScope.launch {
            listRepository.removeCustomLocation(listId, name)
        }
    }

    // ──── מחיקה ────

    fun deleteItem(itemId: String) {
        val listId = _uiState.value.activeList?.id ?: return
        viewModelScope.launch { deleteItemUseCase(itemId, listId) }
    }

    fun deleteItemWithUndo(item: ShoppingItem) {
        val listId = _uiState.value.activeList?.id ?: return
        _uiState.update { it.copy(recentlyDeleted = item) }
        viewModelScope.launch { deleteItemUseCase(item.id, listId) }
    }

    fun undoDelete() {
        val item = _uiState.value.recentlyDeleted ?: return
        _uiState.update { it.copy(recentlyDeleted = null) }
        viewModelScope.launch {
            // התמונה לא נמחקה מ-Storage בזמן חלון ה-undo, כך שה-imageUrl עדיין תקף
            addItemUseCase(item.copy(id = ""))
        }
    }

    // התמונה מאוחסנת בתוך מסמך הפריט (base64), ולכן נמחקת יחד איתו — אין צורך בניקוי נפרד.
    fun clearRecentlyDeleted() = _uiState.update { it.copy(recentlyDeleted = null) }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

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

    fun onEditLocationKeyChange(key: LocationKey) {
        val item = _uiState.value.editingItem ?: return
        val updated = when (key) {
            is LocationKey.BuiltIn -> item.copy(location = key.location, customLocation = "")
            is LocationKey.Custom -> item.copy(customLocation = key.name)
        }
        _uiState.update { it.copy(editingItem = updated) }
    }

    fun onEditTypeChange(type: ItemType) =
        _uiState.update { it.copy(editingItem = it.editingItem?.copy(type = type)) }

    fun onEditPriorityChange(priority: ItemPriority) =
        _uiState.update { it.copy(editingItem = it.editingItem?.copy(priority = priority)) }

    fun onEditImagePicked(context: Context, uri: Uri) {
        _uiState.update { it.copy(editPendingImageUri = uri, editIsUploadingImage = true) }
        viewModelScope.launch {
            // התמונה מקודדת ל-base64 ונשמרת ישירות במסמך הפריט ב-Firestore
            val encoded = imageUploader.encodeItemImage(context, uri)
            _uiState.update { state ->
                state.copy(
                    editingItem = if (encoded != null) state.editingItem?.copy(imageUrl = encoded) else state.editingItem,
                    editPendingImageUri = if (encoded != null) null else state.editPendingImageUri,
                    editIsUploadingImage = false,
                    errorMessage = if (encoded == null) "עיבוד התמונה נכשל, נסה שוב" else state.errorMessage
                )
            }
        }
    }

    fun onEditImageRemoved() {
        _uiState.update { it.copy(
            editingItem = it.editingItem?.copy(imageUrl = ""),
            editPendingImageUri = null,
            editIsUploadingImage = false
        )}
    }

    fun approvePendingRefill(item: ShoppingItem) {
        viewModelScope.launch { itemRepository.approvePendingRefill(item) }
    }

    fun saveEdit() {
        val item = _uiState.value.editingItem ?: return
        _uiState.update { it.copy(
            editingItem = null,
            editPresetNotes = emptyList(),
            editPendingImageUri = null,
            editIsUploadingImage = false
        )}
        viewModelScope.launch { itemRepository.updateItem(item) }
    }

    fun dismissEdit() = _uiState.update { it.copy(
        editingItem = null,
        editPresetNotes = emptyList(),
        editPendingImageUri = null,
        editIsUploadingImage = false
    )}
}
