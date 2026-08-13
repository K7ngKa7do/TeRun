package com.example.terun

import androidx.compose.ui.graphics.Color

/**
 * =====================================================================
 * ThemeColors – Zentrale Farbdefinitionen der App
 * =====================================================================
 *
 * VORLESUNG 13 – Composables & Modifier:
 * Farben werden als globale Kotlin-Konstanten definiert und überall
 * in Composables per Modifier.background() oder als Parameter verwendet.
 * Color(0xFF...) erstellt eine Farbe aus einem Hex-Farbcode (ARGB-Format).
 *
 * Alle Farben an einem Ort = einfach änderbar und konsistent im ganzen Projekt.
 */
val DarkBackground = Color(0xFF0D1B2A)       // Dunkles Midnight-Blau für Screens
val TeRunBlue = Color(0xFF1A6FF5)            // Blau für Primär-Buttons und Markierungen
val TeRunBlueLight = Color(0xFF4A90E2)       // Helles Digital-Blau für Farbverläufe (Gradients)
val MapDark = Color(0xFF0A1420)              // Sehr dunkles Blau für die Karte (Radar-Look)
val TopBarDark = Color(0xFF0E1A29)           // Abgestimmtes Dunkelblau für die TopBar
val BottomBarDark = Color(0xFF050A10)        // Fast schwarzes Dunkelblau für die Bottom Navigation

val SpotBlue = Color(0xFF4A8DFF)             // Leuchtendes Blau für offene Spots
val SpotOrange = Color(0xFFFFB547)           // Neon-Orange für eigene Spieler-Marker

val ActiveGreen = Color(0xFF56C596)          // Pastellgrün für erreichte Spots im aktiven Duell
val EnemyRed = Color(0xFFFF5A5A)             // Neon-Rot für gegnerische Spieler-Marker

val AufgebenRot = Color(0xFFD64545)          // Rot für den "Aufgeben"-Button
val AufgebenRotLight = Color(0xFFFF6B6B)     // Helles Rot für Verläufe
val BadgeGruen = Color(0xFF2E9E6B)           // Dunkleres Grün für den "2 / 3 Spots" Badge
