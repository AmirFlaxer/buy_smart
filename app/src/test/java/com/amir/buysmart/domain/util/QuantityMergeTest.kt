package com.amir.buysmart.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class QuantityMergeTest {

    @Test
    fun `same count unit takes the larger number`() {
        assertEquals("4", QuantityMerge.merge("2", "4", UnitType.WEIGHT))
        assertEquals("3 יחידות", QuantityMerge.merge("2 יחידות", "3 יחידות", UnitType.WEIGHT))
    }

    @Test
    fun `same weight family normalizes and picks larger`() {
        assertEquals("1.5 ק\"ג", QuantityMerge.merge("500 גרם", "1.5 ק\"ג", UnitType.WEIGHT))
        assertEquals("1.5 ק\"ג", QuantityMerge.merge("1.5 ק\"ג", "500 גרם", UnitType.WEIGHT))
    }

    @Test
    fun `different types follow weight preference`() {
        assertEquals("1.5 ק\"ג", QuantityMerge.merge("5", "1.5 ק\"ג", UnitType.WEIGHT))
        assertEquals("1.5 ק\"ג", QuantityMerge.merge("1.5 ק\"ג", "5", UnitType.WEIGHT))
    }

    @Test
    fun `different types follow count preference`() {
        assertEquals("5", QuantityMerge.merge("5", "1.5 ק\"ג", UnitType.COUNT))
        assertEquals("5", QuantityMerge.merge("1.5 ק\"ג", "5", UnitType.COUNT))
    }

    @Test
    fun `only one parseable wins`() {
        assertEquals("3", QuantityMerge.merge("", "3", UnitType.WEIGHT))
        assertEquals("2", QuantityMerge.merge("חבילה", "2", UnitType.WEIGHT))
    }

    @Test
    fun `neither parseable falls back to first non-blank`() {
        assertEquals("חבילה", QuantityMerge.merge("חבילה", "קופסה", UnitType.WEIGHT))
        assertEquals("קופסה", QuantityMerge.merge("", "קופסה", UnitType.WEIGHT))
        assertEquals("", QuantityMerge.merge("", "", UnitType.WEIGHT))
    }
}
