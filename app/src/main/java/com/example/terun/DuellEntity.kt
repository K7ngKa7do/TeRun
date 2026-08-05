package com.example.terun

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * =====================================================================
 * DuellEntity – Datenbankzeile für ein gespeichertes Lauf-Duell
 * =====================================================================
 *
 * VORLESUNG 34 – Entities (Room):
 * Jede Instanz dieser Klasse stellt ein Duell dar, das in der lokalen
 * SQLite-Datenbank gespeichert ist. Room wandelt dieses Kotlin-Objekt
 * automatisch in eine Datenbankzeile um (Object-Relational-Mapping).
 *
 * Aufbau eines Duells:
 * - Ein Duell hat 1–5 Checkpoints ("Spots"), die der Spieler auf der Karte erreichen muss.
 * - Jeder Spot wird als Koordinatenpaar (Breitengrad + Längengrad) gespeichert.
 * - Ein Zeitlimit begrenzt die Spielzeit in Minuten.
 * - Ein Gegner kann eingeladen werden (kommaseparierte Spielernamen).
 */
@Entity(tableName = "duelle") // Name der SQLite-Tabelle in der Datenbank
data class DuellEntity(

    @PrimaryKey
    val id: String,              // Einmalige UUID des Duells (wird beim Erstellen zufällig generiert)

    val name: String,            // Bezeichnung des Duells (vom Ersteller frei wählbar)

    val spotsAnzahl: Int,        // Anzahl der Checkpoints (erlaubt: 1 bis 5)

    val zeitLimitMinuten: Int,   // Wie viele Minuten hat der Spieler, um alle Spots zu erreichen?

    // Koordinaten der 5 möglichen Checkpoints (Spot 1 bis 5):
    // lat = Breitengrad (latitude), lng = Längengrad (longitude)
    val spot1Lat: Double, val spot1Lng: Double, // Spot 1
    val spot2Lat: Double, val spot2Lng: Double, // Spot 2
    val spot3Lat: Double, val spot3Lng: Double, // Spot 3
    val spot4Lat: Double, val spot4Lng: Double, // Spot 4
    val spot5Lat: Double, val spot5Lng: Double, // Spot 5

    val gegner: String = ""      // Eingeladene Gegner (z.B. "Spieler2,Spieler3"), leer = Solo-Duell
)
