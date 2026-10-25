package com.amir.buysmart.domain.protection

import com.amir.buysmart.domain.model.ShoppingItem
import com.amir.buysmart.domain.model.ShoppingLocation
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionPolicyTest {

    private val now = 1_800_000_000_000L
    private val day = 24 * 60 * 60 * 1000L

    private fun deleted(
        id: String,
        at: Long,
        reason: String = "manual",
        restoredAt: Long? = null,
        clientAt: Long? = null
    ) = ShoppingItem(
        id = id, name = "חלב", location = ShoppingLocation.SUPERMARKET,
        addedBy = "u", listId = "l",
        deletedAtMillis = at, deletedAtClientMillis = clientAt,
        deleteReason = reason, autoRestoredAtMillis = restoredAt
    )

    private fun active(id: String) = ShoppingItem(
        id = id, name = "לחם", location = ShoppingLocation.SUPERMARKET,
        addedBy = "u", listId = "l"
    )

    private fun burst(count: Int, step: Long, reason: String = "manual", restoredAt: Long? = null) =
        (0 until count).map { deleted("i$it", now - 60_000 + it * step, reason, restoredAt) }

    @Test
    fun `cluster of 10 manual deletes within 2s is restored`() {
        assertEquals(10, ProtectionPolicy.massDeletionCluster(burst(10, 200), now).size)
    }

    @Test
    fun `9 deletes is not a cluster`() {
        assertTrue(ProtectionPolicy.massDeletionCluster(burst(9, 200), now).isEmpty())
    }

    @Test
    fun `10 deletes spread over 2_7s is not a cluster`() {
        assertTrue(ProtectionPolicy.massDeletionCluster(burst(10, 300), now).isEmpty())
    }

    @Test
    fun `offline batch with one server time but client times 1s apart is not a cluster`() {
        // מחיקות אופליין: כולן מקבלות את אותה חותמת-שרת בזמן הסנכרון
        val synced = now - 60_000
        val items = (0 until 10).map { deleted("off$it", synced, clientAt = synced - 20_000 + it * 1_000) }
        assertTrue(ProtectionPolicy.massDeletionCluster(items, now).isEmpty())
    }

    @Test
    fun `10 deletes within 2s of client time are a cluster even if server times spread`() {
        val items = (0 until 10).map {
            deleted("c$it", now - 60_000 + it * 1_000, clientAt = now - 70_000 + it * 200)
        }
        assertEquals(10, ProtectionPolicy.massDeletionCluster(items, now).size)
    }

    @Test
    fun `exact 2000ms span is included`() {
        val base = now - 60_000
        val offsets = (0 until 9).map { it * 100L } + 2_000L
        val items = offsets.mapIndexed { i, off -> deleted("b$i", base + off, clientAt = base + off) }
        assertEquals(10, ProtectionPolicy.massDeletionCluster(items, now).size)
    }

    @Test
    fun `2001ms span is not a cluster`() {
        val base = now - 60_000
        val offsets = (0 until 9).map { it * 100L } + 2_001L
        val items = offsets.mapIndexed { i, off -> deleted("b$i", base + off, clientAt = base + off) }
        assertTrue(ProtectionPolicy.massDeletionCluster(items, now).isEmpty())
    }

    @Test
    fun `burst of 10 among scattered older deletes is detected`() {
        val scattered = (1..20).map { h ->
            val at = now - h * 60 * 60 * 1000L
            deleted("s$h", at, clientAt = at)
        }
        val burstStart = now - 30 * 60 * 1000L
        val burst = (0 until 10).map { deleted("x$it", burstStart + it * 150, clientAt = burstStart + it * 150) }
        val result = ProtectionPolicy.massDeletionCluster(scattered + burst, now)
        assertEquals(burst.map { it.id }.toSet(), result.toSet())
    }

    @Test
    fun `client time older than 24h is outside the lookback even if server time is recent`() {
        val synced = now - 60_000
        val items = (0 until 10).map {
            deleted("late$it", synced, clientAt = now - 25 * 60 * 60 * 1000L + it * 100)
        }
        assertTrue(ProtectionPolicy.massDeletionCluster(items, now).isEmpty())
    }

    @Test
    fun `finish and merge reasons are ignored`() {
        assertTrue(ProtectionPolicy.massDeletionCluster(burst(10, 200, "finish"), now).isEmpty())
        assertTrue(ProtectionPolicy.massDeletionCluster(burst(10, 200, "merge"), now).isEmpty())
    }

    @Test
    fun `deletes older than 24h are ignored`() {
        val old = (0 until 10).map { deleted("o$it", now - 25 * 60 * 60 * 1000L + it * 200) }
        assertTrue(ProtectionPolicy.massDeletionCluster(old, now).isEmpty())
    }

    @Test
    fun `recently auto-restored items are not counted`() {
        val items = burst(10, 200, restoredAt = now - 60 * 60 * 1000L)
        assertTrue(ProtectionPolicy.massDeletionCluster(items, now).isEmpty())
    }

    @Test
    fun `item re-deleted soon after auto-restore is never counted even 25h after the restore`() {
        // שוחזר לפני 25 שעות, נמחק שוב שעתיים אחרי השחזור (בתוך 24 השעות האחרונות)
        val restoredAt = now - 25 * 60 * 60 * 1000L
        val items = (0 until 10).map {
            deleted("r$it", restoredAt + 2 * 60 * 60 * 1000L + it * 100, restoredAt = restoredAt)
        }
        assertTrue(ProtectionPolicy.massDeletionCluster(items, now).isEmpty())
    }

    @Test
    fun `item re-deleted more than 24h after its auto-restore counts again`() {
        val restoredAt = now - 3 * day
        val items = burst(10, 100, restoredAt = restoredAt)
        assertEquals(10, ProtectionPolicy.massDeletionCluster(items, now).size)
    }

    @Test
    fun `isBackupDay true only on Sunday and Thursday`() {
        assertTrue(ProtectionPolicy.isBackupDay(LocalDate.of(2026, 10, 25)))
        assertTrue(ProtectionPolicy.isBackupDay(LocalDate.of(2026, 10, 29)))
        assertFalse(ProtectionPolicy.isBackupDay(LocalDate.of(2026, 10, 26)))
    }

    @Test
    fun `backupId formats yyyy-MM-dd`() {
        assertEquals("2026-10-25", ProtectionPolicy.backupId(LocalDate.of(2026, 10, 25)))
    }

    @Test
    fun `purges deletes older than 30 days when active items exist`() {
        val items = listOf(active("a"), deleted("old", now - 31 * day))
        assertEquals(listOf("old"), ProtectionPolicy.itemsToPurge(items, now))
    }

    @Test
    fun `keeps newest deleted item when list has no active items`() {
        val items = listOf(
            deleted("d1", now - 40 * day),
            deleted("d2", now - 35 * day),
            deleted("d3", now - 50 * day)
        )
        assertEquals(setOf("d1", "d3"), ProtectionPolicy.itemsToPurge(items, now).toSet())
    }

    @Test
    fun `does not purge recent deletes`() {
        val items = listOf(active("a"), deleted("d", now - 29 * day))
        assertTrue(ProtectionPolicy.itemsToPurge(items, now).isEmpty())
    }

    @Test
    fun `backupsToPurge returns only older than 35 days`() {
        val backups = mapOf("old" to now - 36 * day, "new" to now - 34 * day)
        assertEquals(listOf("old"), ProtectionPolicy.backupsToPurge(backups, now))
    }

    @Test
    fun `shouldRestoreFromBackup`() {
        assertTrue(ProtectionPolicy.shouldRestoreFromBackup(0, false, 5))
        assertFalse(ProtectionPolicy.shouldRestoreFromBackup(0, true, 5))
        assertFalse(ProtectionPolicy.shouldRestoreFromBackup(1, false, 5))
        assertFalse(ProtectionPolicy.shouldRestoreFromBackup(0, false, null))
        assertFalse(ProtectionPolicy.shouldRestoreFromBackup(0, false, 0))
    }
}
