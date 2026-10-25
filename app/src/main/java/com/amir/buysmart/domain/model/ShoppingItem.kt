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
    val imageUrl: String = "",
    // מחיקה רכה: פריט עם deletedAtMillis אינו נמחק פיזית, רק מוסתר מהרשימה
    val deletedAtMillis: Long? = null,
    val deleteReason: String = "",
    val deletedByName: String = "",
    val autoRestoredAtMillis: Long? = null,
    val purchaseCount: Int = 0
) {
    val isDeleted: Boolean get() = deletedAtMillis != null


    /**
     * מפתח קטגוריה אחיד — לקיבוץ ולחיפוש. מבחין בין מובנה ומותאם.
     */
    val categoryKey: String
        get() = if (customLocation.isNotBlank()) "CUSTOM:$customLocation" else location.name
}

/** סיבת המחיקה הרכה - הערכים נשמרים כמחרוזות במסמך Firestore. */
object DeleteReason {
    const val MANUAL = "manual"
    const val FINISH = "finish"
    const val MERGE = "merge"
}
