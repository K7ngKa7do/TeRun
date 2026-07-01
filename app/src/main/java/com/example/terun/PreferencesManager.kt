package com.example.terun

import android.content.Context

class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("TeRunPreferences", Context.MODE_PRIVATE)

    fun saveSpielerName(name: String) = prefs.edit().putString("spieler_name", name).apply()
    fun getSpielerName(): String = prefs.getString("spieler_name", "Spieler1") ?: "Spieler1"

    fun saveTeamName(name: String) = prefs.edit().putString("team_name", name).apply()
    fun getTeamName(): String = prefs.getString("team_name", "Team Blau") ?: "Team Blau"

    fun saveSpielerGesamtPunkte(punkte: Int) = prefs.edit().putInt("spieler_gesamt_punkte", punkte).apply()
    fun getSpielerGesamtPunkte(): Int = prefs.getInt("spieler_gesamt_punkte", 0)

    fun saveSpielerGesamtDistanz(distanz: Double) = prefs.edit().putFloat("spieler_gesamt_distanz", distanz.toFloat()).apply()
    fun getSpielerGesamtDistanz(): Double = prefs.getFloat("spieler_gesamt_distanz", 0.0f).toDouble()

    fun saveAbsolvierteDuelleCount(count: Int) = prefs.edit().putInt("absolvierte_duelle_count", count).apply()
    fun getAbsolvierteDuelleCount(): Int = prefs.getInt("absolvierte_duelle_count", 0)
}
