package com.amir.buysmart.domain.model

data class ShoppingItem(
    val id: String = "",
    val name: String = "",
    val quantity: String = "",
    val note: String = "",
    val location: ShoppingLocation = ShoppingLocation.SUPERMARKET,
    val customLocation: String = "",
    val type: ItemType = ItemType.ONE_TIME,
    val isBought: Boolean = false,
    val addedBy: String = "",
    val addedByName: String = "",
    val listId: String = "",
    val priority: ItemPriority = ItemPriority.NORMAL,
    val pendingRefill: Boolean = false,
    val imageUrl: String = ""
) {
    /**
     * מפתח קטגוריה אחיד — לקיבוץ ולחיפוש. מבחין בין מובנה ומותאם.
     */
    val categoryKey: String
        get() = if (customLocation.isNotBlank()) "CUSTOM:$customLocation" else location.name
}
