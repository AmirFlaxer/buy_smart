package com.amir.buysmart.domain.model

enum class ItemPriority(val displayName: String, val emoji: String) {
    URGENT("דחוף", "❗"),
    NORMAL("רגיל", "●"),
    NOT_URGENT("לא בוער", "○")
}
