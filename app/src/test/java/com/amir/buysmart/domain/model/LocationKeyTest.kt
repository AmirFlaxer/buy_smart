package com.amir.buysmart.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationKeyTest {

    @Test
    fun `fromKey של קטגוריה מובנית`() {
        val key = LocationKey.fromKey("BAKERY")
        assertTrue(key is LocationKey.BuiltIn)
        assertEquals(ShoppingLocation.BAKERY, (key as LocationKey.BuiltIn).location)
        assertFalse(key.isCustom)
    }

    @Test
    fun `fromKey של מפתח לא חוקי נופל ל-OTHER`() {
        val key = LocationKey.fromKey("NOT_A_REAL_LOCATION")
        assertTrue(key is LocationKey.BuiltIn)
        assertEquals(ShoppingLocation.OTHER, (key as LocationKey.BuiltIn).location)
    }

    @Test
    fun `fromKey של קטגוריה מותאמת`() {
        val key = LocationKey.fromKey("CUSTOM:חומרי בניין")
        assertTrue(key is LocationKey.Custom)
        assertEquals("חומרי בניין", (key as LocationKey.Custom).name)
        assertTrue(key.isCustom)
    }

    @Test
    fun `key של מותאמת הוא round-trip ל-fromKey`() {
        val original = LocationKey.Custom("פיצוצייה")
        val restored = LocationKey.fromKey(original.key)
        assertEquals(original, restored)
    }

    @Test
    fun `fromItem עם customLocation מחזיר Custom`() {
        val item = ShoppingItem(name = "מסמרים", customLocation = "חומרי בניין")
        val key = LocationKey.fromItem(item)
        assertTrue(key is LocationKey.Custom)
        assertEquals("חומרי בניין", (key as LocationKey.Custom).name)
    }

    @Test
    fun `fromItem ללא customLocation מחזיר BuiltIn לפי location`() {
        val item = ShoppingItem(name = "חלב", location = ShoppingLocation.SUPERMARKET)
        val key = LocationKey.fromItem(item)
        assertTrue(key is LocationKey.BuiltIn)
        assertEquals(ShoppingLocation.SUPERMARKET, (key as LocationKey.BuiltIn).location)
    }

    @Test
    fun `displayName ו-emoji נגזרים נכון`() {
        assertEquals("מאפייה", LocationKey.BuiltIn(ShoppingLocation.BAKERY).displayName)
        assertEquals("פיצוצייה", LocationKey.Custom("פיצוצייה").displayName)
    }
}
