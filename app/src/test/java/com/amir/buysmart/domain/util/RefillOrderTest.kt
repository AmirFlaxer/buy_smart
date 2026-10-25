package com.amir.buysmart.domain.util

import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RefillOrderTest {

    private fun item(name: String, count: Int = 0) = ShoppingItem(
        id = name, name = name, location = ShoppingLocation.SUPERMARKET,
        addedBy = "u", listId = "l", purchaseCount = count
    )

    @Test
    fun `orders by purchaseCount descending`() {
        val result = RefillOrder.topTags(listOf(item("א", 1), item("ב", 5), item("ג", 3)))
        assertEquals(listOf("ב", "ג", "א"), result.map { it.name })
    }

    @Test
    fun `ties broken by name ascending`() {
        val result = RefillOrder.topTags(listOf(item("גלידה"), item("בננות")))
        assertEquals(listOf("בננות", "גלידה"), result.map { it.name })
    }

    @Test
    fun `returns at most 8`() {
        val result = RefillOrder.topTags((1..12).map { item("פריט$it", it) })
        assertEquals(8, result.size)
    }

    @Test
    fun `empty input gives empty output`() {
        assertTrue(RefillOrder.topTags(emptyList()).isEmpty())
    }
}
