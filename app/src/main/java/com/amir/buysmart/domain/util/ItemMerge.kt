package com.amir.buysmart.domain.util

import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ShoppingItem

/** תוצאת מיזוג: הפריט השורד (עם הערכים הממוזגים) והמזהים למחיקה. */
data class MergeResult(val survivor: ShoppingItem, val deleteIds: List<String>)

object ItemMerge {

    /** ממזג קבוצת פריטים (אותו שם מנורמל) לפריט אחד. הראשון שורד. */
    fun merge(group: List<ShoppingItem>, preference: UnitType): MergeResult {
        val first = group.first()
        val quantity = group.map { it.quantity }
            .reduce { acc, q -> QuantityMerge.merge(acc, q, preference) }
        val note = group.flatMap { it.note.split(", ") }
            .map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(", ")
        val priority = group.minByOrNull { it.priority.ordinal }!!.priority
        val type = if (group.any { it.type == ItemType.RECURRING }) ItemType.RECURRING else ItemType.ONE_TIME
        val imageUrl = group.firstOrNull { it.imageUrl.isNotBlank() }?.imageUrl ?: ""
        val survivor = first.copy(
            quantity = quantity, note = note, priority = priority, type = type, imageUrl = imageUrl
        )
        return MergeResult(survivor, group.drop(1).map { it.id })
    }
}
