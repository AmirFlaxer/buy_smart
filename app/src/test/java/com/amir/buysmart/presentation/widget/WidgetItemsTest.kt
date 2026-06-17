package com.amir.buysmart.presentation.widget

import com.amir.buysmart.domain.model.ItemPriority
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetItemsTest {

    private fun item(
        id: String,
        name: String,
        isBought: Boolean = false,
        pendingRefill: Boolean = false,
        priority: ItemPriority = ItemPriority.NORMAL
    ) = ShoppingItem(
        id = id,
        name = name,
        location = ShoppingLocation.SUPERMARKET,
        type = ItemType.RECURRING,
        isBought = isBought,
        addedBy = "u1",
        listId = "list1",
        priority = priority,
        pendingRefill = pendingRefill
    )

    @Test
    fun `filters out bought and pending refill items`() {
        val items = listOf(
            item("1", "חלב"),
            item("2", "לחם", isBought = true),
            item("3", "ביצים", pendingRefill = true)
        )
        val result = widgetItems(items, max = 12)
        assertEquals(listOf("חלב"), result.map { it.name })
    }

    @Test
    fun `sorts by priority - urgent first`() {
        val items = listOf(
            item("1", "רגיל", priority = ItemPriority.NORMAL),
            item("2", "דחוף", priority = ItemPriority.URGENT),
            item("3", "לא דחוף", priority = ItemPriority.NOT_URGENT)
        )
        val result = widgetItems(items, max = 12)
        assertEquals(listOf("דחוף", "רגיל", "לא דחוף"), result.map { it.name })
    }

    @Test
    fun `caps result to max`() {
        val items = (1..20).map { item(it.toString(), "פריט$it") }
        val result = widgetItems(items, max = 12)
        assertEquals(12, result.size)
    }

    @Test
    fun `returns empty list for empty input`() {
        assertEquals(emptyList<ShoppingItem>(), widgetItems(emptyList(), max = 12))
    }
}
