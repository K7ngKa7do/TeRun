// Datei: LocationHelper.kt
// Paket: com.example.terun
// Quelle: developer.android.com/develop/sensors-and-location/location/retrieve-current — Ortungsdaten über LocationManager beziehen

package com.example.terun

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

class LocationHelper(context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Prüft, ob GPS eingeschaltet ist
    val isGpsEnabled: Boolean
        get() = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    private var activeListener: LocationListener? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocationChanged: (Location) -> Unit) {
        // Alten Listener stoppen
        stopLocationUpdates()

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                onLocationChanged(location)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        activeListener = listener

        try {
            // Letzten bekannten Standort sofort abfragen und senden zur schnellen Initialisierung
            var lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastKnown == null) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            if (lastKnown == null) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            }
            lastKnown?.let { onLocationChanged(it) }

            // Ortungs-Updates über GPS anfordern (Intervall: 1 Sekunde, Distanz: 1 Meter)
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    1f,
                    listener
                )
            }
            // Fallback auf Netzwerk-Ortung
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    1f,
                    listener
                )
            }
        } catch (e: SecurityException) {
            // Keine Berechtigung erteilt
        }
    }

    fun stopLocationUpdates() {
        activeListener?.let {
            locationManager.removeUpdates(it)
            activeListener = null
        }
    }
}
