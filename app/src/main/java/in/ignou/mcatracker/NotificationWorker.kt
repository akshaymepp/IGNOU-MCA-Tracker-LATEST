package `in`.ignou.mcatracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Runs on the periodic schedule configured in MainActivity.
 * First successful check silently records existing notices as a baseline.
 * Each newly detected announcement after that gets its own notification.
 */
class NotificationWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previouslySeen = prefs.getStringSet(KEY_SEEN_IDS, emptySet())
            ?.toMutableSet() ?: mutableSetOf()
        val announcements = AnnouncementRepository().fetch()
        val currentIds = announcements.map { it.id }.toSet()

        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            prefs.edit()
                .putStringSet(KEY_SEEN_IDS, currentIds)
                .putBoolean(KEY_INITIALIZED, true)
                .apply()
            return Result.success()
        }

        val newAnnouncements = announcements.filter { it.id !in previouslySeen }
        if (newAnnouncements.isNotEmpty()) createNotificationChannel()
        newAnnouncements.forEach { postNotification(it) }

        val updatedHistory = (previouslySeen + currentIds)
            .toList()
            .takeLast(MAX_STORED_IDS)
            .toSet()

        prefs.edit().putStringSet(KEY_SEEN_IDS, updatedHistory).apply()
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }

    private fun postNotification(announcement: Announcement) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(announcement.url))
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            announcement.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New IGNOU MCA_NEW announcement")
            .setContentText(announcement.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "${announcement.title}\\n${announcement.source}"
            ))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(announcement.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // User has not granted notification permission.
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "IGNOU MCA_NEW announcements",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Newly detected MCA_NEW announcements"
                }
            )
        }
    }

    companion object {
        private const val PREFS = "tracker"
        private const val KEY_SEEN_IDS = "seen_ids"
        private const val KEY_INITIALIZED = "seen_initialized"
        private const val MAX_STORED_IDS = 500
        private const val CHANNEL_ID = "ignou_mca_new_updates"
    }
}
