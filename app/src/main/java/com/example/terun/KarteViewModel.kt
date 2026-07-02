package com.example.terun

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL

/**
 * KarteViewModel — Zentrale Spiellogik der App (MVVM-ViewModel).
 * Hält den gesamten Spielzustand und kommuniziert zwischen UI (KarteScreen)
 * und Datenschicht (SpielRepository, LocationHelper, NotificationHelper).
 */
class KarteViewModel(application: Application) : AndroidViewModel(application) {

    // --- Abhängigkeiten ---
    private val repository = SpielRepository(application)         // Datenzugriff (Room + SharedPreferences)
    private val locationHelper = LocationHelper(application)      // GPS-Ortung
    private val notificationHelper = NotificationHelper(application) // Push-Benachrichtigungen

    // --- Profil ---
    // Interner State für den Spielernamen — Änderungen werden automatisch in SharedPreferences gespeichert
    private var _spielerName = mutableStateOf("")
    var spielerName: String
        get() = _spielerName.value
        set(value) {
            _spielerName.value = value
            repository.speichereSpielerName(value) // Gleichzeitig in DB und Prefs persistieren
        }

    var spielerGesamtDistanz by mutableStateOf(0.0)  // Gelaufene Gesamtdistanz in Kilometern
    var absolvierteDuelleCount by mutableIntStateOf(0) // Anzahl abgeschlossener Duelle

    // --- GPS / Position ---
    var spielerPosition by mutableStateOf<GeoPoint?>(null)  // Aktuelle GPS-Koordinate des Spielers (null = noch kein Fix)
    var startPositionGeo by mutableStateOf<GeoPoint?>(null) // Startposition bei Duellbeginn (= Zielpunkt nach allen Spots)

    // --- Spot-Status ---
    // Jeder Spot hat einen eigenen Boolean-State; true = bereits vom Spieler erreicht
    var spot1Captured by mutableStateOf(false)
    var spot2Captured by mutableStateOf(false)
    var spot3Captured by mutableStateOf(false)
    var spot4Captured by mutableStateOf(false)
    var spot5Captured by mutableStateOf(false)

    // --- Daten-Listen ---
    val duelle = mutableStateListOf<Duell>()   // Alle gespeicherten Duelle (aus Room geladen)
    val freunde = mutableStateListOf<String>() // Anzeigenamen der Freunde des eingeloggten Spielers

    // --- Routing ---
    val routePoints = mutableStateListOf<GeoPoint>() // Wegpunkte der aktuellen Fußgänger-Route (OSRM)
    private var lastRouteFetchTime = 0L               // Zeitstempel der letzten Route-Abfrage (für Throttling)
    private var lastQueryPos: GeoPoint? = null        // Letzte Position bei Route-Abfrage (für Bewegungs-Check)

    // --- Spielzustand ---
    var aktivesDuell by mutableStateOf<Duell?>(null) // Das gerade laufende Duell (null = kein Duell aktiv)
        private set
    var status by mutableStateOf(SpielStatus.IDLE)   // Aktueller Spielstatus: IDLE, LAEUFT oder BEENDET
        private set
    var verbleibendeZeit by mutableIntStateOf(0)     // Verbleibende Zeit in Sekunden
        private set
    var ergebnisse by mutableStateOf<List<Ergebnis>>(emptyList()) // Ergebnisliste nach Duellende
        private set

    private var timerJob: Job? = null // Coroutine-Job für den Countdown-Timer

    // --- Initialisierung ---
    // Wird einmalig beim Erstellen des ViewModels ausgeführt — lädt alle persistierten Daten
    init {
        _spielerName.value = repository.ladeSpielerName()           // Name aus SharedPreferences laden
        spielerGesamtDistanz = repository.ladeGesamtDistanz()       // Gesamtdistanz laden
        absolvierteDuelleCount = repository.ladeAbsolvierteDuelleCount() // Duellanzahl laden
        viewModelScope.launch { duelle.addAll(repository.holeDuelle()) }      // Duelle asynchron aus Room laden
        viewModelScope.launch { freunde.addAll(repository.holeFreunde(repository.getAccountKey())) } // Freunde laden
    }

    // ==========================================================================
    // Freunde
    // ==========================================================================

    // Freundesliste neu aus der Datenbank laden und den State aktualisieren
    fun ladeFreunde() {
        viewModelScope.launch {
            freunde.clear()
            freunde.addAll(repository.holeFreunde(repository.getAccountKey()))
        }
    }

    // Freund anhand des Anzeigenamens hinzufügen; onResult liefert true bei Erfolg, false wenn User nicht gefunden
    fun fuegeFreundHinzu(name: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.fuegeFreundHinzu(repository.getAccountKey(), name)
            if (success) ladeFreunde() // Liste nach Erfolg sofort aktualisieren
            onResult(success)
        }
    }

    // Freund aus der beidseitigen Freundesliste entfernen
    fun loescheFreund(name: String) {
        viewModelScope.launch {
            repository.loescheFreund(repository.getAccountKey(), name)
            ladeFreunde() // Liste nach Löschen aktualisieren
        }
    }

    // ==========================================================================
    // Duell-Verwaltung
    // ==========================================================================

    // Neues Duell erstellen und sowohl im UI-State als auch in der Room-DB speichern
    fun erstelleDuell(name: String, zeitLimitMinuten: Int, spotsList: List<GeoPoint>, gegner: String) {
        // Hilfsfunktionen zum sicheren Zugriff auf Spot-Koordinaten (0.0 wenn kein Spot an dieser Stelle)
        fun lat(i: Int) = spotsList.getOrNull(i)?.latitude ?: 0.0
        fun lng(i: Int) = spotsList.getOrNull(i)?.longitude ?: 0.0
        val neuesDuell = Duell(
            id = java.util.UUID.randomUUID().toString(), // Eindeutige ID generieren
            name = name,
            spotsAnzahl = spotsList.size.coerceIn(1, 5), // Mindestens 1, maximal 5 Spots erlaubt
            zeitLimitMinuten = zeitLimitMinuten,
            spot1Lat = lat(0), spot1Lng = lng(0),
            spot2Lat = lat(1), spot2Lng = lng(1),
            spot3Lat = lat(2), spot3Lng = lng(2),
            spot4Lat = lat(3), spot4Lng = lng(3),
            spot5Lat = lat(4), spot5Lng = lng(4),
            gegner = gegner
        )
        duelle.add(neuesDuell)                                          // Sofort im UI anzeigen
        viewModelScope.launch { repository.speichereDuell(neuesDuell) } // Asynchron in Room speichern
    }

    // Duell aus UI-Liste und Room-Datenbank löschen
    fun loescheDuell(duell: Duell) {
        duelle.remove(duell)
        viewModelScope.launch { repository.loescheDuell(duell) }
    }

    // ==========================================================================
    // Profil
    // ==========================================================================

    // Eigenes Konto vollständig löschen: DB-Einträge, SharedPreferences und UI-State zurücksetzen
    fun loescheProfil(onDeleted: () -> Unit) {
        val email = repository.getAccountKey()
        viewModelScope.launch {
            repository.loescheKonto(email)   // Benutzer + Freundesliste aus DB löschen
            spielerName = ""
            spielerGesamtDistanz = 0.0
            absolvierteDuelleCount = 0
            repository.speichereGesamtDistanz(0.0)
            repository.speichereAbsolvierteDuelleCount(0)
            repository.setAccountKey("")     // Account-Key leeren = ausgeloggt
            onDeleted()                      // Callback: UI auf Login-Screen weiterleiten
        }
    }

    // ==========================================================================
    // Standort & Spielstart
    // ==========================================================================

    // GPS-Updates starten; bei jeder neuen Position: Distanz berechnen, Spots prüfen, Route aktualisieren
    fun starteStandortAbfrage() {
        locationHelper.startLocationUpdates { location ->
            val prevPos = spielerPosition
            val currentGeo = GeoPoint(location.latitude, location.longitude)
            spielerPosition = currentGeo

            // Distanz nur während laufendem Duell und wenn eine Vorposition bekannt ist
            if (status == SpielStatus.LAEUFT && prevPos != null) {
                spielerGesamtDistanz += calculateDistance(
                    prevPos.latitude, prevPos.longitude,
                    currentGeo.latitude, currentGeo.longitude
                ) / 1000.0 // Meter → Kilometer
                repository.speichereGesamtDistanz(spielerGesamtDistanz)
            }
            // Spot-Erkennung und Route-Update nur während laufendem Duell
            if (status == SpielStatus.LAEUFT) {
                checkSpotsCaptured(location.latitude, location.longitude)
                checkAndUpdateRoutePath()
            }
        }
    }

    // Duell starten: Zustand zurücksetzen, Timer starten, GPS aktivieren
    fun duellStarten(duell: Duell) {
        routePoints.clear()       // Alte Route löschen
        lastRouteFetchTime = 0L   // Throttling zurücksetzen
        lastQueryPos = null
        aktivesDuell = duell
        status = SpielStatus.LAEUFT
        verbleibendeZeit = duell.zeitLimitMinuten * 60 // Minuten → Sekunden umrechnen
        // Alle Spots als nicht erreicht markieren
        spot1Captured = false
        spot2Captured = false
        spot3Captured = false
        spot4Captured = false
        spot5Captured = false
        startPositionGeo = spielerPosition // Startpunkt merken (= späterer Zielpunkt nach allen Spots)

        starteStandortAbfrage()
        timerJob?.cancel() // Eventuell laufenden alten Timer stoppen
        timerJob = viewModelScope.launch {
            try {
                // Jede Sekunde herunterzählen
                while (verbleibendeZeit > 0) {
                    delay(1000L)
                    verbleibendeZeit--
                }
                // Zeit abgelaufen → Duell automatisch beenden (kein Erfolg)
                duellBeenden(success = false)
            } catch (_: CancellationException) {} // Timer wurde manuell gestoppt → keine Aktion nötig
        }
    }

    // Duell beenden: Ergebnisse berechnen, Status setzen, Benachrichtigung senden
    fun duellBeenden(success: Boolean = false, aufgegeben: Boolean = false) {
        routePoints.clear()
        timerJob?.cancel()
        timerJob = null
        locationHelper.stopLocationUpdates()
        absolvierteDuelleCount += 1
        repository.speichereAbsolvierteDuelleCount(absolvierteDuelleCount)

        val active = aktivesDuell
        // Ergebnisliste zusammenbauen
        ergebnisse = if (active != null) {
            val count = active.spotsAnzahl
            // Alle Teilnehmer: eigener Name zuerst, dann Gegner aus kommasepariertem String
            val participants = buildList {
                add(spielerName)
                if (active.gegner.isNotEmpty()) {
                    addAll(active.gegner.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                }
            }
            // Eigene Spots zählen (0 wenn aufgegeben)
            val playerSpots = if (aufgegeben) 0 else (1..count).count { capturedForIndex(it) }
            val resultsList = buildList {
                add(Ergebnis(spielerName, playerSpots, aufgegeben))
                // Gegner erhalten 0 Spots (da kein Live-Tracking der Gegner implementiert)
                participants.drop(1).forEach { add(Ergebnis(it, 0, false)) }
            }
            // Aufgegebene Spieler ans Ende sortieren; dann nach Spot-Anzahl absteigend
            resultsList.sortedWith(compareBy<Ergebnis> { it.aufgegeben }.thenByDescending { it.spots })
        } else {
            listOf(Ergebnis(spielerName, 0, aufgegeben))
        }

        status = SpielStatus.BEENDET

        // Passende Benachrichtigung je nach Duell-Ausgang
        val nachricht = when {
            success -> "Glückwunsch! Du hast alle Spots in der Zeit erobert."
            aufgegeben -> "Du hast das Duell aufgegeben."
            else -> "Das Zeitlimit ist abgelaufen."
        }
        notificationHelper.sendNotification("TeRun - Duell beendet", nachricht)
    }

    // Zurück zur Karte: Duell-State bereinigen ohne Ergebnis-Screen
    fun zurueckZurKarte() {
        routePoints.clear()
        timerJob?.cancel()
        timerJob = null
        locationHelper.stopLocationUpdates()
        status = SpielStatus.IDLE
        aktivesDuell = null
    }

    // ==========================================================================
    // Spot-Hilfsfunktionen (privat)
    // ==========================================================================

    // Gibt zurück, ob ein Spot mit dem Index 1–5 bereits erreicht wurde
    private fun capturedForIndex(idx: Int): Boolean = when (idx) {
        1 -> spot1Captured
        2 -> spot2Captured
        3 -> spot3Captured
        4 -> spot4Captured
        5 -> spot5Captured
        else -> true // Ungültiger Index gilt als "erreicht" (Sicherheits-Fallback)
    }

    // Gibt die Koordinaten (Lat, Lng) eines Spots aus dem aktiven Duell zurück
    private fun spotCoords(active: Duell, idx: Int): Pair<Double, Double> = when (idx) {
        1 -> active.spot1Lat to active.spot1Lng
        2 -> active.spot2Lat to active.spot2Lng
        3 -> active.spot3Lat to active.spot3Lng
        4 -> active.spot4Lat to active.spot4Lng
        else -> active.spot5Lat to active.spot5Lng
    }

    // Spot als erreicht markieren und Push-Benachrichtigung senden
    private fun captureSpot(idx: Int) {
        when (idx) {
            1 -> spot1Captured = true
            2 -> spot2Captured = true
            3 -> spot3Captured = true
            4 -> spot4Captured = true
            5 -> spot5Captured = true
        }
        notificationHelper.sendNotification("Spot erobert!", "Du hast Spot $idx erobert!")
    }

    // Prüft bei jeder GPS-Aktualisierung, ob der Spieler nah genug an einem Spot oder dem Ziel ist
    private fun checkSpotsCaptured(lat: Double, lng: Double) {
        val active = aktivesDuell ?: return
        for (i in 1..active.spotsAnzahl) {
            if (!capturedForIndex(i)) {
                val (sLat, sLng) = spotCoords(active, i)
                // Radius von 20 Metern um jeden Spot
                if (calculateDistance(lat, lng, sLat, sLng) <= 20.0) captureSpot(i)
            }
        }
        // Wenn alle Spots erreicht sind UND der Spieler am Startpunkt zurück ist → Sieg
        val allCaptured = (1..active.spotsAnzahl).all { capturedForIndex(it) }
        val (tLat, tLng) = (startPositionGeo?.latitude ?: 0.0) to (startPositionGeo?.longitude ?: 0.0)
        if (allCaptured && calculateDistance(lat, lng, tLat, tLng) <= 20.0) {
            duellBeenden(success = true)
        }
    }

    // ==========================================================================
    // Routing (OSRM)
    // ==========================================================================

    // Fußgänger-Route zum nächsten offenen Spot vom OSRM-Server abrufen
    // Throttling: max. alle 3 Sekunden und nur bei Bewegung > 5 Meter
    fun checkAndUpdateRoutePath() {
        val userPos = spielerPosition ?: return
        val active = aktivesDuell ?: return
        if (status != SpielStatus.LAEUFT) return

        // Nächsten noch nicht erreichten Spot als Ziel bestimmen
        val nextSpot = (1..active.spotsAnzahl).firstOrNull { !capturedForIndex(it) }
        val (targetLat, targetLng) = if (nextSpot != null) {
            spotCoords(active, nextSpot)
        } else {
            // Alle Spots erledigt → Startpunkt als Ziel (Rückweg)
            (startPositionGeo?.latitude ?: 0.0) to (startPositionGeo?.longitude ?: 0.0)
        }
        val targetPos = GeoPoint(targetLat, targetLng)

        // Throttle: nicht öfter als alle 3 Sekunden abfragen
        val now = System.currentTimeMillis()
        if (now - lastRouteFetchTime < 3000) return

        // Keine neue Abfrage wenn der Spieler sich weniger als 5 Meter bewegt hat
        val lastPos = lastQueryPos
        if (lastPos != null && calculateDistance(
                userPos.latitude, userPos.longitude,
                lastPos.latitude, lastPos.longitude
            ) < 5.0
        ) return

        lastRouteFetchTime = now
        lastQueryPos = userPos

        // HTTP-Anfrage im IO-Thread; Ergebnis auf Main-Thread zurückschreiben
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // OSRM-API-URL für Fußgänger-Route zusammenbauen
                val urlStr = "https://router.project-osrm.org/route/v1/foot/" +
                        "${userPos.longitude},${userPos.latitude};" +
                        "${targetPos.longitude},${targetPos.latitude}" +
                        "?overview=full&geometries=geojson"
                val conn = URL(urlStr).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000

                if (conn.responseCode == 200) {
                    // JSON-Antwort parsen und GeoPoint-Liste aufbauen
                    val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val coords = routes.getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONArray("coordinates")
                        val tempPoints = mutableListOf<GeoPoint>()
                        for (i in 0 until coords.length()) {
                            val coord = coords.getJSONArray(i)
                            // OSRM liefert [lng, lat] → wir brauchen (lat, lng)
                            tempPoints.add(GeoPoint(coord.getDouble(1), coord.getDouble(0)))
                        }
                        // Route auf dem Main-Thread in den UI-State schreiben
                        withContext(Dispatchers.Main) {
                            routePoints.clear()
                            routePoints.addAll(tempPoints)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace() // Netzwerkfehler ignorieren; alte Route bleibt bestehen
            }
        }
    }

    // ==========================================================================
    // Benutzersuche
    // ==========================================================================

    // Prüft ob ein Benutzer mit diesem Anzeigenamen in der DB existiert (für Freunde/Gegner)
    suspend fun existiertBenutzerMitName(name: String): Boolean =
        repository.existiertBenutzerMitName(name)

    // Durchsucht alle Benutzernamen nach dem eingegebenen Begriff (Autocomplete)
    suspend fun sucheBenutzerNamen(query: String): List<String> =
        repository.sucheBenutzerNamen(query)

    // ==========================================================================
    // Haversine-Distanzberechnung
    // ==========================================================================

    // Berechnet die Luftliniendistanz zwischen zwei GPS-Koordinaten in Metern (Haversine-Formel)
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Erdradius in Metern
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).let { it * it } +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).let { it * it }
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    // ==========================================================================
    // Lifecycle
    // ==========================================================================

    // Wird aufgerufen wenn das ViewModel zerstört wird (z.B. App geschlossen)
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()                  // Timer stoppen
        locationHelper.stopLocationUpdates() // GPS-Updates abmelden
    }
}
