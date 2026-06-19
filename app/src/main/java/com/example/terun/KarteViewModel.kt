package com.example.terun

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class KarteViewModel : ViewModel() {

    private val repository = SpielRepository()

    var status by mutableStateOf(SpielStatus.IDLE)
        private set

    var ergebnisse by mutableStateOf<List<Ergebnis>>(emptyList())
        private set

    fun duellStarten() {
        status = SpielStatus.LAEUFT
    }

    fun duellBeenden() {
        ergebnisse = repository.ladeErgebnisse()
        status = SpielStatus.BEENDET
    }

    fun zurueckZurKarte() {
        status = SpielStatus.IDLE
    }
}
