package com.example.terun

import androidx.room.*

/**
 * =====================================================================
 * TeRunDao – Data Access Object (Datenbankzugriff)
 * =====================================================================
 *
 * VORLESUNG 35 – DAO (Data Access Objects):
 * Ein DAO ist ein Interface, das alle Datenbankbefehle als Kotlin-Methoden definiert.
 * Room generiert automatisch den zugehörigen Java-Code im Hintergrund (kein SQL schreiben!).
 *
 * Wichtig (aus der Vorlesung):
 * - @Query        → eigene SQL-Abfrage definieren (SELECT, UPDATE, DELETE)
 * - @Insert       → Datensatz einfügen
 * - @Delete       → Datensatz löschen
 * - onConflict    → was passiert wenn ein Datensatz mit gleicher ID schon existiert?
 *                   REPLACE = alten überschreiben (wie "Upsert")
 *
 * Hinweis zu 'suspend':
 * 'suspend' wird hier bewusst NICHT verwendet, um Kotlin 2.x KSP2-Kompatibilitätsprobleme
 * zu vermeiden. Stattdessen werden alle Aufrufe im Repository mit
 * withContext(Dispatchers.IO) auf einen Hintergrundthread verschoben (VL 30 – Dispatchers).
 */
@Dao
interface TeRunDao {

    // ==============================
    // Duelle
    // ==============================

    /**
     * Alle gespeicherten Duelle aus der Tabelle laden.
     * Wird beim App-Start aufgerufen, um die Duell-Liste anzuzeigen.
     */
    @Query("SELECT * FROM duelle")
    fun getAlleDuelle(): List<DuellEntity>

    /**
     * Duell in die Datenbank speichern.
     * REPLACE → falls ein Duell mit derselben ID schon existiert, wird es überschrieben.
     * Gibt die Zeilen-ID des eingefügten Datensatzes zurück.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDuell(duell: DuellEntity): Long

    /**
     * Einzelnes Duell anhand seiner ID löschen.
     * Gibt zurück, wie viele Zeilen gelöscht wurden (0 = nicht gefunden, 1 = erfolgreich).
     */
    @Query("DELETE FROM duelle WHERE id = :duellId")
    fun deleteDuellById(duellId: String): Int


    // ==============================
    // Benutzer
    // ==============================

    /**
     * Benutzer anhand der E-Mail-Adresse suchen.
     * LOWER() macht die Suche unabhängig von Groß-/Kleinschreibung.
     * Gibt null zurück wenn kein Benutzer gefunden wurde.
     */
    @Query("SELECT * FROM benutzer WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    fun getBenutzerByEmail(email: String): BenutzerEntity?

    /**
     * Benutzer anhand des Anzeigenamens suchen.
     * Wird verwendet um zu prüfen ob ein Freund oder Gegner in der DB registriert ist.
     */
    @Query("SELECT * FROM benutzer WHERE name = :name LIMIT 1")
    fun getBenutzerByName(name: String): BenutzerEntity?

    /**
     * Neuen Benutzer in die Datenbank speichern.
     * REPLACE → bei gleicher E-Mail wird der Eintrag aktualisiert (z.B. nach Namensänderung).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBenutzer(benutzer: BenutzerEntity): Long

    /**
     * Anzeigenamen eines Benutzers aktualisieren.
     * Wird aufgerufen wenn der Spieler seinen Namen im Profil ändert.
     */
    @Query("UPDATE benutzer SET name = :newName WHERE LOWER(email) = LOWER(:email)")
    fun updateBenutzerName(email: String, newName: String): Int

    /**
     * Alle Benutzernamen suchen, die den Suchbegriff enthalten.
     * Wird für die Freundes-Suche (Autocomplete) verwendet.
     * LIMIT 10 verhindert zu viele Ergebnisse auf einmal.
     */
    @Query("SELECT name FROM benutzer WHERE name LIKE '%' || :query || '%' LIMIT 10")
    fun sucheBenutzerNamen(query: String): List<String>

    /**
     * Benutzer vollständig aus der Datenbank löschen (z.B. bei Konto-Löschung).
     */
    @Query("DELETE FROM benutzer WHERE LOWER(email) = LOWER(:email)")
    fun deleteBenutzerByEmail(email: String): Int

    /**
     * Alle registrierten Benutzer abrufen.
     * Wird intern für die Offline-Suche nach Freunden verwendet.
     */
    @Query("SELECT * FROM benutzer")
    fun getAlleBenutzer(): List<BenutzerEntity>

    /**
     * Alle Freundschafts-Einträge eines Benutzers löschen (bei Konto-Löschung).
     * Betrifft alle Einträge, in denen die E-Mail vorkommt (als Owner oder als Friend).
     */
    @Query("DELETE FROM freunde WHERE LOWER(ownerEmail) = LOWER(:email) OR LOWER(friendEmail) = LOWER(:email)")
    fun deleteFreundeByEmail(email: String): Int


    // ==============================
    // Freunde
    // ==============================

    /**
     * Alle bestätigten Freunde eines Spielers laden.
     * Status "ACCEPTED" = Freundschaft wurde von beiden Seiten bestätigt.
     */
    @Query("SELECT * FROM freunde WHERE LOWER(ownerEmail) = LOWER(:ownerEmail) AND status = 'ACCEPTED'")
    fun getFreundeByOwner(ownerEmail: String): List<FreundEntity>

    /**
     * Ausstehende Freundschaftsanfragen laden.
     * Status "RECEIVED_PENDING" = jemand hat eine Anfrage gesendet, der Spieler hat noch nicht geantwortet.
     */
    @Query("SELECT * FROM freunde WHERE LOWER(ownerEmail) = LOWER(:ownerEmail) AND status = 'RECEIVED_PENDING'")
    fun getPendingRequestsByOwner(ownerEmail: String): List<FreundEntity>

    /**
     * Einen bestimmten Freundschaftseintrag zwischen zwei Spielern suchen.
     * Wird z.B. geprüft bevor eine neue Anfrage gesendet wird (Duplikat-Check).
     */
    @Query("SELECT * FROM freunde WHERE LOWER(ownerEmail) = LOWER(:ownerEmail) AND LOWER(friendEmail) = LOWER(:friendEmail) LIMIT 1")
    fun getFreundschaft(ownerEmail: String, friendEmail: String): FreundEntity?

    @Query("DELETE FROM freunde WHERE LOWER(ownerEmail) = LOWER(:ownerEmail) AND status != 'ACCEPTED'")
    fun deletePendingFreundeByOwner(ownerEmail: String): Int

    /**
     * Freundschaftseintrag speichern.
     * REPLACE → falls bereits ein Eintrag mit gleichen Schlüsseln existiert, wird er überschrieben.
     * Das ermöglicht Status-Änderungen (z.B. PENDING → ACCEPTED).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFreund(freund: FreundEntity): Long

    /**
     * Einzelnen Freundschaftseintrag löschen.
     * @Delete löscht das Objekt anhand seiner Primärschlüssel.
     */
    @Delete
    fun deleteFreund(freund: FreundEntity): Int
}
