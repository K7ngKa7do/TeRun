// Datei: TeRunBottomNavigation.kt
// Paket: com.example.terun
// Quelle: moco202612creatingcomposables.pdf — NavigationBar, NavigationBarItem und Icon-Verwendung

package com.example.terun

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// Enum für die drei Hauptbereiche der App
enum class Tab { KARTE, DUELLE, PROFIL }

// Wiederverwendbare Bottom Navigation Bar mit Standard Material Design Icons zur robusten Darstellung
@Composable
fun TeRunBottomNavigation(
    aktiverTab: Tab = Tab.KARTE,
    onTabClick: (Tab) -> Unit = {}
) {
    NavigationBar(
        containerColor = BottomBarDark
    ) {
        NavigationBarItem(
            selected = aktiverTab == Tab.KARTE,
            onClick = { onTabClick(Tab.KARTE) },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = "Karte") },
            label = { Text("Karte") }
        )

        NavigationBarItem(
            selected = aktiverTab == Tab.DUELLE,
            onClick = { onTabClick(Tab.DUELLE) },
            icon = { Icon(Icons.Default.List, contentDescription = "Duelle") },
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
