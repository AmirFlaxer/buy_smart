package com.amir.buysmart.domain.protection

import com.amir.buysmart.domain.model.ItemPriority
import com.amir.buysmart.domain.model.ItemType
import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreNotifyTest {

    private val now = 10_000_000_000L
    private val minute = 60_000L
    private val hour = 60 * minute

    private fun item(restoredAt: Long?) = ShoppingItem(
        id = "a", name = "חלב", quantity = "",
        location = ShoppingLocation.SUPERMARKET, type = ItemType.ONE_TIME,
        addedBy = "u", listId = "l", priority = ItemPriority.NORMAL,
        autoRestoredAtMillis = restoredAt
    )

    @Test
    fun `item restored 10 minutes ago is suppressed`() {
        assertTrue(ProtectionPolicy.isRecentlyRestored(item(now - 10 * minute), now))
    }

    @Test
    fun `item restored 2 hours ago is not suppressed`() {
        assertFalse(ProtectionPolicy.isRecentlyRestored(item(now - 2 * hour), now))
    }

    @Test
    fun `item never restored is not suppressed`() {
        assertFalse(ProtectionPolicy.isRecentlyRestored(item(null), now))
    }
}
