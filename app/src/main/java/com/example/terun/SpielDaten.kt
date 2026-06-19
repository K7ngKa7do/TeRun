// Datei: SpielDaten.kt
// Paket: com.example.terun
// Quelle: moco202618mvvm.pdf — Model-Datenstruktur im MVVM-Muster

package com.example.terun

// Spielzustand: bestimmt, welcher Zustand auf der Karte sichtbar ist.
enum class SpielStatus {
    IDLE,
    LAEUFT,
    BEENDET
}

// Repräsentiert ein erstelltes Duell
data class Duell(
    val id: String,
    val name: String,
    val spotsAnzahl: Int,
    val zeitLimitMinuten: Int
)

// Ein einzelnes Ergebnis eines Spielers am Ende eines Duells.
data class Ergebnis(
    val name: String,
    val punkte: Int
)

// Beispiel-Daten für Previews
fun beispielErgebnisse(): List<Ergebnis> {
    return listOf(
        Ergebnis(name = "Du", punkte = 1200),
        Ergebnis(name = "Lena", punkte = 980),
        Ergebnis(name = "Max", punkte = 640)
    )
}
