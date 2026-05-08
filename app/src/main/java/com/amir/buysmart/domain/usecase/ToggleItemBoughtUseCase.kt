package com.amir.buysmart.domain.usecase

import com.amir.buysmart.domain.repository.ItemRepository
import javax.inject.Inject

class ToggleItemBoughtUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    suspend operator fun invoke(itemId: String, listId: String, isBought: Boolean) =
        repository.toggleBought(itemId, listId, isBought)
}
