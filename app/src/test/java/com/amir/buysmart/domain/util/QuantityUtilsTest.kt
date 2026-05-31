package com.amir.buysmart.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class QuantityUtilsTest {

    @Test
    fun `מספר פשוט גדל ב-1`() {
        assertEquals("3", QuantityUtils.increment("2"))
        assertEquals("11", QuantityUtils.increment("10"))
    }

    @Test
    fun `מחרוזת ריקה הופכת ל-2`() {
        assertEquals("2", QuantityUtils.increment(""))
        assertEquals("2", QuantityUtils.increment("   "))
    }

    @Test
    fun `מספר עם יחידה — היחידה נשמרת`() {
        assertEquals("501 גרם", QuantityUtils.increment("500 גרם"))
        assertEquals("3 חבילות", QuantityUtils.increment("2 חבילות"))
    }

    @Test
    fun `טקסט ללא מספר מוביל הופך ל-2`() {
        assertEquals("2", QuantityUtils.increment("חבילה"))
        assertEquals("2", QuantityUtils.increment("גרם"))
    }

    @Test
    fun `רווחים מובילים מקוצצים`() {
        assertEquals("3", QuantityUtils.increment("  2  "))
    }
}
