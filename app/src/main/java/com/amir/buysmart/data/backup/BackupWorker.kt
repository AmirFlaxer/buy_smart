package com.amir.buysmart.data.backup

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.amir.buysmart.data.repository.DataProtectionRepository
import com.amir.buysmart.domain.protection.ProtectionPolicy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

// גיבוי אוטומטי: ביום גיבוי (ראשון/חמישי) יוצר גיבוי לכל רשימה, ובכל ריצה מנקה ישנים
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: DataProtectionRepository,
    private val auth: FirebaseAuth
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: return Result.success()
        val today = LocalDate.now(ZoneId.of("Asia/Jerusalem"))
        val backupDay = ProtectionPolicy.isBackupDay(today)

        val listIds = try {
            repo.userListIds(uid)
        } catch (e: Exception) {
            Log.w(TAG, "טעינת רשימות המשתמש נכשלה", e)
            return if (isPermissionDenied(e)) Result.success() else Result.retry()
        }

        var anyFailed = false
        for (listId in listIds) {
            try {
                if (backupDay) {
                    repo.createBackupIfMissing(listId, ProtectionPolicy.backupId(today), uid)
                }
                repo.purge(listId, System.currentTimeMillis())
            } catch (e: Exception) {
                Log.w(TAG, "גיבוי/ניקוי נכשל לרשימה $listId", e)
                // כללי הגיבויים אולי עוד לא נפרסו - לא מנסים שוב ללא סוף
                if (!isPermissionDenied(e)) anyFailed = true
            }
        }
        return if (anyFailed) Result.retry() else Result.success()
    }

    private fun isPermissionDenied(e: Exception): Boolean =
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED

    companion object {
        private const val TAG = "BackupWorker"
        const val PERIODIC_NAME = "data_backup_daily"
        const val STARTUP_NAME = "data_backup_startup"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val wm = WorkManager.getInstance(context)
            wm.enqueueUniquePeriodicWork(
                PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()
            )
            wm.enqueueUniqueWork(
                STARTUP_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<BackupWorker>()
                    .setConstraints(constraints)
                    .build()
            )
        }
    }
}
