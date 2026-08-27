// Datei: KarteScreen.kt
// Paket: com.example.terun
// Quelle: moco202612creatingcomposables.pdf — Column, Row, Box, Scaffold, Button, Text, LazyColumn, Card
// Quelle: moco202613composablesmodifier.pdf — Modifier-Verwendung (weight, padding, background, shape, verticalScroll)
// Quelle: moco202614recompositionstates.pdf — Statusverwaltung mit remember und mutableStateOf
// Quelle: moco202618mvvm.pdf — MVVM mit ViewModel zur Trennung von UI und Spiellogik
// Quelle: moco202640permissions.pdf — Berechtigungen und GPS-Ortung über Google Maps

package com.example.terun

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed

// Haupt-Screen für den "Karte"-Tab.
@Composable
fun KarteScreen(
    viewModel: KarteViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    var aktiverTab by remember { mutableStateOf(Tab.KARTE) }
    val status = viewModel.status
    val context = LocalContext.current
    val duellLaeuft = (status == SpielStatus.LAEUFT)

    // Beobachte Toast-Nachrichten aus dem ViewModel
    LaunchedEffect(viewModel.toastMessage) {
        viewModel.toastMessage?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.toastMessage = null
        }
    }

    // Dialog-Status zur Auswahl eines Duells vor dem Start
    var showSelectDuelDialog by remember { mutableStateOf(false) }

    // Kamera-State für die Hauptkarte (Google Maps Compose)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(50.9348, 6.9852), 17f)
    }

    // Standort-Updates starten, sobald der Screen geladen wird und die Berechtigung erteilt ist
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.starteStandortAbfrage()
        }
    }

    // Launcher zur Abfrage der Standortberechtigung
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.starteStandortAbfrage()
            showSelectDuelDialog = true
        }
    }

    Scaffold(
        topBar = {
            if (aktiverTab == Tab.KARTE) {
                KarteTopBar(duellLaeuft = duellLaeuft, viewModel = viewModel)
            } else {
                val title = when (aktiverTab) {
                    Tab.DUELLE -> "Duell-Verwaltung"
                    Tab.PROFIL -> "Spielerprofil"
                    else -> "TeRun"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .background(DarkBackground)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        },
        bottomBar = {
            TeRunBottomNavigation(
                aktiverTab = aktiverTab,
                onTabClick = { gewaehlt ->
                    aktiverTab = gewaehlt
                    if (gewaehlt == Tab.DUELLE) {
                        viewModel.ladeDuelle()
                        viewModel.ladeDuellEinladungen() // Eingehende Duell-Einladungen aktualisieren
                    } else if (gewaehlt == Tab.PROFIL) {
                        viewModel.ladeFreunde()
                        viewModel.ladeAusstehendeFreundesanfragen() // Eingehende Freundschaftsanfragen aktualisieren
                    }
                }
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
                    if (status == SpielStatus.BEENDET) {
                        EndScreen(
                            ergebnisse = viewModel.ergebnisse,
                            onZurueck = { viewModel.zurueckZurKarte() }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(MapDark)
                        ) {
                            TeRunMap(
                                viewModel = viewModel,
                                duellLaeuft = duellLaeuft,
                                cameraPositionState = cameraPositionState,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Banner für Eingeladene: Wartend auf Spielstart durch den Ersteller
                            // [Kompatibilität 27.08.2026] Vorhandener Block bleibt erhalten, ist aber deaktiviert, da der aktuelle ViewModel-Stand keinen waitingForStart-State enthält.
//                             if (viewModel.waitingForStart) {
//                                 GlassmorphicCard(
//                                     modifier = Modifier
//                                         .align(Alignment.TopCenter)
//                                         .padding(top = 80.dp, start = 16.dp, end = 16.dp)
//                                         .fillMaxWidth(0.9f)
//                                 ) {
//                                     Column(
//                                         modifier = Modifier.padding(14.dp),
//                                         horizontalAlignment = Alignment.CenterHorizontally
//                                     ) {
//                                         Text(
//                                             text = "⏳ Duell akzeptiert!",
//                                             color = SpotOrange,
//                                             fontWeight = FontWeight.Bold,
//                                             fontSize = 16.sp
//                                         )
//                                         Spacer(modifier = Modifier.height(4.dp))
//                                         Text(
//                                             text = "Warte auf Ersteller... Spiel startet automatisch sobald der Ersteller 'Starten' drückt.",
//                                             color = Color.White.copy(alpha = 0.85f),
//                                             fontSize = 13.sp,
//                                             textAlign = androidx.compose.ui.text.style.TextAlign.Center
//                                         )
//                                     }
//                                 }
//                             }
//
                            // Live Multiplayer-Scoreboard Overlay (wird per 📊 Button geöffnet)
                            var showScoreOverlay by remember { mutableStateOf(false) }
                            val active = viewModel.aktivesDuell
                            if (duellLaeuft && active != null && showScoreOverlay) {
                                val myScore = listOf(
                                    viewModel.spot1Captured,
                                    viewModel.spot2Captured,
                                    viewModel.spot3Captured,
                                    viewModel.spot4Captured,
                                    viewModel.spot5Captured
                                ).take(active.spotsAnzahl).count { it }

                                GlassmorphicCard(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(bottom = 80.dp, end = 14.dp)
                                        .width(220.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("📊 Punkte-Stand", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            IconButton(
                                                onClick = { showScoreOverlay = false },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Schließen", tint = Color.White.copy(alpha = 0.7f))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Du (${viewModel.spielerName}): $myScore / ${active.spotsAnzahl} Spots", color = TeRunBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        viewModel.gegnerStati.forEach { (name, statePair) ->
                                            Text("$name: ${statePair.second} / ${active.spotsAnzahl} Spots", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            // Karten-Steuerungsknöpfe auf der rechten Seite (Zentrieren, Zoom In, Zoom Out, Scoreboard)
                            val coroutineScope = rememberCoroutineScope()
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 🎯 Zentrieren
                                Button(
                                    onClick = {
                                        val pos = viewModel.spielerPosition ?: LatLng(50.9348, 6.9852)
                                        coroutineScope.launch {
                                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pos, 17f))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground.copy(alpha = 0.9f)),
                                    shape = RoundedCornerShape(50.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Text("🎯", fontSize = 18.sp)
                                }

                                // ➕ Zoom In
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground.copy(alpha = 0.9f)),
                                    shape = RoundedCornerShape(50.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Text("➕", fontSize = 18.sp)
                                }

                                // ➖ Zoom Out
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground.copy(alpha = 0.9f)),
                                    shape = RoundedCornerShape(50.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Text("➖", fontSize = 18.sp)
                                }

                                // 📊 Punktestand-Button (unterhalb der Zoom-Buttons)
                                if (duellLaeuft) {
                                    Button(
                                        onClick = { showScoreOverlay = !showScoreOverlay },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkBackground.copy(alpha = 0.9f)),
                                        shape = RoundedCornerShape(50.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Text("📊", fontSize = 18.sp)
                                    }
                                }
                            }

                            // Pop-up Dialog bei Aufgabe eines Mitspielers
                            // [Kompatibilität 27.08.2026] Vorhandener Block bleibt erhalten, ist aber deaktiviert, da die zugehörigen ViewModel-States/Funktionen im aktuellen Stand fehlen.
//                             if (viewModel.showGiveUpDialog) {
//                                 AlertDialog(
//                                     onDismissRequest = { viewModel.dismissGiveUpDialog() },
//                                     title = {
//                                         Text("Duell-Hinweis", color = Color.White, fontWeight = FontWeight.Bold)
//                                     },
//                                     text = {
//                                         Text(
//                                             "Spieler ${viewModel.opponentGaveUpName ?: "Ein Gegner"} hat das Duell aufgegeben.",
//                                             color = Color.White.copy(alpha = 0.9f),
//                                             fontSize = 14.sp
//                                         )
//                                     },
//                                     confirmButton = {
//                                         Button(
//                                             onClick = { viewModel.dismissGiveUpDialog() },
//                                             colors = ButtonDefaults.buttonColors(containerColor = TeRunBlue)
//                                         ) {
//                                             Text("Weiterspielen", color = Color.White)
//                                         }
//                                     },
//                                     dismissButton = {
//                                         Button(
//                                             onClick = {
//                                                 viewModel.duellBeenden(success = false, aufgegeben = true)
//                                             },
//                                             colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
//                                         ) {
//                                             Text("Duell beenden", color = Color.White)
//                                         }
//                                     },
//                                     containerColor = DarkBackground,
//                                     shape = RoundedCornerShape(14.dp)
//                                 )
//                             }
//
                            // Pop-up Dialog bei neuer Freundschaftsanfrage in Echtzeit
                            // [Kompatibilität 27.08.2026] Vorhandener Block bleibt erhalten, ist aber deaktiviert, da die zugehörigen ViewModel-States/Funktionen im aktuellen Stand fehlen.
//                             if (viewModel.showFriendRequestDialog) {
//                                 val senderName = viewModel.incomingFriendRequestName ?: "Ein Spieler"
//                                 AlertDialog(
//                                     onDismissRequest = { viewModel.dismissFriendRequestDialog() },
//                                     title = {
//                                         Text("📩 Neue Freundschaftsanfrage", color = Color.White, fontWeight = FontWeight.Bold)
//                                     },
//                                     text = {
//                                         Text(
//                                             "Spieler '$senderName' möchte sich mit dir befreunden!",
//                                             color = Color.White.copy(alpha = 0.9f),
//                                             fontSize = 14.sp
//                                         )
//                                     },
//                                     confirmButton = {
//                                         Button(
//                                             onClick = {
//                                                 viewModel.antworteAufFreundesanfrage(senderName, akzeptiert = true)
//                                                 viewModel.dismissFriendRequestDialog()
//                                             },
//                                             colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.85f))
//                                         ) {
//                                             Text("Zustimmen", color = Color.White, fontWeight = FontWeight.Bold)
//                                         }
//                                     },
//                                     dismissButton = {
//                                         Button(
//                                             onClick = {
//                                                 viewModel.antworteAufFreundesanfrage(senderName, akzeptiert = false)
//                                                 viewModel.dismissFriendRequestDialog()
//                                             },
//                                             colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
//                                         ) {
//                                             Text("Ablehnen", color = Color.White)
//                                         }
//                                     },
//                                     containerColor = DarkBackground,
//                                     shape = RoundedCornerShape(14.dp)
//                                 )
//                             }



                        }

                        if (!duellLaeuft) {
                            TeRunButton(
                                text = "Duell starten",
                                onClick = {
                                    // GPS Berechtigung prüfen & anfordern
                                    if (ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        showSelectDuelDialog = true
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        } else {
                            TeRunButton(
                                text = "Aufgeben",
                                onClick = { viewModel.duellBeenden(success = false, aufgegeben = true) },
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
                        onNavigateToKarte = {
                            aktiverTab = Tab.KARTE
                        }
                    )
                }
            }
            Tab.PROFIL -> {
                Box(modifier = Modifier.padding(paddingValues)) {
                    ProfilTabContent(
                        viewModel = viewModel,
                        onLogout = onLogout
                    )
                }
            }
        }
    }

    DuelSelectionDialog(
        showDialog = showSelectDuelDialog,
        viewModel = viewModel,
        onDismiss = { showSelectDuelDialog = false }
    )

}

// [Cleanup 27.08.2026]
// Die bereits separat vorhandenen Dateien EndScreen.kt, DuellErstellenScreen.kt,
// DuelleTabContent.kt und ProfilTabContent.kt werden hier nicht erneut definiert.
// Dadurch entstehen keine doppelten Composable-Funktionen / Conflicting overloads.

// Haversine Distanz-Berechnung
private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000 // Erdradius in Metern
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

// Obere Statusleiste (auch wiederverwendet in Meilenstein-Kompatibilität)
@Composable
fun KarteTopBar(duellLaeuft: Boolean, viewModel: KarteViewModel = viewModel()) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .background(DarkBackground)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (duellLaeuft) {
                val hours = viewModel.verbleibendeZeit / 3600
                val minutes = (viewModel.verbleibendeZeit % 3600) / 60
                val seconds = viewModel.verbleibendeZeit % 60
                val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                "Duell läuft ($timeString)"
            } else {
                "TeRun"
            },
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}
