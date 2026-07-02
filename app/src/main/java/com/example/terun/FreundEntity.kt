package com.example.terun

import androidx.room.Entity

/**
 * FreundEntity — Room-Entity für Freundschaftsverbindungen.
 * Tabelle: "freunde"
 * Primärschlüssel: Kombination aus ownerEmail + friendEmail (verhindert doppelte Einträge).
 * Freundschaften werden beidseitig gespeichert: A→B UND B→A.
 */
@Entity(tableName = "freunde", primaryKeys = ["ownerEmail", "friendEmail"])
data class FreundEntity(
    val ownerEmail: String,  // E-Mail des Spielers, dem der Freund gehört
    val friendEmail: String  // E-Mail des befreundeten Spielers
)
