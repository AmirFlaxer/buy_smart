package com.amir.buysmart.presentation.widget

import com.amir.buysmart.domain.model.ShoppingItem

/**
 * הפריטים שמוצגים ב-widget: רק מה שטרם נקנה ואינו ממתין לחידוש,
 * ממוין לפי דחיפות (URGENT ראשון), חתוך לכמות מקסימלית.
 */
fun widgetItems(items: List<ShoppingItem>, max: Int): List<ShoppingItem> =
    items
        .filter { !it.isBought && !it.pendingRefill }
        .sortedBy { it.priority.ordinal }
        .take(max)
