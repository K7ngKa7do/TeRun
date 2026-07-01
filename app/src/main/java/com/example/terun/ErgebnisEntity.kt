// Datei: ErgebnisEntity.kt
// Paket: com.example.terun
// Quelle: moco202634entities.pdf — Definition einer Entity-Klasse in Room

package com.example.terun

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ergebnisse")
data class ErgebnisEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val punkte: Int
)
