package com.amir.buysmart.presentation.screens.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amir.buysmart.domain.model.LocationKey
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation
import com.amir.buysmart.domain.repository.ListRepository
import com.amir.buysmart.domain.usecase.FinishShoppingUseCase
import com.amir.buysmart.domain.usecase.GetItemsByLocationUseCase
import com.amir.buysmart.domain.usecase.ToggleItemBoughtUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShoppingUiState(
    val selectedKey: LocationKey = LocationKey.BuiltIn(ShoppingLocation.SUPERMARKET),
    val items: List<ShoppingItem> = emptyList(),
    val customLocations: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val finished: Boolean = false,
    /** קטגוריה מותאמת אישית שזה עתה הסתיימה — לשאלה אם למחוק */
    val justFinishedCustom: String? = null
)

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val getItemsByLocation: GetItemsByLocationUseCase,
    private val toggleItemBought: ToggleItemBoughtUseCase,
    private val finishShopping: FinishShoppingUseCase,
    private val listRepository: ListRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    private var currentListId = ""
    private var itemsJob: Job? = null
    private var listJob: Job? = null

    fun init(listId: String) {
        currentListId = listId
        observeList(listId)
        loadItems(listId, _uiState.value.selectedKey)
    }

    private fun observeList(listId: String) {
        listJob?.cancel()
        listJob = viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            listRepository.getUserLists(userId).collect { lists ->
                val list = lists.find { it.id == listId }
                _uiState.update { it.copy(customLocations = list?.customLocations ?: emptyList()) }
            }
        }
    }

    fun selectLocationKey(key: LocationKey) {
        _uiState.update { it.copy(selectedKey = key) }
        loadItems(currentListId, key)
    }

    private fun loadItems(listId: String, key: LocationKey) {
        itemsJob?.cancel()
        itemsJob = viewModelScope.launch {
            getItemsByLocation(listId, key.key).collect { items ->
                _uiState.update { it.copy(
                    items = items.sortedWith(compareBy({ it.isBought }, { it.priority.ordinal })),
                    isLoading = false
                )}
            }
        }
    }

    fun toggleBought(item: ShoppingItem) {
        viewModelScope.launch {
            toggleItemBought(item.id, currentListId, !item.isBought)
        }
    }

    fun finishShopping() {
        viewModelScope.launch {
            val key = _uiState.value.selectedKey
            finishShopping(currentListId, key.key)
            // אם זו קטגוריה מותאמת — להציג שאלה למחוק לפני יציאה
            if (key is LocationKey.Custom) {
                _uiState.update { it.copy(justFinishedCustom = key.name) }
            } else {
                _uiState.update { it.copy(finished = true) }
            }
        }
    }

    fun confirmDeleteFinishedCustom() {
        val name = _uiState.value.justFinishedCustom ?: return
        viewModelScope.launch {
            listRepository.removeCustomLocation(currentListId, name)
            _uiState.update { it.copy(justFinishedCustom = null, finished = true) }
        }
    }

    fun dismissDeleteFinishedCustom() {
        _uiState.update { it.copy(justFinishedCustom = null, finished = true) }
    }
}
