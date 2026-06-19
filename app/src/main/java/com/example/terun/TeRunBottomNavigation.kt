// Datei: TeRunBottomNavigation.kt
// Paket: com.example.terun
// Quelle: moco202612creatingcomposables.pdf — NavigationBar, NavigationBarItem
// Quelle: moco202616navigation.pdf — Navigation unter Verwendung von Events (Callbacks)

package com.example.terun

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// Enum für die vier Hauptbereiche der App
enum class Tab { KARTE, DUELLE, RANGLISTE, PROFIL }

// Wiederverwendbare Bottom Navigation Bar, die den aktiven Tab hervorhebt
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
            icon = { Text("⌖") },
            label = { Text("Karte") }
        )

        NavigationBarItem(
            selected = aktiverTab == Tab.DUELLE,
            onClick = { onTabClick(Tab.DUELLE) },
            icon = { Text("⚔") },
            label = { Text("Duelle") }
        )

        NavigationBarItem(
            selected = aktiverTab == Tab.RANGLISTE,
            onClick = { onTabClick(Tab.RANGLISTE) },
            icon = { Text("★") },
            label = { Text("Rangliste") }
        )

        NavigationBarItem(
            selected = aktiverTab == Tab.PROFIL,
            onClick = { onTabClick(Tab.PROFIL) },
            icon = { Text("●") },
            label = { Text("Profil") }
        )
    }
}
