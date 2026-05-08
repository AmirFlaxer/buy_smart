package com.amir.buysmart.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingList
import com.amir.buysmart.domain.model.ShoppingLocation
import com.amir.buysmart.domain.repository.ItemRepository
import com.amir.buysmart.domain.repository.ListRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val activeList: ShoppingList? = null,
    val itemsByLocation: Map<ShoppingLocation, List<ShoppingItem>> = emptyMap(),
    val isLoading: Boolean = true,
    val showJoinDialog: Boolean = false,
    val showCreateDialog: Boolean = false,
    val inviteCode: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val listRepository: ListRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val userId get() = auth.currentUser?.uid ?: ""

    init {
        loadActiveList()
    }

    private fun loadActiveList() {
        viewModelScope.launch {
            val listId = listRepository.getActiveListId(userId)
            if (listId != null) {
                observeItems(listId)
            } else {
                listRepository.getUserLists(userId).collect { lists ->
                    if (lists.isNotEmpty()) {
                        val first = lists.first()
                        listRepository.setActiveList(userId, first.id)
                        _uiState.update { it.copy(activeList = first) }
                        observeItems(first.id)
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    private fun observeItems(listId: String) {
        viewModelScope.launch {
            itemRepository.getItemsForList(listId).collect { items ->
                val grouped = ShoppingLocation.entries.associateWith { loc ->
                    items.filter { it.location == loc }.sortedBy { it.isBought }
                }.filterValues { it.isNotEmpty() }
                _uiState.update { it.copy(itemsByLocation = grouped, isLoading = false) }
            }
        }
    }

    fun createList(name: String) {
        viewModelScope.launch {
            val inviteCode = (100000..999999).random().toString()
            val list = listRepository.createList(
                ShoppingList(name = name, ownerId = userId, members = listOf(userId), inviteCode = inviteCode)
            )
            listRepository.setActiveList(userId, list.id)
            _uiState.update { it.copy(activeList = list, showCreateDialog = false) }
            observeItems(list.id)
        }
    }

    fun joinList(code: String) {
        viewModelScope.launch {
            val list = listRepository.joinListByCode(code, userId)
            if (list != null) {
                listRepository.setActiveList(userId, list.id)
                _uiState.update { it.copy(activeList = list, showJoinDialog = false) }
                observeItems(list.id)
            }
        }
    }

    fun showJoinDialog() = _uiState.update { it.copy(showJoinDialog = true) }
    fun hideJoinDialog() = _uiState.update { it.copy(showJoinDialog = false) }
    fun showCreateDialog() = _uiState.update { it.copy(showCreateDialog = true) }
    fun hideCreateDialog() = _uiState.update { it.copy(showCreateDialog = false) }
}
