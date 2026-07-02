package com.example.terun

import androidx.room.*

/**
 * TeRunDao — Data Access Object für die Room-Datenbank.
 * Definiert alle SQL-Abfragen als Interface-Methoden.
 *
 * Hinweis: 'suspend' wird bewusst weggelassen, um KSP2-Continuation-Signaturkonflikte
 * unter Kotlin 2.x zu vermeiden. Alle Aufrufe erfolgen im Repository via withContext(Dispatchers.IO).
 */
@Dao
interface TeRunDao {

    // --- Duelle ---

    // Alle gespeicherten Duelle aus der Tabelle laden
    @Query("SELECT * FROM duelle")
    fun getAlleDuelle(): List<DuellEntity>

    // Duell einfügen; bei gleicher ID wird der bestehende Eintrag überschrieben
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDuell(duell: DuellEntity): Long

    // Einzelnes Duell anhand seiner ID löschen
    @Query("DELETE FROM duelle WHERE id = :duellId")
    fun deleteDuellById(duellId: String): Int

    // --- Benutzer ---

    // Benutzer anhand der E-Mail-Adresse suchen (eindeutiger Primärschlüssel)
    @Query("SELECT * FROM benutzer WHERE email = :email LIMIT 1")
    fun getBenutzerByEmail(email: String): BenutzerEntity?

    // Benutzer anhand des Anzeigenamens suchen (für Freundes-/Gegnersuche)
    @Query("SELECT * FROM benutzer WHERE name = :name LIMIT 1")
    fun getBenutzerByName(name: String): BenutzerEntity?

    // Neuen Benutzer anlegen; bei gleicher Email wird überschrieben
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBenutzer(benutzer: BenutzerEntity): Long

    // Anzeigenamen eines bestehenden Benutzers aktualisieren
    @Query("UPDATE benutzer SET name = :newName WHERE email = :email")
    fun updateBenutzerName(email: String, newName: String): Int

    // Alle Benutzernamen suchen die den Suchbegriff enthalten (max. 10 Ergebnisse)
    @Query("SELECT name FROM benutzer WHERE name LIKE '%' || :query || '%' LIMIT 10")
    fun sucheBenutzerNamen(query: String): List<String>

    // Benutzer-Zeile vollständig löschen (bei Konto-Löschung)
    @Query("DELETE FROM benutzer WHERE email = :email")
    fun deleteBenutzerByEmail(email: String): Int

    // Alle Freundschafts-Einträge löschen in denen die E-Mail vorkommt (bei Konto-Löschung)
    @Query("DELETE FROM freunde WHERE ownerEmail = :email OR friendEmail = :email")
    fun deleteFreundeByEmail(email: String): Int

    // --- Freunde ---

    // Alle Freundschafts-Einträge eines bestimmten Nutzers laden
    @Query("SELECT * FROM freunde WHERE ownerEmail = :ownerEmail")
    fun getFreundeByOwner(ownerEmail: String): List<FreundEntity>

    // Freundschafts-Eintrag speichern; bei Duplikat überschreiben (verhindert doppelte Einträge)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFreund(freund: FreundEntity): Long

    // Einzelne Freundschaftsverbindung löschen
    @Delete
    fun deleteFreund(freund: FreundEntity): Int
}
