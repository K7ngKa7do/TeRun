package com.example.terun

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ErgebnisEntity — Room-Entity für die "ergebnisse"-Tabelle.
 * Die Tabelle existiert in der Datenbank, wird aber aktuell nicht aktiv befüllt —
 * Ergebnisse werden nur für den End-Screen im RAM (ViewModel) gehalten.
 */
@Entity(tableName = "ergebnisse")
data class ErgebnisEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Automatisch generierte ID
    val name: String,                                  // Anzeigename des Spielers
    val punkte: Int                                    // Erzielte Punkte
)
