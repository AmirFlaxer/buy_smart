package com.amir.buysmart.domain.usecase

import com.amir.buysmart.domain.repository.ItemRepository
import javax.inject.Inject

class DeleteItemUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    suspend operator fun invoke(itemId: String, listId: String) =
        repository.deleteItem(itemId, listId)
}
