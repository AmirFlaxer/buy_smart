package com.amir.buysmart.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ItemNameKeyTest {

    @Test
    fun `trims and lowercases`() {
        assertEquals("חלב", ItemNameKey.of("  חלב "))
        assertEquals("milk", ItemNameKey.of("MILK"))
    }

    @Test
    fun `collapses internal double spaces`() {
        assertEquals("חלב עיזים", ItemNameKey.of("חלב   עיזים"))
    }

    @Test
    fun `equal names produce equal keys`() {
        assertEquals(ItemNameKey.of("חלב "), ItemNameKey.of("חלב"))
    }
}
