

package com.example.terun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// Rot fuer den "Aufgeben"-Button. (Liegt sonst in DuellLaueftScreen.kt als EnemyRed,
// hier eine eigene Konstante, damit dieser Screen unabhaengig erklaert werden kann.)
val AufgebenRot = Color(0xFFD64545)

// Gruen fuer den Badge "2 / 3 Spots" waehrend ein Duell laeuft.
val BadgeGruen = Color(0xFF2E9E6B)


enum class Tab { KARTE, DUELLE, RANGLISTE, PROFIL }

@Composable
fun PlatzhalterScreen(titel: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = titel, color = Color.White, fontSize = 18.sp)
    }
}

@Composable
fun KarteScreen(viewModel: KarteViewModel = viewModel()) {

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

        when (aktiverTab) {
            Tab.KARTE -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                        .padding(paddingValues)
                ) {

                    // Wenn das Spiel beendet ist, zeigen wir statt der Karte die Ergebnisliste.
                    if (status == SpielStatus.BEENDET) {

                        // EndScreen bekommt:

                        EndScreen(
                            ergebnisse = viewModel.ergebnisse,
                            onZurueck = { viewModel.zurueckZurKarte() }
                        )

                    } else {
                        // Zustand IDLE oder LAEUFT: Karte anzeigen.
                        val duellLaeuft = (status == SpielStatus.LAEUFT)

                        // ---- OBERE LEISTE ----
                        KarteTopBar(duellLaeuft = duellLaeuft)

                        // ---- DIE KARTE (mittlerer Bereich) ----
                        // weight(1f) heisst: nimm den ganzen freien Platz zwischen TopBar und Button ein.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(MapDark)
                        ) {
                            TeRunMapBackground()

                            // Die drei Spots. Vorerst fest eingetragen (statisch).
                            MapSpot(x = 0.25f, y = 0.22f, color = SpotBlue)
                            MapSpot(x = 0.58f, y = 0.40f, color = SpotBlue)
                            MapSpot(x = 0.72f, y = 0.62f, color = SpotBlue)

                            MyPositionMarker(x = 0.44f, y = 0.48f)
                            FinishFlag(x = 0.80f, y = 0.80f)
                        }


                        // if/else entscheidet anhand des Zustands, welcher Button gezeigt wird.
                        if (!duellLaeuft) {
                            // Zustand IDLE: blauer Button -> startet das Duell
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

                            // fuehrt zum End-Screen (status = BEENDET).
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
            Tab.DUELLE -> {
                Box(modifier = Modifier.fillMaxSize().background(DarkBackground).padding(paddingValues)) {
                    PlatzhalterScreen(titel = "Duelle\nIn Arbeit")
                }
            }
            Tab.RANGLISTE -> {
                Box(modifier = Modifier.fillMaxSize().background(DarkBackground).padding(paddingValues)) {
                    PlatzhalterScreen(titel = "Rangliste \nIn Arbeit")
                }
            }
            Tab.PROFIL -> {
                Box(modifier = Modifier.fillMaxSize().background(DarkBackground).padding(paddingValues)) {
                    PlatzhalterScreen(titel = "Profil \nIn Arbeit")
                }
            }
        }
    }
}


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

        // Titel links: wechselt zwischen "TeRun" und "Duell läuft".
        Text(
            text = if (duellLaeuft) "Duell läuft" else "TeRun",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        // Spacer schiebt den Badge nach ganz rechts.
        Spacer(modifier = Modifier.weight(1f))

        // Badge rechts: grau im Idle, gruen waehrend des Duells.
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

// ============================================================================
//  END-SCREEN — wird nach Spielende ("Aufgeben") angezeigt.
//  Zeigt die Endplatzierung: Platz, Name, Punkte.
// ============================================================================

// Der End-Screen bekommt zwei Dinge von aussen:
//  - ergebnisse: die Liste der Spieler (vorerst Beispiel-Daten)
//  - onZurueck:  was passieren soll, wenn man "Zurück zur Karte" tippt
@Composable
fun EndScreen(
    ergebnisse: List<Ergebnis>,
    onZurueck: () -> Unit
) {

    // Liste nach Punkten sortieren: hoechste Punktzahl zuerst (Platz 1).
    // sortedByDescending sortiert absteigend.
    val sortiert = ergebnisse.sortedByDescending { it.punkte }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp)
    ) {

        // Ueberschrift
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

        // Die Liste der Spieler.
        // forEachIndexed laeuft durch jeden Eintrag UND gibt den Index mit (0,1,2,...).
        // platz = index + 1, weil der Index bei 0 anfaengt, der Platz aber bei 1.
        sortiert.forEachIndexed { index, ergebnis ->
            ErgebnisZeile(
                platz = index + 1,
                name = ergebnis.name,
                punkte = ergebnis.punkte
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // weight(1f) auf einem leeren Spacer schiebt den Button nach ganz unten.
        Spacer(modifier = Modifier.weight(1f))

        // Zurueck zur Karte
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

// Eine einzelne Zeile der Ergebnisliste: [Platz]  Name ........ Punkte
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
        // Platznummer links
        Text(
            text = "$platz.",
            color = SpotBlue,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Name in der Mitte
        Text(
            text = name,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        // Spacer schiebt die Punkte nach rechts
        Spacer(modifier = Modifier.weight(1f))

        // Punkte rechts
        Text(
            text = "$punkte Pkt",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 15.sp
        )
    }
}



// Preview zeigt den Startzustand (Idle) direkt in Android Studio.
// Quelle: moco202612creatingcomposables.pdf — @Preview
@Preview(showBackground = true)
@Composable
fun KarteScreenPreview() {
    KarteScreen()
}

// Zweite Preview: zeigt nur den End-Screen mit Beispiel-Daten,
// damit man ihn in Android Studio sieht, ohne erst durchs Spiel zu klicken.
@Preview(showBackground = true)
@Composable
fun EndScreenPreview() {
    EndScreen(
        ergebnisse = beispielErgebnisse(),
        onZurueck = {}
    )
}