// Datei: DuellLaueftScreen.kt
// Paket: com.example.terun
// Quelle: moco202612creatingcomposables.pdf — Column, Row, Box, Scaffold, Button
// Quelle: moco202613composablesmodifier.pdf — Modifier-Verwendung (weight, padding, background, shape)

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
import androidx.compose.ui.unit.sp

// Screen für den aktiven Zustand des Duells ("Duell läuft").
// Verwendet die zentralen MapComponents und ThemeColors.
@Composable
fun DuellLaueftScreen(
    onGiveUpClicked: () -> Unit
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
            // Obere Statusleiste ("Duell läuft" und "2 / 3 Spots")
            KarteTopBar(duellLaeuft = true)

            // Kartenbereich mit dem aktiven Spielgeschehen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MapDark)
            ) {
                // Zeichnet das Gitter mit Straßen
                TeRunMapBackground(showStreets = true)

                // Spots: Zwei grüne (erreicht) und ein blauer (offen) mit Leuchtaura
                MapSpot(x = 0.25f, y = 0.22f, color = ActiveGreen, hasGlow = true)
                MapSpot(x = 0.58f, y = 0.40f, color = ActiveGreen, hasGlow = true)
                MapSpot(x = 0.72f, y = 0.62f, color = SpotBlue, hasGlow = false)

                // Spieler (Orange) und Gegner (Rot) mit innerem Kern zur besseren Erkennung
                PlayerMarker(x = 0.52f, y = 0.47f, color = SpotOrange, hasInnerDot = true)
                PlayerMarker(x = 0.60f, y = 0.43f, color = EnemyRed, hasInnerDot = true)

                // Zielflagge
                FinishFlag(x = 0.80f, y = 0.80f)

                // Das Info-Panel unten über die Karte gelegt
                DuelInfoPanel()
            }

            // Aufgeben Button
            TeRunButton(
                text = "Aufgeben",
                onClick = onGiveUpClicked,
                isNegative = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DuellLaueftScreenPreview() {
    DuellLaueftScreen(
        onGiveUpClicked = {}
    )
}