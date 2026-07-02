package com.example.terun

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * NotificationHelper — Sendet lokale Push-Benachrichtigungen.
 * Wird verwendet um den Spieler über erreichte Spots oder Duell-Ergebnisse zu informieren,
 * auch wenn die App gerade im Hintergrund läuft.
 */
class NotificationHelper(private val context: Context) {

    private val channelId = "terun_notifications"                   // Eindeutige Kanal-ID (fest vergeben)
    private val channelName = "TeRun Spiel-Benachrichtigungen"      // Anzeigename in den Systemeinstellungen
    private val notificationId = 1001                               // ID der Benachrichtigung (überschreibt vorherige)

    // Kanal beim Erstellen des Helpers sofort anlegen
    init {
        createNotificationChannel()
    }

    // NotificationChannel anlegen (ab Android 8.0 / API 26 erforderlich)
    // Ohne Channel werden Benachrichtigungen auf neueren Geräten nicht angezeigt
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Benachrichtigungen für abgeschlossene Duelle oder erreichte Spots"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    // Benachrichtigung mit Titel und Nachrichtentext senden
    // @SuppressLint: Berechtigung POST_NOTIFICATIONS wird im Manifest deklariert
    @SuppressLint("MissingPermission")
    fun sendNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // System-Fallback-Icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Benachrichtigung verschwindet nach Antippen
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {} // Berechtigung fehlt unter Android 13+ → ignorieren
    }
}
