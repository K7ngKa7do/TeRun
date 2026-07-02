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

// Tab-Enum: definiert die drei Hauptbereiche der App
enum class Tab {
    KARTE,   // Karten-Ansicht mit OSMDroid-Karte und Duell-Steuerung
    DUELLE,  // Duell-Liste: vorhandene Duelle anzeigen, neues Duell erstellen
    PROFIL   // Profil-Verwaltung: Name ändern, Freunde, Konto löschen
}

/**
 * TeRunBottomNavigation — Wiederverwendbare Bottom Navigation Bar.
 * Zeigt die drei App-Bereiche (Karte, Duelle, Profil) mit Icons und Labels an.
 * Der aktive Tab wird hervorgehoben.
 */
@Composable
fun TeRunBottomNavigation(
    aktiverTab: Tab = Tab.KARTE,   // Welcher Tab ist gerade ausgewählt
    onTabClick: (Tab) -> Unit = {} // Callback wenn ein Tab angetippt wird
) {
    NavigationBar(containerColor = BottomBarDark) { // Eigene Hintergrundfarbe aus dem App-Theme
        NavigationBarItem(
            selected = aktiverTab == Tab.KARTE,
            onClick = { onTabClick(Tab.KARTE) },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = "Karte") },
            label = { Text("Karte") }
        )
        NavigationBarItem(
            selected = aktiverTab == Tab.DUELLE,
            onClick = { onTabClick(Tab.DUELLE) },
            // AutoMirrored.Filled.List = aktuelle nicht-deprecated Variante des Listen-Icons
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Duelle") },
            label = { Text("Duelle") }
        )
        NavigationBarItem(
            selected = aktiverTab == Tab.PROFIL,
            onClick = { onTabClick(Tab.PROFIL) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
            label = { Text("Profil") }
        )
    }
}
