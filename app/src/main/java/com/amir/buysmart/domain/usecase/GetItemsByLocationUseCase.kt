package com.amir.buysmart.domain.usecase

import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetItemsByLocationUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    operator fun invoke(listId: String, categoryKey: String): Flow<List<ShoppingItem>> =
        repository.getItemsByCategoryKey(listId, categoryKey)
}
