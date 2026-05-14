package com.amir.buysmart.domain.model

enum class ShoppingLocation(val displayName: String, val emoji: String) {
    SUPERMARKET("סופר", "🛒"),
    DELI("מזון מוכן", "🥡"),
    GREENGROCER("ירקניה", "🥦"),
    PHARMACY("בית מרקחת", "💊"),
    BAKERY("מאפייה", "🥖"),
    OTHER("אחר", "📦")
}
