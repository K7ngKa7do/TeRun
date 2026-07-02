package com.example.terun

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * TeRunDatabase — Singleton-Instanz der Room-Datenbank.
 * Enthält alle Tabellen der App und stellt das DAO zur Verfügung.
 *
 * version = 11: Bei strukturellen Schema-Änderungen muss die Version erhöht werden.
 * fallbackToDestructiveMigration: Bei fehlender Migration wird die DB gelöscht und neu erstellt.
 */
@Database(
    entities = [DuellEntity::class, ErgebnisEntity::class, BenutzerEntity::class, FreundEntity::class],
    version = 11,
    exportSchema = false // Schema-Datei nicht exportieren (nur für Produktions-Apps relevant)
)
abstract class TeRunDatabase : RoomDatabase() {

    // Zugriffspunkt auf alle DAO-Methoden
    abstract fun teRunDao(): TeRunDao

    companion object {
        // @Volatile stellt sicher, dass INSTANCE sofort für alle Threads sichtbar ist
        @Volatile
        private var INSTANCE: TeRunDatabase? = null

        // Singleton-Pattern: Datenbank wird nur einmal erstellt (Thread-safe via synchronized)
        fun getDatabase(context: Context): TeRunDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TeRunDatabase::class.java,
                    "terun_database" // Name der SQLite-Datenbankdatei
                )
                    .fallbackToDestructiveMigration() // DB neu erstellen wenn Migration fehlt
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
