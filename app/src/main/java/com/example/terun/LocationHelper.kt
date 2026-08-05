package com.example.terun

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

/**
 * =====================================================================
 * LocationHelper – GPS-Standortabfragen über den LocationManager
 * =====================================================================
 *
 * VORLESUNG 43 – Location-based Services (Standortdienste):
 * Android bietet verschiedene Standortanbieter (Location Provider):
 * - GPS_PROVIDER      → genauester Standort (via GPS-Satellit), funktioniert im Freien
 * - NETWORK_PROVIDER  → schnellerer Standort (via WLAN/Mobilfunk), auch in Gebäuden
 * - PASSIVE_PROVIDER  → nutzt Standortdaten anderer Apps (kein eigener Energieverbrauch)
 *
 * Der LocationManager verwaltet diese Anbieter und liefert Standortdaten
 * über einen LocationListener (Rückruf-Methode bei jeder Positionsänderung).
 *
 * Warum zwei Anbieter gleichzeitig?
 * GPS ist genauer aber langsamer beim ersten Fix (besonders bei bewölktem Himmel).
 * Das Netzwerk liefert sofort eine ungefähre Position als Ersatz.
 * Die App nutzt beide parallel und akzeptiert von beiden Updates.
 *
 * Berechtigung (aus AndroidManifest.xml):
 * ACCESS_FINE_LOCATION → Pflicht um den GPS_PROVIDER nutzen zu können (VL 40 – Permissions)
 */
class LocationHelper(context: Context) {

    // System-Service für alle Standortabfragen
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Speichert den aktiven Listener, damit er beim Stoppen abgemeldet werden kann
    private var activeListener: LocationListener? = null

    /**
     * Gibt zurück ob der GPS-Anbieter aktuell eingeschaltet ist.
     * Falls der Benutzer GPS deaktiviert hat, gibt dies false zurück.
     */
    val isGpsEnabled: Boolean
        get() = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    /**
     * GPS-Updates starten.
     * - Liefert sofort den letzten bekannten Standort (kein Warten auf ersten GPS-Fix)
     * - Danach: Updates alle 1 Sekunde oder bei einer Bewegung von mehr als 1 Meter
     * - onLocationChanged wird bei jeder neuen Position aufgerufen
     *
     * @SuppressLint: Die Berechtigung wird im Manifest und zur Laufzeit abgefragt.
     * Android Studio warnt trotzdem – diese Annotation unterdrückt diese Warnung.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocationChanged: (Location) -> Unit) {
        // Alten Listener zuerst sauber abmelden (verhindert doppelte Updates)
        stopLocationUpdates()

        // Neuen Listener erstellen der bei jeder Positionsänderung den Callback aufruft
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) = onLocationChanged(location)

            // Diese Methoden werden von alten Android-Versionen benötigt (jetzt veraltet)
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        activeListener = listener

        try {
            // Sofortige Positionslieferung: letzten bekannten Standort ermitteln
            // Fallback-Kette: GPS → Netzwerk → Passiv
            val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            lastKnown?.let { onLocationChanged(it) }

            // GPS-Updates anfordern (genauer, aber langsamer)
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L, // Mindestintervall: 1 Sekunde
                    1f,    // Mindestbewegung: 1 Meter
                    listener
                )
            }

            // Netzwerk-Updates anfordern (schneller, aber ungenauer)
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L, // Mindestintervall: 1 Sekunde
                    1f,    // Mindestbewegung: 1 Meter
                    listener
                )
            }
        } catch (_: SecurityException) {
            // Berechtigung fehlt → ignorieren (UI zeigt einen Hinweis an den Benutzer)
        }
    }

    /**
     * GPS-Updates stoppen und den Listener beim LocationManager abmelden.
     * Wichtig: ohne Abmeldung läuft der Listener weiter und verbraucht Akku!
     */
    fun stopLocationUpdates() {
        activeListener?.let {
            locationManager.removeUpdates(it)
            activeListener = null
        }
    }
}
