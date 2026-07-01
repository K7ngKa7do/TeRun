// Datei: DuellEntity.kt
// Paket: com.example.terun
// Quelle: moco202634entities.pdf — Definition einer Entity-Klasse in Room

package com.example.terun

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "duelle")
data class DuellEntity(
    @PrimaryKey val id: String,
    val name: String,
    val spotsAnzahl: Int,
    val zeitLimitMinuten: Int,
    val spot1Lat: Double,
    val spot1Lng: Double,
    val spot2Lat: Double,
    val spot2Lng: Double,
    val spot3Lat: Double,
    val spot3Lng: Double,
    val spot4Lat: Double,
    val spot4Lng: Double,
    val spot5Lat: Double,
    val spot5Lng: Double,
    val gegner: String = ""
)
