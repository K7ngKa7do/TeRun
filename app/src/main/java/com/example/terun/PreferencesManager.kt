package com.example.terun

import android.content.Context

/**
 * PreferencesManager — Wrapper für Android SharedPreferences.
 * Speichert einfache Schlüssel-Wert-Paare persistent auf dem Gerät
 * (überleben App-Neustarts, werden aber beim App-Deinstallieren gelöscht).
 */
class PreferencesManager(context: Context) {

    // SharedPreferences-Datei "TeRunPreferences" im privaten App-Speicher öffnen
    private val prefs = context.getSharedPreferences("TeRunPreferences", Context.MODE_PRIVATE)

    // --- Account-Key ---
    // Der Account-Key ist die E-Mail-Adresse und dient als stabiler Bezeichner für den eingeloggten User
    fun saveAccountKey(email: String) = prefs.edit().putString("account_key", email).apply()
    fun getAccountKey(): String = prefs.getString("account_key", "") ?: ""

    // --- Anzeigename ---
    // Name ist pro Account gespeichert (Schlüssel enthält E-Mail), damit mehrere Accounts möglich sind
    fun saveDisplayName(accountKey: String, name: String) =
        prefs.edit().putString("display_name_$accountKey", name).apply()
    fun getDisplayName(accountKey: String, fallback: String): String =
        prefs.getString("display_name_$accountKey", fallback) ?: fallback

    // --- Statistiken ---
    // Gesamtdistanz in Kilometern (als Float gespeichert, beim Lesen zu Double konvertiert)
    fun saveSpielerGesamtDistanz(distanz: Double) =
        prefs.edit().putFloat("spieler_gesamt_distanz", distanz.toFloat()).apply()
    fun getSpielerGesamtDistanz(): Double =
        prefs.getFloat("spieler_gesamt_distanz", 0.0f).toDouble()

    // Anzahl abgeschlossener Duelle
    fun saveAbsolvierteDuelleCount(count: Int) =
        prefs.edit().putInt("absolvierte_duelle_count", count).apply()
    fun getAbsolvierteDuelleCount(): Int =
        prefs.getInt("absolvierte_duelle_count", 0)
}
