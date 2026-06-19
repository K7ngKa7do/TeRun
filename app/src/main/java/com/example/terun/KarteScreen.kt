// Datei: KarteScreen.kt
// Paket: com.example.terun
// Quelle: moco202612creatingcomposables.pdf — Column, Row, Box, Scaffold, Button, Text
// Quelle: moco202613composablesmodifier.pdf — Modifier-Verwendung (weight, padding, background, shape)
// Quelle: moco202614recompositionstates.pdf — Statusverwaltung mit remember und mutableStateOf
// Quelle: moco202618mvvm.pdf — MVVM mit ViewModel zur Trennung von UI und Spiellogik

package com.example.terun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// Einfacher Platzhalter-Screen für Tabs, die noch in Arbeit sind
@Composable
fun PlatzhalterScreen(titel: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = titel, color = Color.White, fontSize = 18.sp)
    }
}

// Haupt-Screen für den "Karte"-Tab. Verwaltet das Bottom-Menü und
// delegiert die Anzeige je nach Zustand an Idle-Karte, Aktives Duell oder Endstand.
@Composable
fun KarteScreen(viewModel: KarteViewModel = viewModel()) {
    // Merkt sich den aktuell ausgewählten Tab in der Navigation Bar
    var aktiverTab by remember { mutableStateOf(Tab.KARTE) }
    val status = viewModel.status

    Scaffold(
        bottomBar = {
            TeRunBottomNavigation(
                aktiverTab = aktiverTab,
                onTabClick = { gewaehlt -> aktiverTab = gewaehlt }
            )
        }
    ) { paddingValues ->
        // Inhalt je nach ausgewähltem Tab
        when (aktiverTab) {
            Tab.KARTE -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                        .padding(paddingValues)
                ) {
                    if (status == SpielStatus.BEENDET) {
                        // Zustand BEENDET: Zeige das Leaderboard (Endstand)
                        EndScreen(
                            ergebnisse = viewModel.ergebnisse,
                            onZurueck = { viewModel.zurueckZurKarte() }
                        )
                    } else {
                        // Zustand IDLE oder LAEUFT: Karte mit TopBar anzeigen
                        val duellLaeuft = (status == SpielStatus.LAEUFT)

                        // 1. Obere Statusleiste
                        KarteTopBar(duellLaeuft = duellLaeuft)

                        // 2. Kartenbereich (nimmt den restlichen freien Platz ein)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(MapDark)
                        ) {
                            // Straßen werden nur gezeichnet, wenn das Duell aktiv läuft
                            TeRunMapBackground(showStreets = duellLaeuft)

                            if (duellLaeuft) {
                                // AKTIVES DUELL:
                                // Zwei grüne Spots (erreicht) und ein blauer Spot (offen) mit Leuchtaura
                                MapSpot(x = 0.25f, y = 0.22f, color = ActiveGreen, hasGlow = true)
                                MapSpot(x = 0.58f, y = 0.40f, color = ActiveGreen, hasGlow = true)
                                MapSpot(x = 0.72f, y = 0.62f, color = SpotBlue, hasGlow = false)

                                // Spieler (Orange) und Gegner (Rot) mit innerem Kern
                                PlayerMarker(x = 0.52f, y = 0.47f, color = SpotOrange, hasInnerDot = true)
                                PlayerMarker(x = 0.60f, y = 0.43f, color = EnemyRed, hasInnerDot = true)

                                FinishFlag(x = 0.80f, y = 0.80f)

                                // Info-Kästchen unten über die Karte gelegt
                                DuelInfoPanel()
                            } else {
                                // IDLE ZUSTAND (Vor dem Start):
                                // Drei blaue Spots, eigene Position ohne inneren Kern
                                MapSpot(x = 0.25f, y = 0.22f, color = SpotBlue)
                                MapSpot(x = 0.58f, y = 0.40f, color = SpotBlue)
                                MapSpot(x = 0.72f, y = 0.62f, color = SpotBlue)

                                PlayerMarker(x = 0.44f, y = 0.48f, color = SpotOrange)

                                FinishFlag(x = 0.80f, y = 0.80f)
                            }
                        }

                        // 3. Steuerungs-Button unter der Karte
                        if (!duellLaeuft) {
                            Button(
                                onClick = { viewModel.duellStarten() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TeRunBlue)
                            ) {
                                Text(
                                    text = "+ Neues Duell starten",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = { viewModel.duellBeenden() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AufgebenRot)
                            ) {
                                Text(
                                    text = "Aufgeben",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Tab.DUELLE -> PlatzhalterScreen("Duelle\nIn Arbeit")
            Tab.RANGLISTE -> PlatzhalterScreen("Rangliste\nIn Arbeit")
            Tab.PROFIL -> PlatzhalterScreen("Profil\nIn Arbeit")
        }
    }
}

// Obere Statusleiste des Karten-Bildschirms
@Composable
fun KarteTopBar(duellLaeuft: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(TopBarDark)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (duellLaeuft) "Duell läuft" else "TeRun",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        // Zeigt "2 / 3 Spots" an, wenn das Duell läuft, ansonsten "Kein Duell aktiv"
        Box(
            modifier = Modifier
                .background(
                    color = if (duellLaeuft) BadgeGruen else Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (duellLaeuft) "2 / 3 Spots" else "Kein Duell aktiv",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Info-Panel-Card am unteren Rand der Karte während eines Duells
@Composable
fun BoxScope.DuelInfoPanel() {
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .fillMaxWidth()
            .background(
                Color(0xFF0B1118).copy(alpha = 0.88f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = "Aktives Duell",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        DuelStatusRow(
            label = "Team Blau (Du)",
            value = "2 Spots",
            color = ActiveGreen
        )

        Spacer(modifier = Modifier.height(8.dp))

        DuelStatusRow(
            label = "Team Rot",
            value = "1 Spot",
            color = EnemyRed
        )

        Spacer(modifier = Modifier.height(8.dp))

        DuelStatusRow(
            label = "Nächster Spot",
            value = "180 m",
            color = SpotBlue
        )
    }
}

// Einzelne Statuszeile im Info-Panel
@Composable
fun DuelStatusRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Kleiner farbiger Punkt als Indikator
        androidx.compose.foundation.Canvas(modifier = Modifier.size(9.dp)) {
            drawCircle(color = color)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// End-Screen (Leaderboard) mit Spielstand
@Composable
fun EndScreen(
    ergebnisse: List<Ergebnis>,
    onZurueck: () -> Unit
) {
    // Sortiert Ergebnisse absteigend nach Punkten (Platz 1 ganz oben)
    val sortiert = ergebnisse.sortedByDescending { it.punkte }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp)
    ) {
        Text(
            text = "Duell beendet",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Endstand",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        sortiert.forEachIndexed { index, ergebnis ->
            ErgebnisZeile(
                platz = index + 1,
                name = ergebnis.name,
                punkte = ergebnis.punkte
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onZurueck,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TeRunBlue)
        ) {
            Text(
                text = "Zurück zur Karte",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Zeile im Endstand: [Platz.] [Name] [Punkte]
@Composable
fun ErgebnisZeile(
    platz: Int,
    name: String,
    punkte: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MapDark, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$platz.",
            color = SpotBlue,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = name,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "$punkte Pkt",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 15.sp
        )
    }
}

// Previews
@Preview(showBackground = true)
@Composable
fun KarteScreenPreview() {
    KarteScreen()
}

@Preview(showBackground = true)
@Composable
fun EndScreenPreview() {
    EndScreen(
        ergebnisse = beispielErgebnisse(),
        onZurueck = {}
    )
}