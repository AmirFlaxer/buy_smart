package com.amir.buysmart.domain.model

/** תמונת מצב של כל מסמכי הפריטים ברשימה (כולל מחוקים). fromCache - הנתונים מהמטמון המקומי ולא מהשרת. */
data class ItemsSnapshot(val items: List<ShoppingItem>, val fromCache: Boolean)

/** מי ביצע מחיקה - נשמר על המסמך המחוק. */
data class Deleter(val uid: String, val name: String)
