package com.amir.buysmart.presentation.screens.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation
import com.amir.buysmart.domain.usecase.FinishShoppingUseCase
import com.amir.buysmart.domain.usecase.GetItemsByLocationUseCase
import com.amir.buysmart.domain.usecase.ToggleItemBoughtUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShoppingUiState(
    val selectedLocation: ShoppingLocation = ShoppingLocation.SUPERMARKET,
    val items: List<ShoppingItem> = emptyList(),
    val isLoading: Boolean = true,
    val finished: Boolean = false
)

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val getItemsByLocation: GetItemsByLocationUseCase,
    private val toggleItemBought: ToggleItemBoughtUseCase,
    private val finishShopping: FinishShoppingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    private var currentListId = ""

    fun init(listId: String) {
        currentListId = listId
        loadItems(listId, _uiState.value.selectedLocation)
    }

    fun selectLocation(location: ShoppingLocation) {
        _uiState.update { it.copy(selectedLocation = location) }
        loadItems(currentListId, location)
    }

    private fun loadItems(listId: String, location: ShoppingLocation) {
        viewModelScope.launch {
            getItemsByLocation(listId, location).collect { items ->
                _uiState.update { it.copy(items = items, isLoading = false) }
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
            finishShopping(currentListId, _uiState.value.selectedLocation)
            _uiState.update { it.copy(finished = true) }
        }
    }
}
