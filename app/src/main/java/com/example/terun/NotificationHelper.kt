// Datei: NotificationHelper.kt
// Paket: com.example.terun
// Quelle: moco202641notifications.pdf — NotificationManager, NotificationChannel und NotificationCompat-Builder

package com.example.terun

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.annotation.SuppressLint

class NotificationHelper(private val context: Context) {

    private val channelId = "terun_notifications"
    private val channelName = "TeRun Spiel-Benachrichtigungen"
    private val notificationId = 1001

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // NotificationChannels sind ab Android 8.0 (API 26) erforderlich
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Benachrichtigungen für abgeschlossene Duelle oder erreichte Spots"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun sendNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Nutzt das System-Info-Icon als Fallback
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            // Permission POST_NOTIFICATIONS fehlt unter Android 13+
        }
    }
}
