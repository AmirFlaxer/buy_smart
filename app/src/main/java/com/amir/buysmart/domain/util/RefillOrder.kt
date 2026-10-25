package com.amir.buysmart.domain.util

import com.amir.buysmart.domain.model.ShoppingItem

/** סדר התגיות באזור "לחידוש" במסך הראשי: הנקנים הכי הרבה קודם. */
object RefillOrder {
    const val TAG_LIMIT = 8

    /** עד [TAG_LIMIT] פריטים: purchaseCount יורד, ובשוויון לפי שם עולה. */
    fun topTags(items: List<ShoppingItem>): List<ShoppingItem> =
        items.sortedWith(compareByDescending<ShoppingItem> { it.purchaseCount }.thenBy { it.name })
            .take(TAG_LIMIT)
}
