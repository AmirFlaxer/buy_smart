package com.amir.buysmart.domain.util

import com.amir.buysmart.domain.model.ItemPriority
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation
import org.junit.Assert.assertEquals
import org.junit.Test

class ItemMergeTest {

    private fun item(
        id: String,
        quantity: String = "",
        note: String = "",
        priority: ItemPriority = ItemPriority.NORMAL,
        type: ItemType = ItemType.ONE_TIME,
        imageUrl: String = ""
    ) = ShoppingItem(
        id = id, name = "חלב", quantity = quantity, note = note,
        location = ShoppingLocation.SUPERMARKET, type = type,
        addedBy = "u", listId = "l", priority = priority, imageUrl = imageUrl
    )

    @Test
    fun `survivor is first item, others are deleted`() {
        val result = ItemMerge.merge(listOf(item("a"), item("b"), item("c")), UnitType.WEIGHT)
        assertEquals("a", result.survivor.id)
        assertEquals(listOf("b", "c"), result.deleteIds)
    }

    @Test
    fun `quantity is merged with larger value`() {
        val result = ItemMerge.merge(listOf(item("a", quantity = "2"), item("b", quantity = "4")), UnitType.WEIGHT)
        assertEquals("4", result.survivor.quantity)
    }

    @Test
    fun `notes are combined uniquely`() {
        val result = ItemMerge.merge(
            listOf(item("a", note = "1%"), item("b", note = "3%"), item("c", note = "1%")),
            UnitType.WEIGHT
        )
        assertEquals("1%, 3%", result.survivor.note)
    }

    @Test
    fun `highest priority wins`() {
        val result = ItemMerge.merge(
            listOf(item("a", priority = ItemPriority.NORMAL), item("b", priority = ItemPriority.URGENT)),
            UnitType.WEIGHT
        )
        assertEquals(ItemPriority.URGENT, result.survivor.priority)
    }

    @Test
    fun `recurring type wins over one-time`() {
        val result = ItemMerge.merge(
            listOf(item("a", type = ItemType.ONE_TIME), item("b", type = ItemType.RECURRING)),
            UnitType.WEIGHT
        )
        assertEquals(ItemType.RECURRING, result.survivor.type)
    }

    @Test
    fun `first non-blank image is kept`() {
        val result = ItemMerge.merge(
            listOf(item("a", imageUrl = ""), item("b", imageUrl = "img1"), item("c", imageUrl = "img2")),
            UnitType.WEIGHT
        )
        assertEquals("img1", result.survivor.imageUrl)
    }
}
