// Datei: TeRunDao.kt
// Paket: com.example.terun
// Quelle: moco202636databaseclass.pdf — Datenzugriffsobjekt (DAO) Definition
// Info: Entfernung des 'suspend'-Keywords zur Vermeidung von KSP2-Continuation-Signaturkonflikten unter Kotlin 2.x

package com.example.terun

import androidx.room.*

@Dao
interface TeRunDao {

    // --- Duelle ---
    @Query("SELECT * FROM duelle")
    fun getAlleDuelle(): List<DuellEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDuell(duell: DuellEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDuelle(duelle: List<DuellEntity>): List<Long>

    @Query("DELETE FROM duelle WHERE id = :duellId")
    fun deleteDuellById(duellId: String): Int

    // --- Ergebnisse ---
    @Query("SELECT * FROM ergebnisse")
    fun getAlleErgebnisse(): List<ErgebnisEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertErgebnis(ergebnis: ErgebnisEntity): Long

    // --- Benutzer ---
    @Query("SELECT * FROM benutzer WHERE email = :email LIMIT 1")
    fun getBenutzerByEmail(email: String): BenutzerEntity?

    @Query("SELECT * FROM benutzer WHERE name = :name LIMIT 1")
    fun getBenutzerByName(name: String): BenutzerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBenutzer(benutzer: BenutzerEntity): Long

    // --- Freunde ---
    @Query("SELECT * FROM freunde")
    fun getAlleFreunde(): List<FreundEntity>

    @Query("SELECT * FROM freunde WHERE name = :name LIMIT 1")
    fun getFreundByName(name: String): FreundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFreund(freund: FreundEntity): Long

    @Delete
    fun deleteFreund(freund: FreundEntity): Int
}
