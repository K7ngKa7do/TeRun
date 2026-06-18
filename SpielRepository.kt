package com.example.terun

// Repository-Klasse: liefert die Spiel-Daten.
// Spaeter kann hier z.B. Firebase oder eine Datenbank angebunden werden.
class SpielRepository {

    // Gibt die gleichen Beispiel-Daten zurueck wie beispielErgebnisse().
    fun ladeErgebnisse(): List<Ergebnis> {
        return listOf(
            Ergebnis(name = "Du", punkte = 1200),
            Ergebnis(name = "Lena", punkte = 980),
            Ergebnis(name = "Max", punkte = 640)
        )
    }
}
