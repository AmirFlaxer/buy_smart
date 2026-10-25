package com.amir.buysmart.domain.protection

import com.amir.buysmart.domain.model.DeleteReason
import com.amir.buysmart.domain.model.ShoppingItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// לוגיקת הגנה על נתוני הרשימה - טהורה, בלי תלות באנדרואיד או בפיירבייס
object ProtectionPolicy {
    const val MASS_DELETE_MIN_ITEMS = 10
    const val MASS_DELETE_WINDOW_MS = 5_000L
    const val LOOKBACK_MS = 24 * 60 * 60 * 1000L
    const val ITEM_PURGE_AGE_MS = 30 * 24 * 60 * 60 * 1000L
    const val BACKUP_PURGE_AGE_MS = 35 * 24 * 60 * 60 * 1000L
    const val SERVER_RECHECK_DELAY_MS = 5_000L
    const val RESTORE_NOTIFY_SUPPRESS_MS = 60 * 60 * 1000L

    private val BACKUP_ID_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // מחזיר מזהי פריטים לשחזור אם יש אשכול מחיקה גורפת, אחרת רשימה ריקה
    fun massDeletionCluster(items: List<ShoppingItem>, nowMillis: Long): List<String> {
        val candidates = items
            .filter { item ->
                val deletedAt = item.deletedAtMillis ?: return@filter false
                val restoredAt = item.autoRestoredAtMillis
                item.deleteReason == DeleteReason.MANUAL &&
                    nowMillis - deletedAt <= LOOKBACK_MS &&
                    (restoredAt == null || nowMillis - restoredAt > LOOKBACK_MS)
            }
            .sortedBy { it.deletedAtMillis }

        // חלון מחליק: הקבוצה הגדולה ביותר שבה max - min <= החלון
        var bestStart = 0
        var bestSize = 0
        var start = 0
        for (end in candidates.indices) {
            while (candidates[end].deletedAtMillis!! - candidates[start].deletedAtMillis!! >
                MASS_DELETE_WINDOW_MS
            ) start++
            val size = end - start + 1
            if (size > bestSize) {
                bestSize = size
                bestStart = start
            }
        }
        if (bestSize < MASS_DELETE_MIN_ITEMS) return emptyList()
        return candidates.subList(bestStart, bestStart + bestSize).map { it.id }
    }

    // גיבוי בימי ראשון וחמישי
    fun isBackupDay(date: LocalDate): Boolean =
        date.dayOfWeek == DayOfWeek.SUNDAY || date.dayOfWeek == DayOfWeek.THURSDAY

    fun backupId(date: LocalDate): String = date.format(BACKUP_ID_FORMAT)

    // פריטים מחוקים ישנים לניקוי. ברשימה בלי פריטים פעילים נשאר המחוק החדש ביותר
    fun itemsToPurge(items: List<ShoppingItem>, nowMillis: Long): List<String> {
        val deleted = items.filter { it.isDeleted }
        val keepId = if (items.none { !it.isDeleted }) {
            deleted.maxByOrNull { it.deletedAtMillis!! }?.id
        } else null
        return deleted
            .filter { nowMillis - it.deletedAtMillis!! > ITEM_PURGE_AGE_MS && it.id != keepId }
            .map { it.id }
    }

    // המפתח הוא backupId, הערך הוא createdAtMillis
    fun backupsToPurge(backups: Map<String, Long>, nowMillis: Long): List<String> =
        backups.filter { nowMillis - it.value > BACKUP_PURGE_AGE_MS }.keys.toList()

    fun shouldRestoreFromBackup(
        serverDocCount: Int,
        fromCache: Boolean,
        latestBackupItemCount: Int?
    ): Boolean = !fromCache && serverDocCount == 0 && (latestBackupItemCount ?: 0) > 0

    // פריט ששוחזר אוטומטית לאחרונה - לא מציגים עליו התראת "נוסף" למשתמשים אחרים
    fun isRecentlyRestored(item: ShoppingItem, nowMillis: Long): Boolean {
        val restoredAt = item.autoRestoredAtMillis ?: return false
        return nowMillis - restoredAt <= RESTORE_NOTIFY_SUPPRESS_MS
    }
}
