// Datei: DuellCreateScreen.kt
// Paket: com.example.terun
// Quelle: moco202612creatingcomposables.pdf — Column, Box, Scaffold, Button
// Quelle: moco202613composablesmodifier.pdf — Modifier-Verwendung

package com.example.terun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Screen vor dem Start eines Duells.
// Verwendet die zentralen MapComponents und ThemeColors.
@Composable
fun DuellCreateScreen(
    onStartDuelClicked: () -> Unit
) {
    Scaffold(
        bottomBar = {
            TeRunBottomNavigation(
                aktiverTab = Tab.KARTE
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(paddingValues)
        ) {
            // Obere Statusleiste
            KarteTopBar(duellLaeuft = false)

            // Kartenbereich mit den Spots vor dem Spielstart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MapDark)
            ) {
                // Zeichnet das Gitter ohne Straßen
                TeRunMapBackground(showStreets = false)

                // Drei Spots (Stecknadeln) ohne Glow-Effekt
                MapSpot(x = 0.25f, y = 0.22f, color = SpotBlue)
                MapSpot(x = 0.58f, y = 0.40f, color = SpotBlue)
                MapSpot(x = 0.72f, y = 0.62f, color = SpotBlue)

                // Der eigene Spieler-Marker (Orange) ohne inneren Kern
                PlayerMarker(x = 0.44f, y = 0.48f, color = SpotOrange)

                // Zielflagge auf der Karte
                FinishFlag(x = 0.80f, y = 0.80f)
            }

            // Button zum Starten des Duells
            TeRunButton(
                text = "+ Neues Duell starten",
                onClick = onStartDuelClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DuellCreateScreenPreview() {
    DuellCreateScreen(
        onStartDuelClicked = {}
    )
}