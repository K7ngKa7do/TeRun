package com.example.terun

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SpielRepository(context: Context) {

    private val dao = TeRunDatabase.getDatabase(context).teRunDao()
    private val prefs = PreferencesManager(context)

    // Profil (SharedPreferences)
    fun ladeSpielerName(): String = prefs.getSpielerName()
    fun speichereSpielerName(name: String) = prefs.saveSpielerName(name)
    fun ladeTeamName(): String = prefs.getTeamName()
    fun speichereTeamName(name: String) = prefs.saveTeamName(name)
    fun ladeGesamtPunkte(): Int = prefs.getSpielerGesamtPunkte()
    fun speichereGesamtPunkte(punkte: Int) = prefs.saveSpielerGesamtPunkte(punkte)
    fun ladeGesamtDistanz(): Double = prefs.getSpielerGesamtDistanz()
    fun speichereGesamtDistanz(distanz: Double) = prefs.saveSpielerGesamtDistanz(distanz)
    fun ladeAbsolvierteDuelleCount(): Int = prefs.getAbsolvierteDuelleCount()
    fun speichereAbsolvierteDuelleCount(count: Int) = prefs.saveAbsolvierteDuelleCount(count)

    // Duelle
    suspend fun holeDuelle(): List<Duell> = withContext(Dispatchers.IO) {
        val entities = dao.getAlleDuelle()
        if (entities.isEmpty()) {
            val standardDuelle = listOf(
                DuellEntity("1", "Campus Deutz Sprint", 3, 5,
                    50.9355, 6.9860, 50.9340, 6.9840, 50.9360, 6.9845, 50.9350, 6.9850, 50.9365, 6.9835),
                DuellEntity("2", "Stadtpark Runde", 3, 10,
                    50.9380, 6.9900, 50.9390, 6.9880, 50.9370, 6.9920, 50.9385, 6.9910, 50.9375, 6.9895),
                DuellEntity("3", "Rheinpromenade", 3, 8,
                    50.9365, 6.9740, 50.9350, 6.9725, 50.9380, 6.9750, 50.9370, 6.9730, 50.9360, 6.9745)
            )
            dao.insertDuelle(standardDuelle)
            return@withContext standardDuelle.map { it.toDuell() }
        }
        entities.map { it.toDuell() }
    }

    suspend fun speichereDuell(duell: Duell) = withContext(Dispatchers.IO) {
        dao.insertDuell(DuellEntity(
            id = duell.id, name = duell.name,
            spotsAnzahl = duell.spotsAnzahl, zeitLimitMinuten = duell.zeitLimitMinuten,
            spot1Lat = duell.spot1Lat, spot1Lng = duell.spot1Lng,
            spot2Lat = duell.spot2Lat, spot2Lng = duell.spot2Lng,
            spot3Lat = duell.spot3Lat, spot3Lng = duell.spot3Lng,
            spot4Lat = duell.spot4Lat, spot4Lng = duell.spot4Lng,
            spot5Lat = duell.spot5Lat, spot5Lng = duell.spot5Lng,
            gegner = duell.gegner
        ))
    }

    suspend fun loescheDuell(duell: Duell) = withContext(Dispatchers.IO) {
        dao.deleteDuellById(duell.id)
    }

    // Ergebnisse / Rangliste
    suspend fun holeRangliste(): List<Ergebnis> = withContext(Dispatchers.IO) {
        val liste = dao.getAlleErgebnisse().map { Ergebnis(it.name, it.punkte) }.toMutableList()
        val spielerName = ladeSpielerName()
        if (liste.none { it.name == spielerName }) liste.add(Ergebnis(spielerName, ladeGesamtPunkte()))
        liste.sortedByDescending { it.punkte }
    }

    suspend fun speichereErgebnis(name: String, punkte: Int) = withContext(Dispatchers.IO) {
        dao.insertErgebnis(ErgebnisEntity(name = name, punkte = punkte))
    }

    // Benutzer
    suspend fun holeBenutzer(email: String): BenutzerEntity? = withContext(Dispatchers.IO) {
        dao.getBenutzerByEmail(email)
    }

    suspend fun speichereBenutzer(benutzer: BenutzerEntity) = withContext(Dispatchers.IO) {
        dao.insertBenutzer(benutzer)
    }

    // Freunde
    suspend fun holeFreunde(): List<String> = withContext(Dispatchers.IO) {
        dao.getAlleFreunde().map { it.name }
    }

    suspend fun fuegeFreundHinzu(name: String): Boolean = withContext(Dispatchers.IO) {
        if (dao.getBenutzerByName(name) != null) {
            dao.insertFreund(FreundEntity(name))
            true
        } else false
    }

    suspend fun loescheFreund(name: String) = withContext(Dispatchers.IO) {
        dao.deleteFreund(FreundEntity(name))
    }

    suspend fun prepopulateBenutzer() = withContext(Dispatchers.IO) {
        listOf(
            BenutzerEntity("spieler1@terun.de", "Spieler1", "Passwort123."),
            BenutzerEntity("user1@terun.de", "UserOne", "Passwort123."),
            BenutzerEntity("user2@terun.de", "UserTwo", "Passwort123."),
            BenutzerEntity("user3@terun.de", "UserThree", "Passwort123."),
            BenutzerEntity("user4@terun.de", "UserFour", "Passwort123."),
            BenutzerEntity("user5@terun.de", "UserFive", "Passwort123.")
        ).forEach { if (dao.getBenutzerByEmail(it.email) == null) dao.insertBenutzer(it) }
    }

    private fun DuellEntity.toDuell() = Duell(
        id = id, name = name,
        spotsAnzahl = spotsAnzahl, zeitLimitMinuten = zeitLimitMinuten,
        spot1Lat = spot1Lat, spot1Lng = spot1Lng,
        spot2Lat = spot2Lat, spot2Lng = spot2Lng,
        spot3Lat = spot3Lat, spot3Lng = spot3Lng,
        spot4Lat = spot4Lat, spot4Lng = spot4Lng,
        spot5Lat = spot5Lat, spot5Lng = spot5Lng,
        gegner = gegner
    )
}
