package com.example.terun

// Spielzustand: bestimmt, welcher Screen gerade sichtbar ist.
enum class SpielStatus {
    IDLE,
    LAEUFT,
    BEENDET
}

// Ein einzelnes Ergebnis eines Spielers am Ende eines Duells.
data class Ergebnis(
    val name: String,
    val punkte: Int
)

// Beispiel-Daten, solange es noch keine echten Punkte gibt.
// Spaeter liefert die Logik-/Firebase-Person diese Liste -> dann faellt diese Funktion weg.
fun beispielErgebnisse(): List<Ergebnis> {
    return listOf(
        Ergebnis(name = "Du", punkte = 1200),
        Ergebnis(name = "Lena", punkte = 980),
        Ergebnis(name = "Max", punkte = 640)
    )
}
