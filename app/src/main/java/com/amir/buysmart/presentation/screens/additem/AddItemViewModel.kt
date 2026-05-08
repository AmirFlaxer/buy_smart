package com.amir.buysmart.presentation.screens.additem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation
import com.amir.buysmart.domain.usecase.AddItemUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddItemUiState(
    val name: String = "",
    val location: ShoppingLocation = ShoppingLocation.SUPERMARKET,
    val type: ItemType = ItemType.ONE_TIME,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val addItemUseCase: AddItemUseCase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState: StateFlow<AddItemUiState> = _uiState

    fun onNameChange(name: String) = _uiState.value.let { _uiState.value = it.copy(name = name) }
    fun onLocationChange(location: ShoppingLocation) = _uiState.value.let { _uiState.value = it.copy(location = location) }
    fun onTypeChange(type: ItemType) = _uiState.value.let { _uiState.value = it.copy(type = type) }

    fun save(listId: String) {
        val state = _uiState.value
        if (state.name.isBlank()) return
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            addItemUseCase(
                ShoppingItem(
                    name = state.name.trim(),
                    location = state.location,
                    type = state.type,
                    addedBy = auth.currentUser?.uid ?: "",
                    listId = listId
                )
            )
            _uiState.value = state.copy(saved = true)
        }
    }
}
