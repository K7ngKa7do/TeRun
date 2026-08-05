package com.example.terun

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * =====================================================================
 * ErgebnisEntity – Datenbankzeile für ein Spielergebnis nach dem Duell
 * =====================================================================
 *
 * VORLESUNG 34 – Entities (Room):
 * Diese Entity repräsentiert die Tabelle "ergebnisse" in der Datenbank.
 * Sie speichert den Namen des Spielers und seine erreichte Punktzahl
 * (= Anzahl der eingesammelten Spots) nach Abschluss eines Duells.
 *
 * Hinweis: Die Tabelle existiert in der Datenbank, da sie in TeRunDatabase
 * registriert ist. Ergebnisse können hier dauerhaft gespeichert werden,
 * damit die Spielhistorie auch nach einem App-Neustart noch abrufbar ist.
 *
 * autoGenerate = true:
 * Room weist jeder neuen Zeile automatisch eine fortlaufende ID zu (1, 2, 3, …).
 * Der Entwickler muss keine ID selbst vergeben.
 */
@Entity(tableName = "ergebnisse") // Name der SQLite-Tabelle in der Datenbank
data class ErgebnisEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,    // Automatisch hochgezählte ID (Room vergibt sie selbst)

    val name: String,   // Anzeigename des Spielers

    val punkte: Int     // Anzahl der erreichten Checkpoints (Spots) in diesem Duell
)
