package com.example.terun

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * =====================================================================
 * TeRunLocationService – Foreground Service für GPS-Tracking im Hintergrund
 * =====================================================================
 *
 * VORLESUNG 25 – Services (Android App-Komponenten):
 * Ein Service ist eine App-Komponente die im Hintergrund läuft,
 * ohne eine Benutzeroberfläche zu haben.
 *
 * Es gibt zwei Typen:
 * 1. Background Service  → unsichtbar im Hintergrund, kann vom Android-System gestoppt werden
 * 2. Foreground Service  → zeigt dauerhaft eine Benachrichtigung an, Android darf ihn NICHT stoppen
 *
 * Warum ein Foreground Service für GPS?
 * Während eines Duells muss die App den Standort des Spielers auch dann verfolgen,
 * wenn er das Telefon in der Tasche hat und die App nicht sichtbar ist.
 * Ein normaler Service würde vom System für Energiesparzwecke gestoppt.
 * Ein Foreground Service läuft garantiert weiter (VL 45 – Background Execution Limits).
 *
 * VORLESUNG 41 – Benachrichtigungen:
 * Der Foreground Service benötigt ZWINGEND eine sichtbare Benachrichtigung,
 * die dem Benutzer zeigt: "Diese App läuft gerade im Hintergrund und nutzt GPS."
 * Ab Android 8 (API 26) muss dafür ein NotificationChannel erstellt werden.
 *
 * START_STICKY:
 * Falls Android den Service aus Speichermangel beendet, wird er automatisch
 * neu gestartet sobald wieder Ressourcen verfügbar sind.
 *
 * Manifest-Eintrag:
 * Der Service muss im AndroidManifest.xml deklariert sein (android:name=".TeRunLocationService")
 * und der Typ "location" muss angegeben werden (foregroundServiceType="location").
 */
class TeRunLocationService : Service() {

    private lateinit var locationHelper: LocationHelper // GPS-Hilfsklasse
    private val notificationId = 9999                   // ID der Benachrichtigung (eindeutig)
    private val channelId = "terun_location_service"    // ID des Benachrichtigungskanals

    companion object {
        /**
         * Statischer Callback: Das ViewModel setzt hier eine Funktion ein,
         * die bei jeder neuen GPS-Position aufgerufen wird.
         * Verbindung zwischen Service und ViewModel ohne direkte Referenz.
         */
        var onLocationReceived: ((Location) -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()
        locationHelper = LocationHelper(applicationContext)
        createNotificationChannel() // Kanal muss vor der ersten Benachrichtigung existieren
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Pflicht-Benachrichtigung für Foreground Service erstellen
        val notification = createNotification(
            "TeRun Duell aktiv",
            "Standortverfolgung läuft im Hintergrund..."
        )

        // Als Foreground Service registrieren (mit GPS-Typ ab Android 10 / API 29)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    notificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION // Typ: Standort-Service
                )
            } else {
                startForeground(notificationId, notification) // Für ältere Android-Versionen
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // GPS-Ortung starten und Ergebnisse an das ViewModel weiterleiten
        locationHelper.startLocationUpdates { location ->
            onLocationReceived?.invoke(location) // Callback aufrufen falls gesetzt
        }

        // START_STICKY: Service wird automatisch neu gestartet falls er beendet wird
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        locationHelper.stopLocationUpdates() // GPS-Updates abmelden beim Beenden
    }

    // Dieser Service nutzt keine Kommunikation über Binder → null zurückgeben
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Benachrichtigungskanal erstellen (nur einmalig nötig, ab Android 8 / API 26 Pflicht).
     * Ein Kanal bündelt alle Benachrichtigungen eines bestimmten Typs.
     * Der Benutzer kann Kanäle in den System-Einstellungen stumm schalten.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TeRun Hintergrund-Ortung",       // Anzeigename in den System-Einstellungen
                NotificationManager.IMPORTANCE_LOW // Niedrige Priorität = kein Ton
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Eine einzelne Benachrichtigung für die Statusleiste erstellen.
     * setOngoing(true) → Benutzer kann sie nicht wegwischen (Foreground-Pflicht)
     */
    private fun createNotification(title: String, text: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // GPS-Symbol
            .setOngoing(true)                                     // Nicht vom Benutzer schließbar
            .setPriority(NotificationCompat.PRIORITY_LOW)         // Kein Ton, keine Vibration
            .build()
    }
}
