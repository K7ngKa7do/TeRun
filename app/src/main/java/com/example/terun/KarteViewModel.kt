package com.example.terun

import android.app.Application
import android.content.Intent
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.LatLng

// [Cleanup 27.08.2026] Google Maps verwendet LatLng; die frühere OSMDroid-GeoPoint-Verwendung wurde entsprechend angepasst.
import java.util.UUID

/**
 * ============================================================
 * KarteViewModel – Zentrale Spiellogik der App (MVVM-ViewModel)
 * ============================================================
 *
 * VORLESUNG 18 – MVVM (Model-View-ViewModel):
 * Das ViewModel ist die mittlere Schicht zwischen UI und Daten.
 * Es hält den gesamten UI-Zustand und enthält die Spiellogik.
 *
 * Aufgaben des ViewModels:
 * - Stellt Daten für die UI bereit (Spielerstatus, GPS-Position, Duellstatus etc.)
 * - Reagiert auf Benutzeraktionen (Duell starten, Freund hinzufügen, ...)
 * - Weiß NICHTS über konkrete UI-Komponenten (kein Verweis auf Composables)
 * - Überlebt Konfigurationsänderungen wie Bildschirmdrehung
 *
 * AndroidViewModel vs. ViewModel:
 * AndroidViewModel bekommt zusätzlich den Application-Kontext,
 * der benötigt wird um Services zu starten/stoppen (z.B. TeRunLocationService).
 *
 * VORLESUNG 28–30 – Coroutines:
 * Alle Datenbankzugriffe und Netzwerkanfragen laufen in Coroutinen:
 * - viewModelScope.launch { } startet eine Coroutine im ViewModel-Scope
 * - wenn das ViewModel gelöscht wird, werden die Coroutinen automatisch beendet
 *
 * KarteViewModel — Zentrale Spiellogik der App (MVVM-ViewModel).
 * Hält den gesamten Spielzustand und kommuniziert zwischen UI (KarteScreen)
 * und Datenschicht (SpielRepository, LocationHelper, NotificationHelper).
 *
 * [Cleanup 27.08.2026]
 * - Bestehende Kommentare und Vorlesungsbezüge wurden beibehalten.
 * - Bereiche wurden übersichtlicher gegliedert.
 * - Unbenutzte Imports wurden entfernt.
 * - Wiederholte Service- und Spot-Reset-Logik wurde in kleine private
 *   Hilfsmethoden ausgelagert.
 * - Öffentliche Funktionsnamen und das Verhalten des KarteScreens bleiben erhalten.
 */
class KarteViewModel(application: Application) : AndroidViewModel(application) {

    // ==========================================================================
    // Abhängigkeiten
    // ==========================================================================

    // --- Abhängigkeiten ---
    private val repository = SpielRepository(application)         // Datenzugriff (Room + SharedPreferences)
    private val locationHelper = LocationHelper(application)      // GPS-Ortung
    private val notificationHelper = NotificationHelper(application) // Push-Benachrichtigungen

    // [Cleanup 27.08.2026]
    // Repository, GPS-Helfer und NotificationHelper bilden die Datenschicht/
    // Systemanbindung. Das ViewModel koordiniert diese Komponenten für die UI.

    // ==========================================================================
    // Profil
    // ==========================================================================

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

    // ==========================================================================
    // GPS / Position
    // ==========================================================================

    // --- GPS / Position ---

    var spielerPosition by mutableStateOf<LatLng?>(null)  // Aktuelle GPS-Koordinate des Spielers (null = noch kein Fix)
    var startPositionGeo by mutableStateOf<LatLng?>(null) // Startposition bei Duellbeginn (= Zielpunkt nach allen Spots)

    // ==========================================================================
    // Spot-Status
    // ==========================================================================

    // --- Spot-Status ---

    // Jeder Spot hat einen eigenen Boolean-State; true = bereits vom Spieler erreicht
    var spot1Captured by mutableStateOf(false)
    var spot2Captured by mutableStateOf(false)
    var spot3Captured by mutableStateOf(false)
    var spot4Captured by mutableStateOf(false)
    var spot5Captured by mutableStateOf(false)

    // ==========================================================================
    // Daten-Listen
    // ==========================================================================

    // --- Daten-Listen ---

    val duelle = mutableStateListOf<Duell>()   // Alle gespeicherten Duelle (aus Room geladen)
    val freunde = mutableStateListOf<String>() // Anzeigenamen der Freunde des eingeloggten Spielers

    // ==========================================================================
    // Multiplayer- & Live-States
    // ==========================================================================

    // --- Neue Multiplayer- & Live-States ---

    val ausstehendeFreundesanfragen =
        mutableStateListOf<String>() // Eingehende Freundesanfragen

    val ausstehendeDuellEinladungen =
        mutableStateListOf<Duell>() // Eingehende Duell-Einladungen

    val gegnerStati =
        mutableStateMapOf<String, Pair<LatLng, Int>>() // GegnerName -> (Position, eroberte Spots)

    val activeDuelInvitations =
        mutableStateMapOf<String, String>() // GegnerName -> Status (PENDING, ACCEPTED, DECLINED)

    var toastMessage by
    mutableStateOf<String?>(null) // Einfacher Toast-Notifier für Composable-UI

    // [Cleanup 27.08.2026]
    // ListenerRegistration wird nun importiert, damit die Typen hier kürzer
    // und leichter lesbar sind.
    private var duelInvitationsListener: ListenerRegistration? = null
    private var liveSessionListener: ListenerRegistration? = null
    private var acceptedRequestsListener: ListenerRegistration? = null

    // --- Routing (Deaktiviert für Meilenstein 5) ---

    // ==========================================================================
    // Spielzustand
    // ==========================================================================

    // --- Spielzustand ---

    var aktivesDuell by
    mutableStateOf<Duell?>(null) // Das gerade laufende Duell (null = kein Duell aktiv)
        private set

    var status by
    mutableStateOf(SpielStatus.IDLE) // Aktueller Spielstatus: IDLE, LAEUFT oder BEENDET
        private set

    var verbleibendeZeit by
    mutableIntStateOf(0) // Verbleibende Zeit in Sekunden
        private set

    var ergebnisse by
    mutableStateOf<List<Ergebnis>>(emptyList()) // Ergebnisliste nach Duellende
        private set

    private var timerJob: Job? = null // Coroutine-Job für den Countdown-Timer

    // ==========================================================================
    // Initialisierung
    // ==========================================================================

    // --- Initialisierung ---

    // Wird einmalig beim Erstellen des ViewModels ausgeführt — lädt alle persistierten Daten
    init {
        ladeLokaleProfildaten()

        viewModelScope.launch {
            duelle.addAll(repository.holeDuelle()) // Duelle asynchron aus Room laden
        }

        viewModelScope.launch {
            freunde.addAll(
                repository.holeFreunde(repository.getAccountKey())
            ) // Freunde laden

            ladeAusstehendeFreundesanfragen()
            ladeDuellEinladungen()
            starteBeobachtungAngenommeneAnfragen()
        }
    }

    // [Cleanup 27.08.2026]
    // Die drei lokalen Profilwerte wurden lediglich aus init in eine kleine
    // Hilfsmethode verschoben. Die ursprüngliche Lade-Logik bleibt identisch.
    private fun ladeLokaleProfildaten() {
        _spielerName.value =
            repository.ladeSpielerName() // Name aus SharedPreferences laden

        spielerGesamtDistanz =
            repository.ladeGesamtDistanz() // Gesamtdistanz laden

        absolvierteDuelleCount =
            repository.ladeAbsolvierteDuelleCount() // Duellanzahl laden
    }

    // ==========================================================================
    // Freunde
    // ==========================================================================

    // Freundesliste neu aus der Datenbank laden und den State aktualisieren
    fun ladeFreunde() {
        viewModelScope.launch {
            freunde.clear()
            freunde.addAll(
                repository.holeFreunde(repository.getAccountKey())
            )
        }
    }

    // Duellliste neu laden
    fun ladeDuelle() {
        viewModelScope.launch {
            duelle.clear()
            duelle.addAll(repository.holeDuelle())
        }
    }

    // Holt ausstehende Freundschaftsanfragen
    fun ladeAusstehendeFreundesanfragen() {
        viewModelScope.launch {
            ausstehendeFreundesanfragen.clear()

            ausstehendeFreundesanfragen.addAll(
                repository.holeAusstehendeFreundesanfragen(
                    repository.getAccountKey()
                )
            )
        }
    }

    // Antwortet auf eine Freundschaftsanfrage
    fun antworteAufFreundesanfrage(
        senderName: String,
        akzeptiert: Boolean
    ) {
        viewModelScope.launch {
            repository.antworteAufFreundesanfrage(
                repository.getAccountKey(),
                senderName,
                akzeptiert
            )

            ladeAusstehendeFreundesanfragen()
            ladeFreunde()
        }
    }

    // Freund anhand des Anzeigenamens hinzufügen; onResult liefert den Status als String
    fun fuegeFreundHinzu(
        name: String,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result =
                repository.fuegeFreundHinzu(
                    repository.getAccountKey(),
                    name
                )

            if (result == "SUCCESS") {
                ladeAusstehendeFreundesanfragen() // Anfrage wird gesendet
            }

            onResult(result)
        }
    }

    // Startet die Echtzeit-Beobachtung für akzeptierte Anfragen
    fun starteBeobachtungAngenommeneAnfragen() {
        acceptedRequestsListener?.remove()

        acceptedRequestsListener =
            repository.starteBeobachtungAngenommeneAnfragen(
                repository.getAccountKey()
            ) { friendName ->

                toastMessage =
                    "$friendName hat deine Anfrage angenommen"

                ladeFreunde()
            }
    }

    // Freund aus der beidseitigen Freundesliste entfernen
    fun loescheFreund(name: String) {
        viewModelScope.launch {
            repository.loescheFreund(
                repository.getAccountKey(),
                name
            )

            ladeFreunde() // Liste nach Löschen aktualisieren
        }
    }

    // ==========================================================================
    // Duell-Einladungen
    // ==========================================================================

    // Lädt ausstehende Duell-Einladungen
    fun ladeDuellEinladungen() {
        val myName = spielerName

        if (
            myName.isBlank() ||
            !repository.networkMonitor.isOnline.value
        ) {
            return
        }

        repository.firestore
            .collection("duels")
            .whereEqualTo(
                "invitations.$myName",
                "PENDING"
            )
            .get()
            .addOnSuccessListener { result ->

                ausstehendeDuellEinladungen.clear()

                for (doc in result.documents) {
                    val id =
                        doc.getString("id") ?: ""

                    val name =
                        doc.getString("name") ?: ""

                    val spotsAnzahl =
                        doc.getLong("spotsAnzahl")
                            ?.toInt() ?: 1

                    val zeitLimitMinuten =
                        doc.getLong("zeitLimitMinuten")
                            ?.toInt() ?: 30

                    val spot1Lat =
                        doc.getDouble("spot1Lat") ?: 0.0
                    val spot1Lng =
                        doc.getDouble("spot1Lng") ?: 0.0

                    val spot2Lat =
                        doc.getDouble("spot2Lat") ?: 0.0
                    val spot2Lng =
                        doc.getDouble("spot2Lng") ?: 0.0

                    val spot3Lat =
                        doc.getDouble("spot3Lat") ?: 0.0
                    val spot3Lng =
                        doc.getDouble("spot3Lng") ?: 0.0

                    val spot4Lat =
                        doc.getDouble("spot4Lat") ?: 0.0
                    val spot4Lng =
                        doc.getDouble("spot4Lng") ?: 0.0

                    val spot5Lat =
                        doc.getDouble("spot5Lat") ?: 0.0
                    val spot5Lng =
                        doc.getDouble("spot5Lng") ?: 0.0

                    val gegner =
                        doc.getString("gegner") ?: ""

                    val d = Duell(
                        id,
                        name,
                        spotsAnzahl,
                        zeitLimitMinuten,
                        spot1Lat,
                        spot1Lng,
                        spot2Lat,
                        spot2Lng,
                        spot3Lat,
                        spot3Lng,
                        spot4Lat,
                        spot4Lng,
                        spot5Lat,
                        spot5Lng,
                        gegner
                    )

                    ausstehendeDuellEinladungen.add(d)
                }
            }
    }

    // Antwortet auf eine Duell-Einladung
    fun antworteAufDuellEinladung(
        duell: Duell,
        akzeptiert: Boolean
    ) {
        viewModelScope.launch {
            repository.antworteAufDuellEinladung(
                duell.id,
                akzeptiert
            )

            ladeDuellEinladungen()

            if (akzeptiert) {
                repository.speichereDuell(duell)
                ladeDuelle()
            }
        }
    }

    // Beobachtet den Einladungs-Status für den Duell-Ersteller
    fun beobachteDuellEinladungen(duelId: String) {
        duelInvitationsListener?.remove()
        activeDuelInvitations.clear()

        if (!repository.networkMonitor.isOnline.value) {
            return
        }

        duelInvitationsListener =
            repository.firestore
                .collection("duels")
                .document(duelId)
                .addSnapshotListener { snapshot, _ ->

                    if (
                        snapshot != null &&
                        snapshot.exists()
                    ) {
                        @Suppress("UNCHECKED_CAST")
                        val invitations =
                            snapshot.get("invitations")
                                    as? Map<String, String>
                                ?: emptyMap()

                        for ((gegnerName, neuerStatus) in invitations) {
                            val alterStatus =
                                activeDuelInvitations[gegnerName]

                            if (
                                alterStatus != null &&
                                alterStatus != neuerStatus
                            ) {
                                toastMessage =
                                    "Spieler $gegnerName hat das Spiel $neuerStatus!"
                            }

                            activeDuelInvitations[gegnerName] =
                                neuerStatus
                        }
                    }
                }
    }

    fun stoppeDuellEinladungenBeobachtung() {
        duelInvitationsListener?.remove()
        duelInvitationsListener = null
        activeDuelInvitations.clear()
    }

    // ==========================================================================
    // Duell-Verwaltung
    // ==========================================================================

    // Neues Duell erstellen und sowohl im UI-State als auch in der Room-DB speichern
    fun erstelleDuell(
        name: String,
        zeitLimitMinuten: Int,
        spotsList: List<LatLng>,
        gegner: String
    ) {
        // Hilfsfunktionen zum sicheren Zugriff auf Spot-Koordinaten (0.0 wenn kein Spot an dieser Stelle)
        fun lat(i: Int) =
            spotsList.getOrNull(i)?.latitude ?: 0.0

        fun lng(i: Int) =
            spotsList.getOrNull(i)?.longitude ?: 0.0

        val neuesDuell = Duell(
            id = UUID.randomUUID().toString(), // Eindeutige ID generieren
            name = name,
            spotsAnzahl = spotsList.size.coerceIn(1, 5), // Mindestens 1, maximal 5 Spots erlaubt
            zeitLimitMinuten = zeitLimitMinuten,

            spot1Lat = lat(0),
            spot1Lng = lng(0),

            spot2Lat = lat(1),
            spot2Lng = lng(1),

            spot3Lat = lat(2),
            spot3Lng = lng(2),

            spot4Lat = lat(3),
            spot4Lng = lng(3),

            spot5Lat = lat(4),
            spot5Lng = lng(4),

            gegner = gegner
        )

        duelle.add(neuesDuell) // Sofort im UI anzeigen

        viewModelScope.launch {
            repository.speichereDuell(neuesDuell) // Asynchron in Room speichern
        }
    }

    // Duell aus UI-Liste und Room-Datenbank löschen
    fun loescheDuell(duell: Duell) {
        duelle.remove(duell)

        viewModelScope.launch {
            repository.loescheDuell(duell)
        }
    }

    // ==========================================================================
    // Profil
    // ==========================================================================

    // Eigenes Konto vollständig löschen: DB-Einträge, SharedPreferences und UI-State zurücksetzen
    fun loescheProfil(onDeleted: () -> Unit) {
        val email = repository.getAccountKey()

        viewModelScope.launch {
            repository.loescheKonto(email) // Benutzer + Freundesliste aus DB löschen

            spielerName = ""
            spielerGesamtDistanz = 0.0
            absolvierteDuelleCount = 0

            repository.speichereGesamtDistanz(0.0)
            repository.speichereAbsolvierteDuelleCount(0)

            repository.setAccountKey("") // Account-Key leeren = ausgeloggt

            onDeleted() // Callback: UI auf Login-Screen weiterleiten
        }
    }

    // ==========================================================================
    // Standort & Spielstart
    // ==========================================================================

    // GPS-Updates starten; bei jeder neuen Position: Distanz berechnen, Spots prüfen, Route aktualisieren
    fun starteStandortAbfrage() {

        // Empfange Updates vom Foreground Service, falls dieser läuft
        TeRunLocationService.onLocationReceived = { location ->
            onLocationUpdated(location)
        }

        // Starte normale Ortung nur im Vordergrund, falls kein Duell aktiv ist
        if (status != SpielStatus.LAEUFT) {
            locationHelper.startLocationUpdates { location ->
                onLocationUpdated(location)
            }
        }
    }

    private fun onLocationUpdated(location: Location) {
        val prevPos = spielerPosition

        val currentGeo =
            LatLng(
                location.latitude,
                location.longitude
            )

        spielerPosition = currentGeo

        // Distanz nur während laufendem Duell und wenn eine Vorposition bekannt ist
        if (
            status == SpielStatus.LAEUFT &&
            prevPos != null
        ) {
            spielerGesamtDistanz +=
                calculateDistance(
                    prevPos.latitude,
                    prevPos.longitude,
                    currentGeo.latitude,
                    currentGeo.longitude
                ) / 1000.0 // Meter → Kilometer

            repository.speichereGesamtDistanz(
                spielerGesamtDistanz
            )
        }

        // Spot-Erkennung nur während laufendem Duell
        if (status == SpielStatus.LAEUFT) {
            checkSpotsCaptured(
                location.latitude,
                location.longitude
            )

            // Live-Position & Score an die Multiplayer-Session senden
            val active = aktivesDuell

            if (active != null) {
                val score =
                    (1..active.spotsAnzahl)
                        .count { capturedForIndex(it) }

                repository.updateLiveSession(
                    active.id,
                    spielerName,
                    location.latitude,
                    location.longitude,
                    score
                )
            }
        }
    }

    // Duell starten: Zustand zurücksetzen, Timer starten, GPS aktivieren
    fun duellStarten(duell: Duell) {
        aktivesDuell = duell
        status = SpielStatus.LAEUFT

        verbleibendeZeit =
            duell.zeitLimitMinuten * 60 // Minuten → Sekunden umrechnen

        // Alle Spots als nicht erreicht markieren
        resetSpotStatus()

        startPositionGeo =
            spielerPosition // Startpunkt merken (= späterer Zielpunkt nach allen Spots)

        // Snapshot-Listener für Multiplayer-Gegner und Scores starten
        startLiveSessionBeobachtung(duell.id)

        // Foreground Service für Hintergrundortung starten
        starteLocationService()

        starteStandortAbfrage()

        timerJob?.cancel() // Eventuell laufenden alten Timer stoppen

        timerJob =
            viewModelScope.launch {
                try {
                    // Jede Sekunde herunterzählen
                    while (verbleibendeZeit > 0) {
                        delay(1000L)
                        verbleibendeZeit--
                    }

                    // Zeit abgelaufen → Duell automatisch beenden (kein Erfolg)
                    duellBeenden(success = false)

                } catch (_: CancellationException) {
                    // Timer wurde manuell gestoppt → keine Aktion nötig
                }
            }
    }

    // [Cleanup 27.08.2026]
    // Ursprünglicher Spot-Reset aus duellStarten() in Hilfsmethode ausgelagert.
    private fun resetSpotStatus() {
        spot1Captured = false
        spot2Captured = false
        spot3Captured = false
        spot4Captured = false
        spot5Captured = false
    }

    // [Cleanup 27.08.2026]
    // Foreground-Service-Start zentralisiert. Verhalten entspricht dem
    // ursprünglichen Code in duellStarten().
    private fun starteLocationService() {
        val context =
            getApplication<Application>()
                .applicationContext

        val serviceIntent =
            Intent(
                context,
                TeRunLocationService::class.java
            )

        try {
            if (
                android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.O
            ) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // [Cleanup 27.08.2026]
    // Foreground-Service-Stop zentralisiert. Wird bei Duellende und onCleared()
    // verwendet, damit dieselbe Logik nicht doppelt vorhanden ist.
    private fun stoppeLocationService() {
        val context =
            getApplication<Application>()
                .applicationContext

        val serviceIntent =
            Intent(
                context,
                TeRunLocationService::class.java
            )

        try {
            context.stopService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==========================================================================
    // Duell beenden
    // ==========================================================================

    // Duell beenden: Ergebnisse berechnen, Status setzen, Benachrichtigung senden
    fun duellBeenden(
        success: Boolean = false,
        aufgegeben: Boolean = false
    ) {
        timerJob?.cancel()
        timerJob = null

        stoppeLiveSessionBeobachtung()

        val active = aktivesDuell

        if (active != null) {
            repository.loescheLiveSession(active.id)
        }

        // Foreground Service für Hintergrundortung stoppen
        stoppeLocationService()

        locationHelper.stopLocationUpdates()
        TeRunLocationService.onLocationReceived = null

        absolvierteDuelleCount += 1

        repository.speichereAbsolvierteDuelleCount(
            absolvierteDuelleCount
        )

        // Ergebnisliste zusammenbauen
        ergebnisse =
            if (active != null) {
                val count = active.spotsAnzahl

                // Alle Teilnehmer: eigener Name zuerst, dann Gegner aus kommasepariertem String
                val participants =
                    buildList {
                        add(spielerName)

                        if (active.gegner.isNotEmpty()) {
                            addAll(
                                active.gegner
                                    .split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                            )
                        }
                    }

                // Eigene Spots zählen (0 wenn aufgegeben)
                val playerSpots =
                    if (aufgegeben) {
                        0
                    } else {
                        (1..count)
                            .count { capturedForIndex(it) }
                    }

                val resultsList =
                    buildList {
                        add(
                            Ergebnis(
                                spielerName,
                                playerSpots,
                                aufgegeben
                            )
                        )

                        // Live-Scores der Gegner aus dem Firebase-Listener übernehmen
                        participants
                            .drop(1)
                            .forEach { opponentName ->

                                val opponentSpots =
                                    gegnerStati[opponentName]
                                        ?.second ?: 0

                                add(
                                    Ergebnis(
                                        opponentName,
                                        opponentSpots,
                                        false
                                    )
                                )
                            }
                    }

                // Aufgegebene Spieler ans Ende sortieren; dann nach Spot-Anzahl absteigend
                resultsList.sortedWith(
                    compareBy<Ergebnis> { it.aufgegeben }
                        .thenByDescending { it.spots }
                )

            } else {
                listOf(
                    Ergebnis(
                        spielerName,
                        0,
                        aufgegeben
                    )
                )
            }

        status = SpielStatus.BEENDET

        // Passende Benachrichtigung je nach Duell-Ausgang
        val nachricht =
            when {
                success ->
                    "Glückwunsch! Du hast alle Spots in der Zeit erobert."

                aufgegeben ->
                    "Du hast das Duell aufgegeben."

                else ->
                    "Das Zeitlimit ist abgelaufen."
            }

        notificationHelper.sendNotification(
            "TeRun - Duell beendet",
            nachricht
        )
    }

    // Zurück zur Karte: Duell-State bereinigen ohne Ergebnis-Screen
    fun zurueckZurKarte() {
        timerJob?.cancel()
        timerJob = null

        stoppeLiveSessionBeobachtung()

        val active = aktivesDuell

        if (active != null) {
            repository.loescheLiveSession(active.id)
        }

        locationHelper.stopLocationUpdates()

        status = SpielStatus.IDLE
        aktivesDuell = null
    }

    // ==========================================================================
    // Multiplayer Live-Session
    // ==========================================================================

    // Beobachtet das gegnerische Tracking in Echtzeit
    fun startLiveSessionBeobachtung(duelId: String) {
        liveSessionListener?.remove()
        gegnerStati.clear()

        if (!repository.networkMonitor.isOnline.value) {
            return
        }

        liveSessionListener =
            repository.firestore
                .collection("duel_sessions")
                .document(duelId)
                .addSnapshotListener { snapshot, _ ->

                    if (
                        snapshot != null &&
                        snapshot.exists()
                    ) {
                        val data =
                            snapshot.data
                                ?: return@addSnapshotListener

                        for ((name, valueMap) in data) {

                            if (name == spielerName) {
                                continue // Eigenen State überspringen
                            }

                            @Suppress("UNCHECKED_CAST")
                            val m =
                                valueMap as? Map<String, Any>
                                    ?: continue

                            val lat =
                                (m["lat"] as? Number)
                                    ?.toDouble() ?: 0.0

                            val lng =
                                (m["lng"] as? Number)
                                    ?.toDouble() ?: 0.0

                            val spots =
                                (m["spotsCaptured"] as? Number)
                                    ?.toInt() ?: 0

                            val alterState =
                                gegnerStati[name]

                            if (
                                alterState != null &&
                                spots > alterState.second
                            ) {
                                toastMessage =
                                    "Spieler $name hat einen Spot erobert!"
                            }

                            gegnerStati[name] =
                                LatLng(lat, lng) to spots
                        }
                    }
                }
    }

    fun stoppeLiveSessionBeobachtung() {
        liveSessionListener?.remove()
        liveSessionListener = null
        gegnerStati.clear()
    }

    // ==========================================================================
    // Spot-Hilfsfunktionen (privat)
    // ==========================================================================

    // Gibt zurück, ob ein Spot mit dem Index 1–5 bereits erreicht wurde
    private fun capturedForIndex(idx: Int): Boolean =
        when (idx) {
            1 -> spot1Captured
            2 -> spot2Captured
            3 -> spot3Captured
            4 -> spot4Captured
            5 -> spot5Captured
            else -> true // Ungültiger Index gilt als "erreicht" (Sicherheits-Fallback)
        }

    // Gibt die Koordinaten (Lat, Lng) eines Spots aus dem aktiven Duell zurück
    private fun spotCoords(
        active: Duell,
        idx: Int
    ): Pair<Double, Double> =
        when (idx) {
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

        notificationHelper.sendNotification(
            "Spot erobert!",
            "Du hast Spot $idx erobert!"
        )

        // Sofortiges Update an die Live-Session senden
        val active = aktivesDuell
        val pos = spielerPosition

        if (
            active != null &&
            pos != null
        ) {
            val score =
                (1..active.spotsAnzahl)
                    .count { capturedForIndex(it) }

            repository.updateLiveSession(
                active.id,
                spielerName,
                pos.latitude,
                pos.longitude,
                score
            )
        }
    }

    // Prüft bei jeder GPS-Aktualisierung, ob der Spieler nah genug an einem Spot oder dem Ziel ist
    private fun checkSpotsCaptured(
        lat: Double,
        lng: Double
    ) {
        val active =
            aktivesDuell ?: return

        for (i in 1..active.spotsAnzahl) {
            if (!capturedForIndex(i)) {
                val (sLat, sLng) =
                    spotCoords(active, i)

                // Radius von 20 Metern um jeden Spot
                if (
                    calculateDistance(
                        lat,
                        lng,
                        sLat,
                        sLng
                    ) <= SPOT_RADIUS_METER
                ) {
                    captureSpot(i)
                }
            }
        }

        // Wenn alle Spots erreicht sind UND der Spieler am Startpunkt zurück ist → Sieg
        val allCaptured =
            (1..active.spotsAnzahl)
                .all { capturedForIndex(it) }

        val startPosition =
            startPositionGeo

        // [Cleanup 27.08.2026]
        // Nur prüfen, wenn beim Duellstart tatsächlich eine GPS-Position bekannt war.
        if (
            allCaptured &&
            startPosition != null &&
            calculateDistance(
                lat,
                lng,
                startPosition.latitude,
                startPosition.longitude
            ) <= SPOT_RADIUS_METER
        ) {
            duellBeenden(success = true)
        }
    }

    // ==========================================================================
    // Benutzersuche
    // ==========================================================================

    // Prüft ob ein Benutzer mit diesem Anzeigenamen in der DB existiert (für Freunde/Gegner)
    suspend fun existiertBenutzerMitName(
        name: String
    ): Boolean =
        repository.existiertBenutzerMitName(name)

    // Durchsucht alle Benutzernamen nach dem eingegebenen Begriff (Autocomplete)
    suspend fun sucheBenutzerNamen(
        query: String
    ): List<String> =
        repository.sucheBenutzerNamen(query)

    // ==========================================================================
    // Haversine-Distanzberechnung
    // ==========================================================================

    // Berechnet die Luftliniendistanz zwischen zwei GPS-Koordinaten in Metern (Haversine-Formel)
    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val r =
            6371000.0 // Erdradius in Metern

        val dLat =
            Math.toRadians(lat2 - lat1)

        val dLon =
            Math.toRadians(lon2 - lon1)

        val a =
            Math.sin(dLat / 2).let { it * it } +
                    Math.cos(Math.toRadians(lat1)) *
                    Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLon / 2).let { it * it }

        return r * 2 *
                Math.atan2(
                    Math.sqrt(a),
                    Math.sqrt(1 - a)
                )
    }

    // ==========================================================================
    // Lifecycle
    // ==========================================================================

    // Wird aufgerufen wenn das ViewModel zerstört wird (z.B. App geschlossen)
    override fun onCleared() {
        super.onCleared()

        timerJob?.cancel() // Timer stoppen

        stoppeDuellEinladungenBeobachtung()
        stoppeLiveSessionBeobachtung()

        acceptedRequestsListener?.remove()
        acceptedRequestsListener = null

        // [Cleanup 27.08.2026]
        // Gleiche zentrale Methode wie beim normalen Duellende verwenden.
        stoppeLocationService()

        locationHelper.stopLocationUpdates() // GPS-Updates abmelden
        TeRunLocationService.onLocationReceived = null
    }

    companion object {

        // [Cleanup 27.08.2026]
        // Der 20-Meter-Radius war vorher mehrfach direkt als Zahl im Code vorhanden.
        private const val SPOT_RADIUS_METER = 20.0
    }
}
