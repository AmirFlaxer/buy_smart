package com.amir.buysmart.domain.usecase

import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.repository.ItemRepository
import javax.inject.Inject

class AddItemUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    suspend operator fun invoke(item: ShoppingItem) = repository.addItem(item)
}
