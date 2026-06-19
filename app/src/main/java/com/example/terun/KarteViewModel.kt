// Datei: KarteViewModel.kt
// Paket: com.example.terun
// Quelle: moco202614recompositionstates.pdf — Compose State in ViewModels
// Quelle: moco202618mvvm.pdf — MVVM-Architektur, ViewModel hält den UI-Zustand
// Quelle: moco202628coroutines.pdf — Nebenläufigkeit mit Coroutines und viewModelScope
// Quelle: moco202629jobs.pdf — Job-Referenzen und Cancellation von Hintergrund-Tasks

package com.example.terun

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Das ViewModel für die gesamte TeRun App (Shared ViewModel).
// Hält den dynamischen UI-Zustand und ermöglicht das Testen aller Abläufe.
class KarteViewModel : ViewModel() {

    private val repository = SpielRepository()

    // 1. Profil-Zustand (Dynamisch & Editierbar)
    var spielerName by mutableStateOf("Spieler1")
    var teamName by mutableStateOf("Team Blau")
    var spielerGesamtPunkte by mutableIntStateOf(0)

    // 2. Duelle-Zustand (Dynamisch)
    val duelle = mutableStateListOf<Duell>()
    
    // Das aktuell aktive Duell
    var aktivesDuell by mutableStateOf<Duell?>(null)
        private set

    // 3. Spielzustand (Echtzeit-Map-Zustand)
    var status by mutableStateOf(SpielStatus.IDLE)
        private set

    var verbleibendeZeit by mutableIntStateOf(300)
        private set

    var ergebnisse by mutableStateOf<List<Ergebnis>>(emptyList())
        private set

    private var timerJob: Job? = null

    init {
        // Initialisiere die Duell-Liste aus dem Repository
        duelle.addAll(repository.duelleListe)
    }

    // Fügt ein neues Duell dynamisch hinzu
    fun erstelleDuell(name: String, spots: Int, zeitInMinuten: Int) {
        val neuesDuell = Duell(
            id = (duelle.size + 1).toString(),
            name = name,
            spotsAnzahl = spots,
            zeitLimitMinuten = zeitInMinuten
        )
        duelle.add(neuesDuell)
    }

    // Startet ein bestimmtes Duell und setzt den asynchronen Timer in Gang
    fun duellStarten(duell: Duell) {
        aktivesDuell = duell
        status = SpielStatus.LAEUFT
        verbleibendeZeit = duell.zeitLimitMinuten * 60

        // Alten Timer abbrechen
        timerJob?.cancel()

        // Asynchroner Countdown über Coroutine im viewModelScope
        timerJob = viewModelScope.launch {
            try {
                while (verbleibendeZeit > 0) {
                    delay(1000L)
                    verbleibendeZeit--
                }
                // Bei Zeitablauf automatisch beenden
                duellBeenden()
            } catch (e: CancellationException) {
                // Job abgebrochen (Aufgeben)
            }
        }
    }

    // Beendet das laufende Duell, berechnet Punkte und aktualisiert Profile/Ranglisten
    fun duellBeenden() {
        timerJob?.cancel()
        timerJob = null

        // Punkte berechnen: 300 Punkte Basis + Bonus für übrig gebliebene Zeit
        val punkteGewonnen = 300 + (verbleibendeZeit / 10) * 10
        spielerGesamtPunkte += punkteGewonnen

        // Ergebnisse dieses Duells generieren
        ergebnisse = repository.ladeErgebnisse(punkteGewonnen)
        
        // Globale Rangliste aktualisieren
        repository.updateRangliste(punkteGewonnen)

        status = SpielStatus.BEENDET
    }

    // Setzt das Duell zurück
    fun zurueckZurKarte() {
        timerJob?.cancel()
        timerJob = null
        status = SpielStatus.IDLE
        aktivesDuell = null
    }

    // Liefert die sortierte globale Rangliste zurück (unter Einbezug der dynamischen Spieler-Werte)
    fun holeRangliste(): List<Ergebnis> {
        val sortierteListe = repository.rangliste.map { ergebnis ->
            if (ergebnis.name == "Du") {
                Ergebnis(spielerName, spielerGesamtPunkte)
            } else {
                ergebnis
            }
        }
        return sortierteListe.sortedByDescending { it.punkte }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
