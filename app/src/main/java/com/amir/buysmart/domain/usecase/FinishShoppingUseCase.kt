package com.amir.buysmart.domain.usecase

import com.amir.buysmart.domain.model.ShoppingLocation
import com.amir.buysmart.domain.repository.ItemRepository
import javax.inject.Inject

class FinishShoppingUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    // חד-פעמיים שנקנו → נמחקים. חוזרים שנקנו → isBought=false
    suspend operator fun invoke(listId: String, location: ShoppingLocation) =
        repository.finishShopping(listId, location)
}
