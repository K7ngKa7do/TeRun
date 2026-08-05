package com.example.terun

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * =====================================================================
 * BenutzerEntity – Datenbankzeile für einen registrierten Benutzer
 * =====================================================================
 *
 * VORLESUNG 34 – Entities (Room):
 * Eine @Entity-Klasse repräsentiert eine Tabelle in der SQLite-Datenbank.
 * Jedes Objekt dieser Klasse = eine Zeile in der Tabelle "benutzer".
 * Room kümmert sich automatisch um das Erstellen der Tabelle (kein SQL nötig).
 *
 * Primärschlüssel (PrimaryKey):
 * - Die E-Mail-Adresse ist einmalig und ändert sich nach der Registrierung nicht.
 * - Daher eignet sie sich als Primärschlüssel (eindeutiger Bezeichner pro Zeile).
 */
@Entity(tableName = "benutzer") // Name der SQLite-Tabelle in der Datenbank
data class BenutzerEntity(

    @PrimaryKey
    val email: String,    // Eindeutige E-Mail → dient als Identifikation des Benutzers

    val name: String,     // Anzeigename (vom Benutzer selbst festgelegt, muss einmalig sein)

    val passwort: String  // Passwort im Klartext (für Hochschulprojekt akzeptabel; in echter Produktion: Hash verwenden!)
)
