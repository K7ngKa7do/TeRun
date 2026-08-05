package com.example.terun

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * =====================================================================
 * NotificationHelper – Lokale Push-Benachrichtigungen senden
 * =====================================================================
 *
 * VORLESUNG 41 – Notifying the User (Benachrichtigungen):
 * Benachrichtigungen informieren den Benutzer über Ereignisse,
 * auch wenn die App gerade nicht aktiv auf dem Bildschirm ist.
 *
 * Verwendete Ereignisse in TeRun:
 * - Ein Checkpoint (Spot) wurde erreicht → "Du hast Spot X erobert!"
 * - Duell beendet → "Glückwunsch!" oder "Zeitlimit abgelaufen"
 *
 * Aufbau einer Benachrichtigung (laut VL 41):
 * 1. Berechtigung anfragen (POST_NOTIFICATIONS, ab Android 13 Pflicht)
 * 2. NotificationChannel erstellen (ab Android 8 / API 26 Pflicht)
 * 3. Benachrichtigung mit NotificationCompat.Builder aufbauen
 * 4. PendingIntent setzen → welche Activity öffnet sich beim Tippen?
 * 5. Benachrichtigung anzeigen via NotificationManagerCompat
 *
 * NotificationCompat:
 * - Aus der AndroidX-Bibliothek
 * - Sorgt für Rückwärtskompatibilität mit älteren Android-Versionen
 */
class NotificationHelper(private val context: Context) {

    private val channelId = "terun_notifications"               // Eindeutige Kanal-ID
    private val channelName = "TeRun Spiel-Benachrichtigungen"  // Anzeigename in System-Einstellungen
    private val notificationId = 1001                           // ID der Benachrichtigung

    // Kanal sofort beim Erstellen des Helpers anlegen
    init {
        createNotificationChannel()
    }

    /**
     * Benachrichtigungskanal anlegen (einmalig).
     * Ab Android 8 (API 26) müssen alle Benachrichtigungen einem Kanal zugewiesen sein.
     * Ohne Kanal werden Benachrichtigungen auf neueren Geräten NICHT angezeigt.
     *
     * IMPORTANCE_DEFAULT:
     * - Benachrichtigung erscheint in der Statusleiste
     * - Kurzes Aufleuchten des Bildschirms
     * - Kein Klingelton
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Benachrichtigungen für abgeschlossene Duelle oder erreichte Checkpoints"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Eine Benachrichtigung mit Titel und Nachrichtentext anzeigen.
     *
     * PendingIntent:
     * Öffnet die MainActivity wenn der Benutzer auf die Benachrichtigung tippt.
     * FLAG_IMMUTABLE → Sicherheitsanforderung ab Android 12 (API 31)
     * FLAG_ACTIVITY_CLEAR_TASK → alle offenen Screens werden geschlossen, MainActivity öffnet neu
     *
     * setAutoCancel(true):
     * Benachrichtigung verschwindet automatisch nachdem der Benutzer darauf tippt.
     *
     * @SuppressLint: POST_NOTIFICATIONS-Berechtigung ist im Manifest deklariert.
     */
    @SuppressLint("MissingPermission")
    fun sendNotification(title: String, message: String) {
        // Beim Tippen auf die Benachrichtigung → MainActivity öffnen
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Benachrichtigung aufbauen
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Info-Icon in der Statusleiste
            .setContentTitle(title)                           // Fettgedruckte Überschrift
            .setContentText(message)                          // Nachrichtentext darunter
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Normale Priorität
            .setContentIntent(pendingIntent)                  // Aktion beim Tippen
            .setAutoCancel(true)                              // Nach Tippen automatisch schließen
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS-Berechtigung fehlt (z.B. unter Android 13 nicht erteilt)
            // → Benachrichtigung still ignorieren, App läuft weiter
        }
    }
}
