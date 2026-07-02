package com.example.terun

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * BenutzerEntity — Room-Entity für registrierte Benutzer.
 * Tabelle: "benutzer"
 * Primärschlüssel: E-Mail-Adresse (eindeutig, unveränderlich nach Registrierung)
 */
@Entity(tableName = "benutzer")
data class BenutzerEntity(
    @PrimaryKey val email: String,  // Eindeutige E-Mail — dient auch als Account-Key in SharedPreferences
    val name: String,               // Anzeigename (vom User selbst wählbar, muss einmalig sein)
    val passwort: String            // Passwort im Klartext (für Hochschulprojekt; in Produktion: Hash)
)
