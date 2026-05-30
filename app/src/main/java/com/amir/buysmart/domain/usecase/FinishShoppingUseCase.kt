package com.amir.buysmart.domain.usecase

import com.amir.buysmart.domain.repository.ItemRepository
import javax.inject.Inject

class FinishShoppingUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    // חד-פעמיים שנקנו → נמחקים. חוזרים שנקנו → pendingRefill=true
    suspend operator fun invoke(listId: String, categoryKey: String) =
        repository.finishShopping(listId, categoryKey)
}
