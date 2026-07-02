package com.example.terun

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

/**
 * LocationHelper — Wrapper für den Android LocationManager.
 * Kümmert sich um das Starten und Stoppen von GPS-Updates und liefert
 * via Callback die jeweils aktuelle Geräteposition.
 */
class LocationHelper(context: Context) {

    // System-Service für Standortabfragen (GPS, Netzwerk, passiv)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Referenz auf den aktiven Listener (zum späteren Abmelden benötigt)
    private var activeListener: LocationListener? = null

    // Gibt an ob GPS-Provider aktuell eingeschaltet ist
    val isGpsEnabled: Boolean
        get() = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    // GPS-Updates starten; onLocationChanged wird bei jeder neuen Position aufgerufen
    @SuppressLint("MissingPermission") // Berechtigung wird im Manifest und zur Laufzeit abgefragt
    fun startLocationUpdates(onLocationChanged: (Location) -> Unit) {
        stopLocationUpdates() // Alten Listener zuerst sauber stoppen

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) = onLocationChanged(location)

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        activeListener = listener

        try {
            // Zuletzt bekannte Position sofort liefern (kein Warten auf ersten GPS-Fix)
            val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            lastKnown?.let { onLocationChanged(it) }

            // Live-GPS-Updates anfordern: alle 1 Sekunde oder bei Bewegung > 1 Meter
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, listener)
            }
            // Fallback: Netzwerk-Ortung (weniger genau, aber auch in Gebäuden verfügbar)
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 1f, listener)
            }
        } catch (_: SecurityException) {} // Keine Berechtigung → ignorieren (UI zeigt Hinweis)
    }

    // GPS-Updates stoppen und Listener beim LocationManager abmelden
    fun stopLocationUpdates() {
        activeListener?.let {
            locationManager.removeUpdates(it)
            activeListener = null
        }
    }
}
