package com.amir.buysmart.domain.model

enum class ShoppingLocation(val displayName: String, val emoji: String) {
    SUPERMARKET("סופר", "🛒"),
    DELI("מעדניה", "🥩"),
    GREENGROCER("ירקניה", "🥦"),
    PHARMACY("בית מרקחת", "💊"),
    BAKERY("מאפייה", "🥖"),
    OTHER("אחר", "📦")
}
