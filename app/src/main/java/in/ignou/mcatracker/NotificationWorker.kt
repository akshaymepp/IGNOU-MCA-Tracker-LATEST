
package `in`.ignou.mcatracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = AnnouncementRepository(applicationContext)
            val announcements = repository.fetchAnnouncements()

            val preferences = applicationContext.getSharedPreferences(
                "ignou_tracker",
                Context.MODE_PRIVATE
            )

            val seenIds = preferences.getStringSet(
                "seen_announcement_ids",
                emptySet()
            )?.toMutableSet() ?: mutableSetOf()

            val currentIds = announcements.map { it.id }.toSet()

            // First successful run: save existing announcements silently.
            if (seenIds.isEmpty()) {
                preferences.edit()
                    .putStringSet("seen_announcement_ids", currentIds)
                    .apply()
                return Result.success()
            }

            val newAnnouncements = announcements.filter {
                it.id !in seenIds
            }

            newAnnouncements.forEachIndexed { index, announcement ->
                showNotification(
                    announcement.title,
                    announcement.url,
                    index
                )
            }

            val updatedIds = (seenIds + currentIds).toList().takeLast(500).toSet()

            preferences.edit()
                .putStringSet("seen_announcement_ids", updatedIds)
                .apply()

            Result.success()

        } catch (exception: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(
        title: String,
        url: String,
        notificationId: Int
    ) {
        val channelId = "ignou_announcements"

        val manager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "IGNOU Announcements",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(
            applicationContext,
            MainActivity::class.java
        ).apply {
            putExtra("announcement_url", url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            channelId
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("IGNOU MCA Update")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(notificationId, notification)
    }
}
