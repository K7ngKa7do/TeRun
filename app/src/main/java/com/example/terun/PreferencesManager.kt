package com.example.terun

import android.content.Context

/**
 * =====================================================================
 * PreferencesManager – Einfache Schlüssel-Wert-Speicherung
 * =====================================================================
 *
 * VORLESUNG 32 – Key-Value-Speicherung (SharedPreferences):
 * SharedPreferences ist der einfachste Weg, kleine Datenmenge dauerhaft
 * auf dem Gerät zu speichern. Geeignet für:
 * - Einstellungen des Benutzers (z.B. bevorzugte Sprache)
 * - Anmeldedaten (z.B. eingeloggte E-Mail)
 * - Zähler oder einfache Werte (z.B. Gesamtdistanz)
 *
 * Nicht geeignet für:
 * - Große Datenmengen → dafür Room (VL 33–36)
 * - Komplexe Strukturen → dafür Room oder Firestore
 *
 * Funktionsweise:
 * - Daten werden als Schlüssel-Wert-Paare gespeichert (wie ein Wörterbuch)
 * - Beispiel: Schlüssel "display_name_a@b.de" → Wert "Spieler1"
 * - prefs.edit().putString(...).apply() schreibt asynchron auf den Speicher
 * - Daten überleben App-Neustarts, werden aber beim Deinstallieren gelöscht
 *
 * MODE_PRIVATE:
 * Die Datei ist nur für diese App zugänglich (andere Apps können nicht drauf zugreifen).
 */
class PreferencesManager(context: Context) {

    // Die SharedPreferences-Datei wird unter diesem Namen im privaten App-Speicher abgelegt
    private val prefs = context.getSharedPreferences("TeRunPreferences", Context.MODE_PRIVATE)


    // ==============================
    // Account-Key (eingeloggte E-Mail)
    // ==============================

    /**
     * Die E-Mail des eingeloggten Spielers speichern.
     * Dieser Wert bleibt auch nach dem Schließen der App erhalten.
     * → Beim nächsten Start weiß die App, wer eingeloggt ist.
     */
    fun saveAccountKey(email: String) = prefs.edit().putString("account_key", email).apply()

    /**
     * Die gespeicherte E-Mail des eingeloggten Spielers laden.
     * Gibt einen leeren String zurück wenn noch niemand eingeloggt ist.
     */
    fun getAccountKey(): String = prefs.getString("account_key", "") ?: ""


    // ==============================
    // Anzeigename (pro Account)
    // ==============================

    /**
     * Den Anzeigenamen für einen bestimmten Account speichern.
     * Der Schlüssel enthält die E-Mail, damit mehrere Accounts möglich sind.
     * Beispiel-Schlüssel: "display_name_kadan@test.de"
     */
    fun saveDisplayName(accountKey: String, name: String) =
        prefs.edit().putString("display_name_$accountKey", name).apply()

    /**
     * Den gespeicherten Anzeigenamen laden.
     * Falls kein Name gespeichert ist, wird 'fallback' zurückgegeben.
     */
    fun getDisplayName(accountKey: String, fallback: String): String =
        prefs.getString("display_name_$accountKey", fallback) ?: fallback


    // ==============================
    // Statistiken
    // ==============================

    /**
     * Die gesamte gelaufene Distanz aller Duelle in Kilometern speichern.
     * Float-Typ in SharedPreferences, da kein Double-Typ unterstützt wird.
     */
    fun saveSpielerGesamtDistanz(distanz: Double) =
        prefs.edit().putFloat("spieler_gesamt_distanz", distanz.toFloat()).apply()

    /**
     * Die gespeicherte Gesamtdistanz laden und zurück zu Double konvertieren.
     */
    fun getSpielerGesamtDistanz(): Double =
        prefs.getFloat("spieler_gesamt_distanz", 0.0f).toDouble()

    /**
     * Die Anzahl abgeschlossener Duelle speichern (für Profil-Anzeige).
     */
    fun saveAbsolvierteDuelleCount(count: Int) =
        prefs.edit().putInt("absolvierte_duelle_count", count).apply()

    /**
     * Die Anzahl abgeschlossener Duelle laden.
     * Standard ist 0 (noch kein Duell gespielt).
     */
    fun getAbsolvierteDuelleCount(): Int =
        prefs.getInt("absolvierte_duelle_count", 0)
}
