package com.amir.buysmart.domain.util

/** מנרמל שם פריט למפתח השוואה: trim + lowercase + רווח יחיד בין מילים. */
object ItemNameKey {
    private val multiSpace = Regex("\\s+")
    fun of(name: String): String =
        name.trim().lowercase().replace(multiSpace, " ")
}
