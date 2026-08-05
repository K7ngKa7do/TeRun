package com.example.terun

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * =====================================================================
 * TeRunDatabase – Die zentrale SQLite-Datenbank der App (Room)
 * =====================================================================
 *
 * VORLESUNG 36 – Database Class (Room):
 * Die @Database-Klasse ist eine der drei Hauptkomponenten von Room:
 * 1. Entities       → definieren die Tabellen (z.B. BenutzerEntity)
 * 2. DAO            → definiert die Datenbankbefehle (TeRunDao)
 * 3. Database Class → hält die eigentliche Datenbankverbindung (diese Klasse hier!)
 *
 * Singleton-Muster:
 * Es soll immer nur eine einzige Datenbankinstanz existieren.
 * Das verhindert, dass mehrere Verbindungen gleichzeitig geöffnet werden,
 * was zu inkonsistenten Daten oder Abstürzen führen könnte.
 * Gelöst durch: @Volatile + synchronized { } (threadsichere Erzeugung)
 *
 * version = 12:
 * Immer wenn die Struktur einer Tabelle geändert wird (z.B. neues Feld),
 * muss diese Versionsnummer erhöht werden. Room erkennt dann die Änderung.
 *
 * fallbackToDestructiveMigration:
 * Wenn keine Migrationsstrategie definiert ist, löscht Room die alte Datenbank
 * und erstellt sie neu. Für Entwicklungsphasen praktisch, aber Achtung:
 * Alle vorhandenen Daten gehen dabei verloren!
 */
@Database(
    entities = [
        DuellEntity::class,     // Tabelle "duelle" – gespeicherte Lauf-Duelle
        ErgebnisEntity::class,  // Tabelle "ergebnisse" – Spielergebnisse nach Duellen
        BenutzerEntity::class,  // Tabelle "benutzer" – registrierte Spieler
        FreundEntity::class     // Tabelle "freunde" – Freundschaftsverbindungen
    ],
    version = 12,
    exportSchema = false // Keine Schema-Exportdatei erzeugen (nur für Produktions-Apps relevant)
)
abstract class TeRunDatabase : RoomDatabase() {

    // Über diese Methode bekommt man Zugriff auf alle Datenbankbefehle (DAO)
    abstract fun teRunDao(): TeRunDao

    companion object {

        // @Volatile stellt sicher, dass INSTANCE sofort für alle Threads sichtbar ist
        // Ohne @Volatile könnte ein Thread eine alte, gecachte Version sehen
        @Volatile
        private var INSTANCE: TeRunDatabase? = null

        /**
         * Gibt die einzige Datenbankinstanz zurück (Singleton-Muster).
         * Falls noch keine existiert, wird sie erstellt.
         * synchronized { } sorgt dafür, dass kein zweiter Thread gleichzeitig
         * eine weitere Instanz erstellt (Thread-Sicherheit).
         */
        fun getDatabase(context: Context): TeRunDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,    // App-Kontext (kein Activity-Leak)
                    TeRunDatabase::class.java,
                    "terun_database"               // Dateiname der SQLite-Datenbankdatei
                )
                    .fallbackToDestructiveMigration() // DB neu aufbauen wenn Migration fehlt
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
