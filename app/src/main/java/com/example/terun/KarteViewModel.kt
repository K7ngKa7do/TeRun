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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class KarteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SpielRepository(application)
    private val locationHelper = LocationHelper(application)
    private val notificationHelper = NotificationHelper(application)

    // Profil
    private var _spielerName = mutableStateOf("Spieler1")
    var spielerName: String
        get() = _spielerName.value
        set(value) { _spielerName.value = value; repository.speichereSpielerName(value) }

    private var _teamName = mutableStateOf("Team Blau")
    var teamName: String
        get() = _teamName.value
        set(value) { _teamName.value = value; repository.speichereTeamName(value) }

    var spielerGesamtPunkte by mutableIntStateOf(0)
        private set
    var spielerGesamtDistanz by mutableStateOf(0.0)
    var absolvierteDuelleCount by mutableIntStateOf(0)

    // GPS / Position
    var spielerPosition by mutableStateOf<GeoPoint?>(null)
    var startPositionGeo by mutableStateOf<GeoPoint?>(null)
    var simulationActive by mutableStateOf(false)

    // Spots (index 0–4)
    var spot1Captured by mutableStateOf(false)
    var spot2Captured by mutableStateOf(false)
    var spot3Captured by mutableStateOf(false)
    var spot4Captured by mutableStateOf(false)
    var spot5Captured by mutableStateOf(false)

    // Daten-Listen
    val duelle = mutableStateListOf<Duell>()
    val rangliste = mutableStateListOf<Ergebnis>()
    val freunde = mutableStateListOf<String>()

    var aktivesDuell by mutableStateOf<Duell?>(null)
        private set
    var status by mutableStateOf(SpielStatus.IDLE)
        private set
    var verbleibendeZeit by mutableIntStateOf(300)
        private set
    var ergebnisse by mutableStateOf<List<Ergebnis>>(emptyList())
        private set

    private var timerJob: Job? = null

    init {
        _spielerName.value = repository.ladeSpielerName()
        _teamName.value = repository.ladeTeamName()
        spielerGesamtPunkte = repository.ladeGesamtPunkte()
        spielerGesamtDistanz = repository.ladeGesamtDistanz()
        absolvierteDuelleCount = repository.ladeAbsolvierteDuelleCount()
        viewModelScope.launch { duelle.addAll(repository.holeDuelle()) }
        viewModelScope.launch { freunde.addAll(repository.holeFreunde()) }
        ladeRangliste()
    }

    // Freunde
    fun ladeFreunde() {
        viewModelScope.launch {
            freunde.clear()
            freunde.addAll(repository.holeFreunde())
        }
    }

    fun fuegeFreundHinzu(name: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.fuegeFreundHinzu(name)
            if (success) ladeFreunde()
            onResult(success)
        }
    }

    fun loescheFreund(name: String) {
        viewModelScope.launch {
            repository.loescheFreund(name)
            ladeFreunde()
        }
    }

    // Duell-Verwaltung
    fun erstelleDuell(name: String, zeitLimitMinuten: Int, spotsList: List<GeoPoint>, gegner: String) {
        val defaults = listOf(
            GeoPoint(50.9355, 6.9860), GeoPoint(50.9340, 6.9840),
            GeoPoint(50.9360, 6.9845), GeoPoint(50.9350, 6.9850), GeoPoint(50.9365, 6.9835)
        )
        fun s(i: Int) = spotsList.getOrNull(i) ?: defaults[i]
        val neuesDuell = Duell(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            spotsAnzahl = spotsList.size.coerceIn(1, 5),
            zeitLimitMinuten = zeitLimitMinuten,
            spot1Lat = s(0).latitude, spot1Lng = s(0).longitude,
            spot2Lat = s(1).latitude, spot2Lng = s(1).longitude,
            spot3Lat = s(2).latitude, spot3Lng = s(2).longitude,
            spot4Lat = s(3).latitude, spot4Lng = s(3).longitude,
            spot5Lat = s(4).latitude, spot5Lng = s(4).longitude,
            gegner = gegner
        )
        duelle.add(neuesDuell)
        viewModelScope.launch { repository.speichereDuell(neuesDuell) }
    }

    fun loescheDuell(duell: Duell) {
        duelle.remove(duell)
        viewModelScope.launch { repository.loescheDuell(duell) }
    }

    fun loescheProfil() {
        spielerName = "Spieler1"
        teamName = "Team Blau"
        spielerGesamtPunkte = 0
        spielerGesamtDistanz = 0.0
        absolvierteDuelleCount = 0
        repository.speichereGesamtPunkte(0)
        repository.speichereGesamtDistanz(0.0)
        repository.speichereAbsolvierteDuelleCount(0)
    }

    fun starteStandortAbfrage() {
        locationHelper.startLocationUpdates { location ->
            val prevPos = spielerPosition
            val currentGeo = GeoPoint(location.latitude, location.longitude)
            spielerPosition = currentGeo
            if (status == SpielStatus.LAEUFT && prevPos != null && !simulationActive) {
                spielerGesamtDistanz += calculateDistance(prevPos.latitude, prevPos.longitude, currentGeo.latitude, currentGeo.longitude) / 1000.0
                repository.speichereGesamtDistanz(spielerGesamtDistanz)
            }
            if (status == SpielStatus.LAEUFT && !simulationActive) {
                checkSpotsCaptured(location.latitude, location.longitude)
            }
        }
    }

    fun duellStarten(duell: Duell) {
        aktivesDuell = duell
        status = SpielStatus.LAEUFT
        verbleibendeZeit = duell.zeitLimitMinuten * 60
        spot1Captured = false; spot2Captured = false; spot3Captured = false
        spot4Captured = false; spot5Captured = false

        val startPos = spielerPosition ?: GeoPoint(50.9348, 6.9852)
        spielerPosition = startPos
        startPositionGeo = startPos

        aktivesDuell = duell.copy(
            spot1Lat = startPos.latitude + 0.0005, spot1Lng = startPos.longitude + 0.0006,
            spot2Lat = startPos.latitude - 0.0005, spot2Lng = startPos.longitude - 0.0004,
            spot3Lat = startPos.latitude + 0.0007, spot3Lng = startPos.longitude - 0.0005,
            spot4Lat = startPos.latitude - 0.0007, spot4Lng = startPos.longitude + 0.0005,
            spot5Lat = startPos.latitude + 0.0008, spot5Lng = startPos.longitude + 0.0004
        )

        starteStandortAbfrage()
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            try {
                while (verbleibendeZeit > 0) {
                    delay(1000L)
                    verbleibendeZeit--
                    if (simulationActive) simuliereBewegungsStep()
                }
                duellBeenden(success = false)
            } catch (_: CancellationException) {}
        }
    }

    fun duellBeenden(success: Boolean = false) {
        timerJob?.cancel(); timerJob = null
        locationHelper.stopLocationUpdates()
        simulationActive = false
        val punkteGewonnen = if (success) 300 + (verbleibendeZeit / 10) * 10 else 50
        spielerGesamtPunkte += punkteGewonnen
        absolvierteDuelleCount += 1
        repository.speichereGesamtPunkte(spielerGesamtPunkte)
        repository.speichereAbsolvierteDuelleCount(absolvierteDuelleCount)
        viewModelScope.launch {
            repository.speichereErgebnis(spielerName, punkteGewonnen)
            ergebnisse = listOf(Ergebnis(spielerName, punkteGewonnen))
            status = SpielStatus.BEENDET
            ladeRangliste()
            notificationHelper.sendNotification(
                "TeRun - Duell beendet",
                if (success) "Glückwunsch! Du hast alle Spots in der Zeit erobert und $punkteGewonnen Punkte erzielt."
                else "Das Duell wurde beendet. Du hast $punkteGewonnen Trostpunkte erhalten."
            )
        }
    }

    private fun capturedForIndex(idx: Int) = when (idx) {
        1 -> spot1Captured; 2 -> spot2Captured; 3 -> spot3Captured
        4 -> spot4Captured; 5 -> spot5Captured; else -> true
    }

    private fun spotCoords(active: Duell, idx: Int): Pair<Double, Double> = when (idx) {
        1 -> active.spot1Lat to active.spot1Lng
        2 -> active.spot2Lat to active.spot2Lng
        3 -> active.spot3Lat to active.spot3Lng
        4 -> active.spot4Lat to active.spot4Lng
        else -> active.spot5Lat to active.spot5Lng
    }

    private fun captureSpot(idx: Int) {
        when (idx) {
            1 -> spot1Captured = true; 2 -> spot2Captured = true; 3 -> spot3Captured = true
            4 -> spot4Captured = true; 5 -> spot5Captured = true
        }
        notificationHelper.sendNotification("Spot erobert!", "Du hast Spot $idx erobert!")
    }

    private fun checkSpotsCaptured(lat: Double, lng: Double) {
        val active = aktivesDuell ?: return
        for (i in 1..active.spotsAnzahl) {
            if (!capturedForIndex(i)) {
                val (sLat, sLng) = spotCoords(active, i)
                if (calculateDistance(lat, lng, sLat, sLng) <= 20.0) captureSpot(i)
            }
        }
        val allCaptured = (1..active.spotsAnzahl).all { capturedForIndex(it) }
        val (tLat, tLng) = (startPositionGeo?.latitude ?: 50.9348) to (startPositionGeo?.longitude ?: 6.9852)
        if (allCaptured && calculateDistance(lat, lng, tLat, tLng) <= 20.0) duellBeenden(success = true)
    }

    private fun simuliereBewegungsStep() {
        val current = spielerPosition ?: GeoPoint(50.9348, 6.9852)
        val active = aktivesDuell ?: return

        val nextOpenSpot = (1..active.spotsAnzahl).firstOrNull { !capturedForIndex(it) }
        val (targetLat, targetLng) = if (nextOpenSpot != null) {
            spotCoords(active, nextOpenSpot)
        } else {
            (startPositionGeo?.latitude ?: 50.9348) to (startPositionGeo?.longitude ?: 6.9852)
        }

        val dist = calculateDistance(current.latitude, current.longitude, targetLat, targetLng)
        if (dist <= 20.0) {
            if (nextOpenSpot != null) captureSpot(nextOpenSpot) else duellBeenden(success = true)
        } else {
            val angle = Math.atan2(targetLat - current.latitude, targetLng - current.longitude)
            spielerPosition = GeoPoint(
                current.latitude + 0.0001 * Math.sin(angle),
                current.longitude + 0.0001 * Math.cos(angle)
            )
            spielerGesamtDistanz += 0.011
            repository.speichereGesamtDistanz(spielerGesamtDistanz)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).let { it * it } +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).let { it * it }
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    fun zurueckZurKarte() {
        timerJob?.cancel(); timerJob = null
        locationHelper.stopLocationUpdates()
        status = SpielStatus.IDLE
        aktivesDuell = null
        simulationActive = false
    }

    fun ladeRangliste() {
        viewModelScope.launch {
            val list = repository.holeRangliste()
            rangliste.clear()
            rangliste.addAll(list)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        locationHelper.stopLocationUpdates()
    }
}
