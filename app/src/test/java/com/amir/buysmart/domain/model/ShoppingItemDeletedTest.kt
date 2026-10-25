package com.amir.buysmart.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingItemDeletedTest {

    @Test
    fun `isDeleted false when deletedAtMillis null`() {
        assertFalse(ShoppingItem(deletedAtMillis = null).isDeleted)
    }

    @Test
    fun `isDeleted true when deletedAtMillis set`() {
        assertTrue(ShoppingItem(deletedAtMillis = 1L).isDeleted)
    }

    @Test
    fun `DeleteReason values match spec`() {
        assertEquals("manual", DeleteReason.MANUAL)
        assertEquals("finish", DeleteReason.FINISH)
        assertEquals("merge", DeleteReason.MERGE)
    }
}
