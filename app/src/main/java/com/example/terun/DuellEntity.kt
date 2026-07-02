package com.example.terun

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * DuellEntity — Room-Entity für gespeicherte Duelle.
 * Tabelle: "duelle"
 * Spiegelt das Domain-Objekt Duell 1:1 wider; wird über SpielRepository in Duell konvertiert.
 */
@Entity(tableName = "duelle")
data class DuellEntity(
    @PrimaryKey val id: String,      // UUID des Duells
    val name: String,                // Bezeichnung
    val spotsAnzahl: Int,            // Anzahl aktiver Spots (1–5)
    val zeitLimitMinuten: Int,       // Zeitlimit in Minuten
    val spot1Lat: Double,            // Spot 1 — Breitengrad
    val spot1Lng: Double,            // Spot 1 — Längengrad
    val spot2Lat: Double,            // Spot 2 — Breitengrad
    val spot2Lng: Double,            // Spot 2 — Längengrad
    val spot3Lat: Double,            // Spot 3 — Breitengrad
    val spot3Lng: Double,            // Spot 3 — Längengrad
    val spot4Lat: Double,            // Spot 4 — Breitengrad
    val spot4Lng: Double,            // Spot 4 — Längengrad
    val spot5Lat: Double,            // Spot 5 — Breitengrad
    val spot5Lng: Double,            // Spot 5 — Längengrad
    val gegner: String = ""          // Kommaseparierte Anzeigenamen der Gegner (leer = kein Gegner)
)
