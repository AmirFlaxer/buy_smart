package com.amir.buysmart.presentation.screens.home

import android.util.Log
import com.amir.buysmart.data.repository.DataProtectionRepository
import com.amir.buysmart.domain.model.ItemsSnapshot
import com.amir.buysmart.domain.protection.ProtectionPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import java.time.LocalDate
import javax.inject.Inject

/**
 * שומר הרשימה: שחזור אוטומטי במחיקה גורפת (א') ובמחיקת פריטים מהשרת (ב').
 * שחזור אחד בכל פעם - תמונה שמגיעה בזמן שחזור מדולגת.
 */
class ListGuardian @Inject constructor(
    private val repo: DataProtectionRepository
) {
    private val restoreLock = Mutex()

    /** מחזיר הודעה להצגה למשתמש, או null. לעולם לא זורק. */
    suspend fun onSnapshot(
        listId: String,
        uid: String,
        snapshot: ItemsSnapshot,
        nowMillis: Long
    ): String? {
        if (!restoreLock.tryLock()) return null
        try {
            val cluster = ProtectionPolicy.massDeletionCluster(snapshot.items, nowMillis)
            if (cluster.isNotEmpty()) {
                repo.autoRestore(listId, cluster)
                return "שוחזרו ${cluster.size} פריטים שנמחקו בבת אחת"
            }
            // קריאת גיבוי רק כשהתמונה ריקה ומהשרת
            if (snapshot.items.isEmpty() && !snapshot.fromCache) {
                val latest = repo.latestBackup(listId)
                if (latest != null && ProtectionPolicy.shouldRestoreFromBackup(
                        snapshot.items.size, snapshot.fromCache, latest.itemCount
                    )
                ) {
                    delay(ProtectionPolicy.SERVER_RECHECK_DELAY_MS)
                    if (repo.serverItemDocCount(listId) == 0 &&
                        repo.listExistsAndIsMember(listId, uid)
                    ) {
                        repo.restoreFromBackup(listId, latest)
                        return "פריטי הרשימה נמחקו מהשרת ושוחזרו מגיבוי מיום ${displayDate(latest.id)}"
                    }
                }
            }
            return null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "שחזור אוטומטי נכשל", e)
            return null
        } finally {
            restoreLock.unlock()
        }
    }

    // מזהה גיבוי yyyy-MM-dd לתצוגה dd/MM
    private fun displayDate(backupId: String): String = try {
        val d = LocalDate.parse(backupId)
        "%02d/%02d".format(d.dayOfMonth, d.monthValue)
    } catch (e: Exception) {
        backupId
    }

    private companion object {
        const val TAG = "ListGuardian"
    }
}
