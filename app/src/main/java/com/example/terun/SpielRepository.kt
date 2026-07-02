package com.example.terun

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SpielRepository — Datenschicht der App.
 * Kapselt den gesamten Datenzugriff: Room-Datenbank (DAO) und SharedPreferences (PreferencesManager).
 * Das ViewModel kennt nur das Repository, nie direkt DAO oder Prefs.
 */
class SpielRepository(context: Context) {

    private val dao = TeRunDatabase.getDatabase(context).teRunDao() // Datenbankzugriffsobjekt
    private val prefs = PreferencesManager(context)                 // SharedPreferences-Wrapper

    // ==========================================================================
    // Account
    // ==========================================================================

    // Account-Key = E-Mail-Adresse des eingeloggten Nutzers; wird beim Login gesetzt
    fun setAccountKey(email: String) = prefs.saveAccountKey(email)
    fun getAccountKey(): String = prefs.getAccountKey()

    // ==========================================================================
    // Profil
    // ==========================================================================

    // Anzeigenamen des aktuell eingeloggten Spielers laden
    // Fallback: E-Mail-Präfix (z.B. "max" aus "max@mail.de") wenn kein Name gesetzt
    fun ladeSpielerName(): String {
        val key = prefs.getAccountKey()
        return if (key.isBlank()) "Spieler" else prefs.getDisplayName(key, key.substringBefore("@"))
    }

    // Anzeigenamen in SharedPreferences und Room-DB aktualisieren
    fun speichereSpielerName(name: String) {
        val key = prefs.getAccountKey()
        if (key.isNotBlank()) {
            prefs.saveDisplayName(key, name)
            // DB-Update im IO-Thread (Fire-and-Forget, kein Rückgabewert benötigt)
            CoroutineScope(Dispatchers.IO).launch {
                dao.updateBenutzerName(key, name)
            }
        }
    }

    fun ladeGesamtDistanz(): Double = prefs.getSpielerGesamtDistanz()
    fun speichereGesamtDistanz(distanz: Double) = prefs.saveSpielerGesamtDistanz(distanz)

    fun ladeAbsolvierteDuelleCount(): Int = prefs.getAbsolvierteDuelleCount()
    fun speichereAbsolvierteDuelleCount(count: Int) = prefs.saveAbsolvierteDuelleCount(count)

    // ==========================================================================
    // Duelle
    // ==========================================================================

    // Alle gespeicherten Duelle aus der Room-DB laden und als Domain-Objekte zurückgeben
    suspend fun holeDuelle(): List<Duell> = withContext(Dispatchers.IO) {
        dao.getAlleDuelle().map { it.toDuell() } // Entity → Domain-Objekt
    }

    // Duell als neue Zeile in die Room-DB schreiben
    suspend fun speichereDuell(duell: Duell) = withContext(Dispatchers.IO) {
        dao.insertDuell(
            DuellEntity(
                id = duell.id,
                name = duell.name,
                spotsAnzahl = duell.spotsAnzahl,
                zeitLimitMinuten = duell.zeitLimitMinuten,
                spot1Lat = duell.spot1Lat, spot1Lng = duell.spot1Lng,
                spot2Lat = duell.spot2Lat, spot2Lng = duell.spot2Lng,
                spot3Lat = duell.spot3Lat, spot3Lng = duell.spot3Lng,
                spot4Lat = duell.spot4Lat, spot4Lng = duell.spot4Lng,
                spot5Lat = duell.spot5Lat, spot5Lng = duell.spot5Lng,
                gegner = duell.gegner
            )
        )
    }

    // Duell anhand seiner ID aus der Room-DB löschen
    suspend fun loescheDuell(duell: Duell) = withContext(Dispatchers.IO) {
        dao.deleteDuellById(duell.id)
    }

    // ==========================================================================
    // Benutzer
    // ==========================================================================

    // Benutzer anhand der E-Mail laden (z.B. für Login-Validierung)
    suspend fun holeBenutzer(email: String): BenutzerEntity? = withContext(Dispatchers.IO) {
        dao.getBenutzerByEmail(email)
    }

    // Neuen Benutzer in der Room-DB anlegen (bei Registrierung)
    suspend fun speichereBenutzer(benutzer: BenutzerEntity) = withContext(Dispatchers.IO) {
        dao.insertBenutzer(benutzer)
    }

    // Prüft ob ein Benutzername bereits vergeben ist (für Eindeutigkeits-Validierung bei Registrierung)
    suspend fun existiertBenutzerMitName(name: String): Boolean = withContext(Dispatchers.IO) {
        dao.getBenutzerByName(name) != null
    }

    // Sucht Benutzernamen die den eingegebenen Begriff enthalten (für Autocomplete bei Gegner-/Freundessuche)
    suspend fun sucheBenutzerNamen(query: String): List<String> = withContext(Dispatchers.IO) {
        dao.sucheBenutzerNamen(query)
    }

    // Konto vollständig löschen: Benutzer-Zeile + alle Freundschafts-Einträge entfernen
    suspend fun loescheKonto(email: String) = withContext(Dispatchers.IO) {
        dao.deleteBenutzerByEmail(email)
        dao.deleteFreundeByEmail(email)
    }

    // ==========================================================================
    // Freunde
    // ==========================================================================

    // Alle Freunde des Spielers laden und deren Anzeigenamen zurückgeben
    // Freundschaften sind in der DB beidseitig gespeichert (ownerEmail ↔ friendEmail)
    suspend fun holeFreunde(ownerEmail: String): List<String> = withContext(Dispatchers.IO) {
        dao.getFreundeByOwner(ownerEmail).mapNotNull { friend ->
            // friendEmail → Anzeigename über DB-Lookup auflösen
            dao.getBenutzerByEmail(friend.friendEmail)?.name
        }
    }

    // Freund anhand des Anzeigenamens hinzufügen; gibt false zurück wenn User nicht gefunden
    // Freundschaft wird beidseitig eingetragen: A sieht B und B sieht A
    suspend fun fuegeFreundHinzu(ownerEmail: String, friendName: String): Boolean =
        withContext(Dispatchers.IO) {
            val friendUser = dao.getBenutzerByName(friendName) ?: return@withContext false
            val friendEmail = friendUser.email
            if (ownerEmail == friendEmail) return@withContext false // Sich selbst hinzufügen verhindern
            dao.insertFreund(FreundEntity(ownerEmail = ownerEmail, friendEmail = friendEmail))
            dao.insertFreund(FreundEntity(ownerEmail = friendEmail, friendEmail = ownerEmail))
            true
        }

    // Freundschaft beidseitig löschen
    suspend fun loescheFreund(ownerEmail: String, friendName: String) = withContext(Dispatchers.IO) {
        val friendUser = dao.getBenutzerByName(friendName) ?: return@withContext
        val friendEmail = friendUser.email
        dao.deleteFreund(FreundEntity(ownerEmail = ownerEmail, friendEmail = friendEmail))
        dao.deleteFreund(FreundEntity(ownerEmail = friendEmail, friendEmail = ownerEmail))
    }

    // ==========================================================================
    // Mapper (privat)
    // ==========================================================================

    // Konvertiert eine Room-Entity (DuellEntity) in ein Domain-Objekt (Duell)
    // Trennung: DB-Schicht kennt nur Entities, ViewModel/UI nur Domain-Objekte
    private fun DuellEntity.toDuell() = Duell(
        id = id,
        name = name,
        spotsAnzahl = spotsAnzahl,
        zeitLimitMinuten = zeitLimitMinuten,
        spot1Lat = spot1Lat, spot1Lng = spot1Lng,
        spot2Lat = spot2Lat, spot2Lng = spot2Lng,
        spot3Lat = spot3Lat, spot3Lng = spot3Lng,
        spot4Lat = spot4Lat, spot4Lng = spot4Lng,
        spot5Lat = spot5Lat, spot5Lng = spot5Lng,
        gegner = gegner
    )
}
