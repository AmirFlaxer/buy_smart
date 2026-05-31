package com.amir.buysmart.domain.util

/** עזר לטיפול במחרוזות כמות (למשל "2", "500 גרם", "3 חבילות"). */
object QuantityUtils {

    private val leadingNumber = Regex("^(\\d+)(.*)$")

    /**
     * מגדיל ב-1 את המספר שבתחילת מחרוזת הכמות, תוך שמירת היחידה.
     * "2" → "3", "500 גרם" → "501 גרם", "" → "2", "חבילה" → "2".
     */
    fun increment(current: String): String {
        if (current.isBlank()) return "2"
        val match = leadingNumber.find(current.trim()) ?: return "2"
        val num = match.groupValues[1].toIntOrNull() ?: return "2"
        return "${num + 1}${match.groupValues[2]}"
    }
}
