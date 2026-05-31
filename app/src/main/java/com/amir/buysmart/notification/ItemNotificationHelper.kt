package com.amir.buysmart.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.amir.buysmart.R
import com.amir.buysmart.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * התראות מקומיות — מוצגות מהמכשיר עצמו (ללא שרת/FCM) כשמתווסף פריט ע"י משתמש אחר.
 * עובד כל עוד תהליך האפליקציה חי וה-listener ל-Firestore פעיל.
 */
@Singleton
class ItemNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var channelCreated = false

    private fun ensureChannel() {
        if (channelCreated) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "פריטים חדשים ברשימה",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "התראה כשמישהו מוסיף פריט לרשימה המשותפת" }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        channelCreated = true
    }

    /** מציג התראה על פריט שנוסף. dedupeId — מזהה ייחודי כדי לא לדרוס התראות. */
    fun showItemAdded(title: String, body: String, dedupeId: Int) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return // המשתמש לא אישר התראות — לא עושים כלום

        ensureChannel()

        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = android.app.PendingIntent.getActivity(
            context, 0, intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(dedupeId, notification)
    }

    companion object {
        private const val CHANNEL_ID = "buysmart_items"
    }
}
