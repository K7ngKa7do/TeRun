// Datei: SpielRepository.kt
// Paket: com.example.terun
// Quelle: moco202618mvvm.pdf — Repository-Entwurfsmuster für den Datenzugriff im Model

package com.example.terun

// Das Repository verwaltet die Daten im Speicher (In-Memory).
// Später wird dies an Firebase Firestore angebunden.
class SpielRepository {

    // Liste der standardmäßig verfügbaren Duelle
    val duelleListe = mutableListOf(
        Duell("1", "Stadtpark Runde", 3, 5),
        Duell("2", "Kölner Dom Sprint", 5, 10),
        Duell("3", "Rheinpromenade", 4, 8)
    )

    // Globale Highscore-Rangliste der Teams
    val rangliste = mutableListOf(
        Ergebnis("Du", 0),
        Ergebnis("Lena", 980),
        Ergebnis("Max", 640),
        Ergebnis("Sarah", 450)
    )

    // Simuliert Spielergebnisse für das aktuelle Match
    fun ladeErgebnisse(spielerPunkte: Int): List<Ergebnis> {
        return listOf(
            Ergebnis("Du", spielerPunkte),
            Ergebnis("Lena", (300..900).random()),
            Ergebnis("Max", (200..700).random())
        )
    }

    // Aktualisiert die Punkte des Spielers in der globalen Liste
    fun updateRangliste(neuePunkte: Int) {
        val index = rangliste.indexOfFirst { it.name == "Du" }
        if (index != -1) {
            val alterWert = rangliste[index].punkte
            rangliste[index] = Ergebnis("Du", alterWert + neuePunkte)
        }
    }
}
