// Datei: TeRunDatabase.kt
// Paket: com.example.terun
// Quelle: moco202636databaseclass.pdf — Implementierung der RoomDatabase-Klasse als Singleton

package com.example.terun

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DuellEntity::class, ErgebnisEntity::class, BenutzerEntity::class, FreundEntity::class], version = 8, exportSchema = false)
abstract class TeRunDatabase : RoomDatabase() {

    abstract fun teRunDao(): TeRunDao

    companion object {
        @Volatile
        private var INSTANCE: TeRunDatabase? = null

        fun getDatabase(context: Context): TeRunDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TeRunDatabase::class.java,
                    "terun_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
