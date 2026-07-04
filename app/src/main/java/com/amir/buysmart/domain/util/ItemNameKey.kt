package com.amir.buysmart.domain.util

/** מנרמל שם פריט למפתח השוואה: trim + lowercase + רווח יחיד בין מילים. */
object ItemNameKey {
    private val multiSpace = Regex("\\s+")
    fun of(name: String): String =
        name.trim().lowercase().replace(multiSpace, " ")

    /** נרמול לשמירה: trim + רווח יחיד, בלי שינוי אותיות. */
    fun collapseSpaces(name: String): String =
        name.trim().replace(multiSpace, " ")
}
