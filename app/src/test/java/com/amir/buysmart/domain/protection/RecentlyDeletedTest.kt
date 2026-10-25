package com.amir.buysmart.domain.protection

import com.amir.buysmart.domain.model.ShoppingItem
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentlyDeletedTest {

    private val zone = ZoneId.of("Asia/Jerusalem")
    private val day = 24 * 60 * 60 * 1000L

    private fun millis(d: Int, m: Int, hour: Int, minute: Int = 0): Long =
        ZonedDateTime.of(2026, m, d, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private val now = millis(25, 10, 20)

    private fun deleted(id: String, at: Long, reason: String = "manual") = ShoppingItem(
        id = id, name = id, addedBy = "u", listId = "l",
        deletedAtMillis = at, deleteReason = reason
    )

    @Test
    fun `finish deletions are excluded`() {
        val result = ProtectionPolicy.recentlyDeleted(
            listOf(deleted("a", now - 1000, "finish")), now, zone
        )
        assertEquals(emptyList<Pair<LocalDate, List<ShoppingItem>>>(), result)
    }

    @Test
    fun `manual and merge deletions are included`() {
        val result = ProtectionPolicy.recentlyDeleted(
            listOf(deleted("a", now - 1000, "manual"), deleted("b", now - 2000, "merge")),
            now, zone
        )
        assertEquals(listOf("a", "b"), result.flatMap { it.second }.map { it.id })
    }

    @Test
    fun `deletions older than 30 days are excluded`() {
        val result = ProtectionPolicy.recentlyDeleted(
            listOf(
                deleted("old", now - 30 * day - 1000),
                deleted("edge", now - 30 * day),
                ShoppingItem(id = "live", name = "live", addedBy = "u", listId = "l")
            ),
            now, zone
        )
        assertEquals(listOf("edge"), result.flatMap { it.second }.map { it.id })
    }

    @Test
    fun `groups by local day newest first`() {
        val result = ProtectionPolicy.recentlyDeleted(
            listOf(
                deleted("old23", millis(23, 10, 9)),
                deleted("early25", millis(25, 10, 8)),
                deleted("late25", millis(25, 10, 18))
            ),
            now, zone
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 25) to listOf("late25", "early25"),
                LocalDate.of(2026, 10, 23) to listOf("old23")
            ),
            result.map { (date, items) -> date to items.map { it.id } }
        )
    }
}
