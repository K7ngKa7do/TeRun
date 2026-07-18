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
 * TeRunLocationService — Ein Foreground Service zur stabilen GPS-Verfolgung im Hintergrund (VL 5 / VL 10).
 * Verhindert, dass das Android-Betriebssystem die Standorterfassung beendet, wenn die App im
 * Hintergrund läuft (z.B. in der Tasche).
 */
class TeRunLocationService : Service() {

    private lateinit var locationHelper: LocationHelper
    private val notificationId = 9999
    private val channelId = "terun_location_service"

    companion object {
        // Statischer Callback, um Positionsänderungen an das ViewModel weiterzuleiten
        var onLocationReceived: ((Location) -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()
        locationHelper = LocationHelper(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("TeRun Duell aktiv", "Standortverfolgung läuft im Hintergrund...")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    notificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(notificationId, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // GPS Ortung starten
        locationHelper.startLocationUpdates { location ->
            onLocationReceived?.invoke(location)
        }

        // START_STICKY sorgt dafür, dass der Dienst neu gestartet wird, falls er vom System gekillt wird
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        locationHelper.stopLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TeRun Hintergrund-Ortung",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, text: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
