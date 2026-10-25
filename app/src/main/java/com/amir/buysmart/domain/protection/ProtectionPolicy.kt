package com.amir.buysmart.domain.protection

import com.amir.buysmart.domain.model.DeleteReason
import com.amir.buysmart.domain.model.ShoppingItem
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// לוגיקת הגנה על נתוני הרשימה - טהורה, בלי תלות באנדרואיד או בפיירבייס
object ProtectionPolicy {
    const val MASS_DELETE_MIN_ITEMS = 10
    const val MASS_DELETE_WINDOW_MS = 2_000L
    const val LOOKBACK_MS = 24 * 60 * 60 * 1000L
    const val ITEM_PURGE_AGE_MS = 30 * 24 * 60 * 60 * 1000L
    const val BACKUP_PURGE_AGE_MS = 35 * 24 * 60 * 60 * 1000L
    const val SERVER_RECHECK_DELAY_MS = 5_000L
    const val RESTORE_NOTIFY_SUPPRESS_MS = 60 * 60 * 1000L
    const val BACKUP_STALE_MS = 5 * 24 * 60 * 60 * 1000L

    private val BACKUP_ID_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // זמן המחיקה לזיהוי אשכול: שעון המכשיר המוחק. מחיקות אופליין מקבלות כולן את אותה
    // חותמת-שרת בזמן הסנכרון, ולכן חותמת-השרת משמשת רק כגיבוי למסמכים בלי זמן-מכשיר
    private fun clusterTime(item: ShoppingItem): Long? = item.deletedAtClientMillis ?: item.deletedAtMillis

    // מחזיר מזהי פריטים לשחזור אם יש אשכול מחיקה גורפת, אחרת רשימה ריקה
    fun massDeletionCluster(items: List<ShoppingItem>, nowMillis: Long): List<String> {
        val candidates = items
            .filter { item ->
                val deletedAt = item.deletedAtMillis ?: return@filter false
                val clientAt = clusterTime(item) ?: return@filter false
                val restoredAt = item.autoRestoredAtMillis
                // פריט שהוחזר אוטומטית ונמחק שוב תוך 24 שעות מהשחזור - מחיקה מכוונת, לא נספר
                // לעולם. נמדד מול זמן המחיקה בשרת (שני השדות חותמות-שרת) ולא מול עכשיו
                item.deleteReason == DeleteReason.MANUAL &&
                    nowMillis - clientAt <= LOOKBACK_MS &&
                    (restoredAt == null ||
                        (deletedAt - restoredAt > LOOKBACK_MS && nowMillis - restoredAt > LOOKBACK_MS))
            }
            .sortedBy { clusterTime(it) }

        // חלון מחליק: הקבוצה הגדולה ביותר שבה max - min <= החלון
        var bestStart = 0
        var bestSize = 0
        var start = 0
        for (end in candidates.indices) {
            while (clusterTime(candidates[end])!! - clusterTime(candidates[start])!! >
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

    // המפתח הוא backupId, הערך הוא createdAtMillis. הגיבוי החדש ביותר לא נמחק לעולם,
    // גם אם הגיבויים הפסיקו להיווצר
    fun backupsToPurge(backups: Map<String, Long>, nowMillis: Long): List<String> {
        val newestId = backups.maxByOrNull { it.value }?.key
        return backups
            .filter { it.key != newestId && nowMillis - it.value > BACKUP_PURGE_AGE_MS }
            .keys.toList()
    }

    // אין גיבוי, או שהאחרון ותיק מ-5 ימים (גיבוי פעמיים בשבוע: הפער הרגיל עד 4 ימים)
    fun isBackupStale(latestCreatedAtMillis: Long?, nowMillis: Long): Boolean =
        latestCreatedAtMillis == null || nowMillis - latestCreatedAtMillis > BACKUP_STALE_MS

    fun shouldRestoreFromBackup(
        serverDocCount: Int,
        fromCache: Boolean,
        latestBackupItemCount: Int?
    ): Boolean = !fromCache && serverDocCount == 0 && (latestBackupItemCount ?: 0) > 0

    // פריט שהוחזר לאחרונה (ידנית או אוטומטית) - לא מציגים עליו התראת "נוסף" למשתמשים אחרים
    fun isRecentlyRestored(item: ShoppingItem, nowMillis: Long): Boolean =
        listOfNotNull(item.restoredAtMillis, item.autoRestoredAtMillis)
            .any { nowMillis - it <= RESTORE_NOTIFY_SUPPRESS_MS }

    // פריטים שנמחקו ידנית או במיזוג ב-30 הימים האחרונים, מקובצים לפי יום מקומי (חדש ראשון)
    fun recentlyDeleted(
        items: List<ShoppingItem>,
        nowMillis: Long,
        zone: ZoneId
    ): List<Pair<LocalDate, List<ShoppingItem>>> =
        items
            .filter { item ->
                val deletedAt = item.deletedAtMillis ?: return@filter false
                (item.deleteReason == DeleteReason.MANUAL || item.deleteReason == DeleteReason.MERGE) &&
                    nowMillis - deletedAt <= ITEM_PURGE_AGE_MS
            }
            .sortedByDescending { it.deletedAtMillis }
            .groupBy { Instant.ofEpochMilli(it.deletedAtMillis!!).atZone(zone).toLocalDate() }
            .toList()
            .sortedByDescending { it.first }
}
