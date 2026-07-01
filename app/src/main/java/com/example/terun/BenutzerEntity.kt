// Datei: BenutzerEntity.kt
// Paket: com.example.terun
// Quelle: moco202634entities.pdf — Definition einer Entity-Klasse in Room

package com.example.terun

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "benutzer")
data class BenutzerEntity(
    @PrimaryKey val email: String,
    val name: String,
    val passwort: String
)
