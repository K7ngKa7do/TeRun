// Datei: KarteViewModel.kt
// Paket: com.example.terun
// Quelle: moco202614recompositionstates.pdf — Compose State in ViewModels
// Quelle: moco202618mvvm.pdf — MVVM-Architektur, ViewModel als Halter des UI-Zustands
// Quelle: moco202628coroutines.pdf — Asynchrone Programmierung mit Coroutines und viewModelScope
// Quelle: moco202629jobs.pdf — Job-Referenzen und Cancellation von Hintergrund-Tasks

package com.example.terun

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Das ViewModel für den Karten-Bildschirm verwaltet den Zustand des Spiels (MVVM)
class KarteViewModel : ViewModel() {

    private val repository = SpielRepository()

    // Der aktuelle Status des Duells (Compose State)
    var status by mutableStateOf(SpielStatus.IDLE)
        private set

    // Endstandergebnisse nach Spielende
    var ergebnisse by mutableStateOf<List<Ergebnis>>(emptyList())
        private set

    // Verbleibende Spielzeit in Sekunden (Simulierte Nebenläufigkeit)
    var verbleibendeZeit by mutableIntStateOf(300)
        private set

    // Referenz auf den laufenden Timer-Job (für Abbruch/Abgabe)
    private var timerJob: Job? = null

    // Startet das Duell, initialisiert die Zeit und startet den asynchronen Timer in einer Coroutine
    fun duellStarten() {
        status = SpielStatus.LAEUFT
        verbleibendeZeit = 300 // 5 Minuten Startzeit

        // Abbruch eines eventuell noch laufenden alten Jobs
        timerJob?.cancel()

        // Startet eine Coroutine im viewModelScope (Nebenläufigkeit auf dem Main-Thread mit nicht-blockierendem delay)
        // Quelle: moco202628coroutines.pdf — viewModelScope.launch
        timerJob = viewModelScope.launch {
            try {
                while (verbleibendeZeit > 0) {
                    // Quelle: moco202628coroutines.pdf — delay (nicht-blockierendes Warten)
                    delay(1000L)
                    verbleibendeZeit--
                }
                // Wenn der Countdown abgelaufen ist, beende das Duell automatisch
                duellBeenden()
            } catch (e: CancellationException) {
                // Die Coroutine wurde ordnungsgemäß abgebrochen (z. B. durch Aufgeben)
                // Quelle: moco202629jobs.pdf — Coroutine Cancellation
            }
        }
    }

    // Beendet das Duell (Entweder durch Aufgeben oder Ablauf der Zeit)
    fun duellBeenden() {
        // Laufenden Timer-Job abbrechen
        timerJob?.cancel()
        timerJob = null

        // Ergebnisse aus dem Model (Repository) laden
        ergebnisse = repository.ladeErgebnisse()
        status = SpielStatus.BEENDET
    }

    // Setzt das Spiel in den Ausgangszustand zurück
    fun zurueckZurKarte() {
        timerJob?.cancel()
        timerJob = null
        status = SpielStatus.IDLE
    }

    // Wird aufgerufen, wenn das ViewModel zerstört wird
    override fun onCleared() {
        super.onCleared()
        // Zusätzliche Absicherung: Timer abbrechen
        timerJob?.cancel()
    }
}
