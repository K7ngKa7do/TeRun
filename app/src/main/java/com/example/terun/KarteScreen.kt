// Datei: KarteScreen.kt
// Paket: com.example.terun
// Quelle: moco202612creatingcomposables.pdf — Column, Row, Box, Scaffold, Button, Text, LazyColumn, Card
// Quelle: moco202613composablesmodifier.pdf — Modifier-Verwendung (weight, padding, background, shape, verticalScroll)
// Quelle: moco202614recompositionstates.pdf — Statusverwaltung mit remember und mutableStateOf
// Quelle: moco202618mvvm.pdf — MVVM mit ViewModel zur Trennung von UI und Spiellogik

package com.example.terun

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// Haupt-Screen für den "Karte"-Tab. Verwaltet das Bottom-Menü und
// delegiert die Anzeige je nach Zustand an Karte, Duelle-Verwaltung, Rangliste oder Profil.
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

                        // 1. Obere Statusleiste mit Countdown-Timer
                        KarteTopBar(
                            duellLaeuft = duellLaeuft,
                            verbleibendeZeit = viewModel.verbleibendeZeit
                        )

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
                            TeRunButton(
                                text = "+ Duell starten",
                                onClick = {
                                    val standardDuell = viewModel.duelle.firstOrNull() ?: Duell("1", "Default Duell", 3, 5)
                                    viewModel.duellStarten(standardDuell)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        } else {
                            TeRunButton(
                                text = "Aufgeben",
                                onClick = { viewModel.duellBeenden() },
                                isNegative = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
            Tab.DUELLE -> {
                Box(modifier = Modifier.padding(paddingValues)) {
                    DuelleTabContent(
                        viewModel = viewModel,
                        onNavigateToKarte = { aktiverTab = Tab.KARTE }
                    )
                }
            }
            Tab.RANGLISTE -> {
                Box(modifier = Modifier.padding(paddingValues)) {
                    RanglisteTabContent(viewModel = viewModel)
                }
            }
            Tab.PROFIL -> {
                Box(modifier = Modifier.padding(paddingValues)) {
                    ProfilTabContent(viewModel = viewModel)
                }
            }
        }
    }
}

// Obere Statusleiste des Karten-Bildschirms
@Composable
fun KarteTopBar(duellLaeuft: Boolean, verbleibendeZeit: Int = 300) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(DarkBackground)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Zeigt "Duell läuft (MM:SS)" oder den Standardnamen "TeRun"
        Text(
            text = if (duellLaeuft) {
                val mins = verbleibendeZeit / 60
                val secs = verbleibendeZeit % 60
                val timeString = String.format("%02d:%02d", mins, secs)
                "Duell läuft ($timeString)"
            } else {
                "TeRun"
            },
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
    GlassmorphicCard(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .fillMaxWidth()
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

        TeRunButton(
            text = "Zurück zur Karte",
            onClick = onZurueck,
            modifier = Modifier.fillMaxWidth()
        )
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

// ============================================================================
//  DUELLE-TAB — Dynamische Erstellung & Auswahl von Duellen
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuelleTabContent(
    viewModel: KarteViewModel,
    onNavigateToKarte: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var spotsInput by remember { mutableStateOf("3") }
    var dauerInput by remember { mutableStateOf("5") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Duell-Verwaltung",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Formular zum Anlegen eines neuen Duells
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Neues Duell anlegen",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Duell-Name", color = Color.White.copy(alpha = 0.5f)) },
                placeholder = { Text("z.B. Campus-Runde") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = TeRunBlue,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = spotsInput,
                    onValueChange = { spotsInput = it },
                    label = { Text("Spots (1-5)", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TeRunBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = dauerInput,
                    onValueChange = { dauerInput = it },
                    label = { Text("Dauer (Min)", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TeRunBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TeRunButton(
                text = "Duell anlegen",
                onClick = {
                    val spots = spotsInput.toIntOrNull() ?: 3
                    val dauer = dauerInput.toIntOrNull() ?: 5
                    if (nameInput.isNotBlank()) {
                        viewModel.erstelleDuell(nameInput, spots, dauer)
                        nameInput = ""
                        spotsInput = "3"
                        dauerInput = "5"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Verfügbare Duelle",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        viewModel.duelle.forEach { duell ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = duell.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${duell.spotsAnzahl} Spots | ${duell.zeitLimitMinuten} Min Limit",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.duellStarten(duell)
                            onNavigateToKarte()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp).width(80.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(TeRunBlue, TeRunBlueLight)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Starten", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
//  RANGLISTE-TAB — Dynamisch berechnete Leaderboards
// ============================================================================
@Composable
fun RanglisteTabContent(viewModel: KarteViewModel) {
    val rangliste = viewModel.holeRangliste()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Rangliste",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            rangliste.forEachIndexed { index, ergebnis ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(MapDark, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}.",
                        color = SpotBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = ergebnis.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "${ergebnis.punkte} Pkt",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// ============================================================================
//  PROFIL-TAB — Dynamisches Profil mit Statistiken
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilTabContent(viewModel: KarteViewModel) {
    var editMode by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(viewModel.spielerName) }
    var teamInput by remember { mutableStateOf(viewModel.teamName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Spielerprofil",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeRunLogo(size = 80.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                if (editMode) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = { Text("Spielername") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = TeRunBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = teamInput,
                        onValueChange = { teamInput = it },
                        placeholder = { Text("Teamname") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = TeRunBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                } else {
                    Text(
                        text = viewModel.spielerName,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Team: ${viewModel.teamName}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Statistiken-Karte
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Statistiken",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            StatRow(label = "Gesamte Ranglistenpunkte", value = "${viewModel.spielerGesamtPunkte} Pkt")
            StatRow(label = "Zurückgelegte Distanz", value = "0.0 km")
            StatRow(label = "Absolvierte Duelle", value = if (viewModel.spielerGesamtPunkte > 0) "1" else "0")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Editier-Button
        TeRunButton(
            text = if (editMode) "Profil speichern" else "Profil bearbeiten",
            onClick = {
                if (editMode) {
                    if (nameInput.isNotBlank()) viewModel.spielerName = nameInput
                    if (teamInput.isNotBlank()) viewModel.teamName = teamInput
                } else {
                    nameInput = viewModel.spielerName
                    teamInput = viewModel.teamName
                }
                editMode = !editMode
            },
            isPositiveAlternative = editMode,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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