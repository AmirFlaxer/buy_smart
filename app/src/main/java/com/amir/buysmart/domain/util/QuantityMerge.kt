package com.amir.buysmart.domain.util

enum class UnitType { WEIGHT, COUNT }

/**
 * ממזג שתי מחרוזות כמות. כשהיחידות מאותו סוג - בוחר את הגדולה.
 * כשסוג שונה (משקל מול ספירה) - לפי ההעדפה. ראה QuantityMergeTest להתנהגות מלאה.
 */
object QuantityMerge {

    // יחידה טקסטואלית → מכפיל נרמול לסקלה בסיסית. מילים אלו מסומנות WEIGHT.
    private val weightUnits = mapOf(
        "ק\"ג" to 1000.0, "קג" to 1000.0, "קילו" to 1000.0,
        "גרם" to 1.0, "גר" to 1.0, "ג" to 1.0, "ג'" to 1.0,
        "ליטר" to 1000.0, "ל" to 1000.0, "ל'" to 1000.0,
        "מ\"ל" to 1.0, "מל" to 1.0
    )

    private val leadingNumber = Regex("^(\\d+(?:\\.\\d+)?)\\s*(.*)$")

    private data class Parsed(val value: Double, val type: UnitType)

    private fun parse(q: String): Parsed? {
        val m = leadingNumber.find(q.trim()) ?: return null
        val num = m.groupValues[1].toDoubleOrNull() ?: return null
        val unit = m.groupValues[2].trim()
        val multiplier = weightUnits[unit]
        return if (multiplier != null) Parsed(num * multiplier, UnitType.WEIGHT)
        else Parsed(num, UnitType.COUNT)
    }

    fun merge(a: String, b: String, preference: UnitType): String {
        val pa = parse(a)
        val pb = parse(b)
        return when {
            pa != null && pb != null -> when {
                pa.type == pb.type -> if (pa.value >= pb.value) a else b
                else -> if (pa.type == preference) a else b
            }
            pa != null -> a
            pb != null -> b
            a.isNotBlank() -> a
            else -> b
        }
    }
}
