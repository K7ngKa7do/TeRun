package com.example.terun

/**
 * =====================================================================
 * SpielDaten – Domain-Objekte der Model-Schicht (MVVM)
 * =====================================================================
 *
 * VORLESUNG 18 – MVVM (Model-View-ViewModel):
 * Diese Datei enthält die Datenklassen der Model-Schicht:
 * - SpielStatus → steuert, welche UI angezeigt wird (Idle / Läuft / Beendet)
 * - Duell       → beschreibt ein Lauf-Duell mit Spots und Zeitlimit
 * - Ergebnis    → enthält das Resultat eines Spielers nach einem Duell
 *
 * Diese Objekte werden vom ViewModel (KarteViewModel) verwaltet
 * und von der View (KarteScreen) beobachtet und angezeigt.
 */

// Spielzustand der App — wird im ViewModel gehalten und steuert die UI-Anzeige
enum class SpielStatus {
    IDLE,     // Kein Duell aktiv; Karte wird normal angezeigt
    LAEUFT,   // Duell läuft: Timer zählt, Spots werden gecheckt, Route wird aktualisiert
    BEENDET   // Duell beendet: Ergebnis-Dialog wird angezeigt
}

/**
 * Duell — Domain-Objekt (nicht die Room-Entity!).
 * Repräsentiert ein Duell mit bis zu 5 Spot-Koordinaten und einem Zeitlimit.
 * Default-Werte 0.0 für nicht genutzte Spots (bei spotsAnzahl < 5).
 */
data class Duell(
    val id: String,                              // Eindeutige UUID
    val name: String,                            // Bezeichnung des Duells
    val spotsAnzahl: Int,                        // Anzahl aktiver Spots (1–5)
    val zeitLimitMinuten: Int,                   // Zeitlimit in Minuten
    val spot1Lat: Double = 0.0, val spot1Lng: Double = 0.0, // Koordinaten Spot 1
    val spot2Lat: Double = 0.0, val spot2Lng: Double = 0.0, // Koordinaten Spot 2
    val spot3Lat: Double = 0.0, val spot3Lng: Double = 0.0, // Koordinaten Spot 3
    val spot4Lat: Double = 0.0, val spot4Lng: Double = 0.0, // Koordinaten Spot 4
    val spot5Lat: Double = 0.0, val spot5Lng: Double = 0.0, // Koordinaten Spot 5
    val gegner: String = ""                      // Kommaseparierte Anzeigenamen der Gegner
)

/**
 * Ergebnis — Ergebnis eines einzelnen Teilnehmers nach Duellende.
 * Wird in der Ergebnisliste im ViewModel gehalten und auf dem End-Screen angezeigt.
 */
data class Ergebnis(
    val name: String,                       // Anzeigename des Spielers
    val spots: Int,                         // Anzahl der tatsächlich erreichten Spots
    val aufgegeben: Boolean = false         // true = Spieler hat aufgegeben (zählt als letzter Platz)
)
