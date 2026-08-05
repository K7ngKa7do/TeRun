package com.example.terun

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * =====================================================================
 * TeRunBottomNavigation – Untere Navigationsleiste der App
 * =====================================================================
 *
 * VORLESUNG 16 – Navigation (Jetpack Compose Navigation):
 * Die untere Navigationsleiste ermöglicht dem Benutzer schnell zwischen
 * den drei Hauptbereichen der App zu wechseln.
 *
 * Drei Tabs:
 * - KARTE   → Karten-Ansicht mit OSMDroid-Karte und Duell-Steuerung
 * - DUELLE  → Liste aller Duelle, neues Duell erstellen
 * - PROFIL  → Eigenes Profil, Freundesliste, Konto-Verwaltung
 *
 * Tab-Enum:
 * Statt einfacher Strings (fehleranfällig) wird ein enum verwendet.
 * Das stellt sicher dass immer nur gültige Tab-Werte übergeben werden können.
 *
 * Composable-Prinzip (VL 11–12 – Declarative UI / Composables):
 * Diese Funktion ist ein Composable – sie beschreibt WIE die Navigation
 * aussehen soll. Compose rendert die UI automatisch neu wenn sich
 * der 'aktiverTab'-Parameter ändert (Recomposition).
 */
enum class Tab {
    KARTE,  // Tab 1: Karte mit GPS-Position und Duell-Steuerung
    DUELLE, // Tab 2: Liste und Verwaltung der Duelle
    PROFIL  // Tab 3: Profil, Freunde und Konto-Einstellungen
}

/**
 * Die untere Navigationsleiste als wiederverwendbares Composable.
 *
 * @param aktiverTab   Welcher Tab ist aktuell ausgewählt (wird hervorgehoben)
 * @param onTabClick   Callback: wird aufgerufen wenn der Benutzer einen Tab antippt
 */
@Composable
fun TeRunBottomNavigation(
    aktiverTab: Tab = Tab.KARTE,
    onTabClick: (Tab) -> Unit = {}
) {
    // NavigationBar = Material3-Komponente für die untere Navigationsleiste
    NavigationBar(containerColor = BottomBarDark) {

        // Tab 1: Karte
        NavigationBarItem(
            selected = aktiverTab == Tab.KARTE,          // Aktiv-Markierung
            onClick = { onTabClick(Tab.KARTE) },         // Callback an KarteScreen
            icon = { Icon(Icons.Default.LocationOn, contentDescription = "Karte") },
            label = { Text("Karte") }
        )

        // Tab 2: Duelle
        NavigationBarItem(
            selected = aktiverTab == Tab.DUELLE,
            onClick = { onTabClick(Tab.DUELLE) },
            // AutoMirrored = aktuelle nicht-veraltete Variante des Listen-Icons (LTR/RTL-kompatibel)
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Duelle") },
            label = { Text("Duelle") }
        )

        // Tab 3: Profil
        NavigationBarItem(
            selected = aktiverTab == Tab.PROFIL,
            onClick = { onTabClick(Tab.PROFIL) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
            label = { Text("Profil") }
        )
    }
}
