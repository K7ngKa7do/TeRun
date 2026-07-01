// Datei: KarteScreen.kt
// Paket: com.example.terun
// Quelle: moco202612creatingcomposables.pdf — Column, Row, Box, Scaffold, Button, Text, LazyColumn, Card
// Quelle: moco202613composablesmodifier.pdf — Modifier-Verwendung (weight, padding, background, shape, verticalScroll)
// Quelle: moco202614recompositionstates.pdf — Statusverwaltung mit remember und mutableStateOf
// Quelle: moco202618mvvm.pdf — MVVM mit ViewModel zur Trennung von UI und Spiellogik
// Quelle: moco202640permissions.pdf — Berechtigungen und GPS-Ortung über AndroidView mit OSMDroid

package com.example.terun

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    // Dialog-Status zur Auswahl eines Duells vor dem Start
    var showSelectDuelDialog by remember { mutableStateOf(false) }

    // Steuerung der Kartenzentrierung: true = Benutzer hat geschoben, false = zentriere auf Spieler
    var hasCenteredMap by remember { mutableStateOf(false) }
    var mainMapInstance by remember { mutableStateOf<MapView?>(null) }

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
                            // Integration der echten OSMDroid OpenStreetMap-Karte
                            AndroidView(
                                factory = { ctx ->
                                    Configuration.getInstance().load(
                                        ctx,
                                        ctx.getSharedPreferences("osm_prefs", Context.MODE_PRIVATE)
                                    )
                                    MapView(ctx).apply {
                                        setTileSource(TileSourceFactory.MAPNIK)
                                        setMultiTouchControls(true)
                                        setBuiltInZoomControls(false)
                                        controller.setZoom(17.5)
                                        minZoomLevel = 4.0
                                        maxZoomLevel = 21.0

                                        // Compass needle overlay
                                        val compass = CompassOverlay(ctx, InternalCompassOrientationProvider(ctx), this).apply {
                                            enableCompass()
                                        }
                                        overlays.add(compass)

                                        mainMapInstance = this
                                    }
                                },
                                update = { mapView ->
                                    mapView.overlays.clear()

                                    // Retain compass overlay during updates
                                    val compass = CompassOverlay(context, InternalCompassOrientationProvider(context), mapView).apply {
                                        enableCompass()
                                    }
                                    mapView.overlays.add(compass)

                                    val currentPos = viewModel.spielerPosition ?: GeoPoint(50.9348, 6.9852)
                                    
                                    // Nur zentrieren, wenn der Benutzer die Ansicht nicht manuell verschoben hat
                                    if (!hasCenteredMap) {
                                        mapView.controller.setCenter(currentPos)
                                        hasCenteredMap = true
                                    }

                                    // 1. Eigener Spieler-Marker auf der Karte
                                    val playerMarker = Marker(mapView).apply {
                                        position = currentPos
                                        title = viewModel.spielerName
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    }
                                    mapView.overlays.add(playerMarker)

                                    // 2. Ziel-Beacon (Startpunkt des Duells)
                                    val finishPos = viewModel.startPositionGeo ?: GeoPoint(50.9348, 6.9852)
                                    val finishMarker = Marker(mapView).apply {
                                        position = finishPos
                                        title = "Ziel-Beacon (Startort)"
                                        icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_myplaces)
                                    }
                                    mapView.overlays.add(finishMarker)

                                    val active = viewModel.aktivesDuell
                                    if (duellLaeuft && active != null) {
                                        val count = active.spotsAnzahl
                                        // 3. Spots mit sich farblich ändernden Icons zeichnen
                                        if (count >= 1) {
                                            val spot1Marker = Marker(mapView).apply {
                                                position = GeoPoint(active.spot1Lat, active.spot1Lng)
                                                title = "Spot 1"
                                                icon = ContextCompat.getDrawable(
                                                    context,
                                                    if (viewModel.spot1Captured) R.drawable.ic_flag_captured
                                                    else R.drawable.ic_flag_uncaptured
                                                )
                                            }
                                            mapView.overlays.add(spot1Marker)
                                        }

                                        if (count >= 2) {
                                            val spot2Marker = Marker(mapView).apply {
                                                position = GeoPoint(active.spot2Lat, active.spot2Lng)
                                                title = "Spot 2"
                                                icon = ContextCompat.getDrawable(
                                                    context,
                                                    if (viewModel.spot2Captured) R.drawable.ic_flag_captured
                                                    else R.drawable.ic_flag_uncaptured
                                                )
                                            }
                                            mapView.overlays.add(spot2Marker)
                                        }

                                        if (count >= 3) {
                                            val spot3Marker = Marker(mapView).apply {
                                                position = GeoPoint(active.spot3Lat, active.spot3Lng)
                                                title = "Spot 3"
                                                icon = ContextCompat.getDrawable(
                                                    context,
                                                    if (viewModel.spot3Captured) R.drawable.ic_flag_captured
                                                    else R.drawable.ic_flag_uncaptured
                                                )
                                            }
                                            mapView.overlays.add(spot3Marker)
                                        }

                                        if (count >= 4) {
                                            val spot4Marker = Marker(mapView).apply {
                                                position = GeoPoint(active.spot4Lat, active.spot4Lng)
                                                title = "Spot 4"
                                                icon = ContextCompat.getDrawable(
                                                    context,
                                                    if (viewModel.spot4Captured) R.drawable.ic_flag_captured
                                                    else R.drawable.ic_flag_uncaptured
                                                )
                                            }
                                            mapView.overlays.add(spot4Marker)
                                        }

                                        if (count >= 5) {
                                            val spot5Marker = Marker(mapView).apply {
                                                position = GeoPoint(active.spot5Lat, active.spot5Lng)
                                                title = "Spot 5"
                                                icon = ContextCompat.getDrawable(
                                                    context,
                                                    if (viewModel.spot5Captured) R.drawable.ic_flag_captured
                                                    else R.drawable.ic_flag_uncaptured
                                                )
                                            }
                                            mapView.overlays.add(spot5Marker)
                                        }

                                        // 4. Pfad-Verbindungslinie zum nächsten offenen Spot zeichnen
                                        val nextSpotGeo = when {
                                            count >= 1 && !viewModel.spot1Captured -> GeoPoint(active.spot1Lat, active.spot1Lng)
                                            count >= 2 && !viewModel.spot2Captured -> GeoPoint(active.spot2Lat, active.spot2Lng)
                                            count >= 3 && !viewModel.spot3Captured -> GeoPoint(active.spot3Lat, active.spot3Lng)
                                            count >= 4 && !viewModel.spot4Captured -> GeoPoint(active.spot4Lat, active.spot4Lng)
                                            count >= 5 && !viewModel.spot5Captured -> GeoPoint(active.spot5Lat, active.spot5Lng)
                                            else -> finishPos
                                        }

                                        val routePolyline = Polyline().apply {
                                            addPoint(currentPos)
                                            addPoint(nextSpotGeo)
                                            outlinePaint.color = android.graphics.Color.BLUE
                                            outlinePaint.strokeWidth = 6f
                                        }
                                        mapView.overlays.add(routePolyline)
                                    }

                                    mapView.invalidate()
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Karten-Steuerungsknöpfe auf der rechten Seite (Locate, Zoom In, Zoom Out)
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 🎯 Zentrieren
                                Button(
                                    onClick = {
                                        val currentPos = viewModel.spielerPosition ?: GeoPoint(50.9348, 6.9852)
                                        mainMapInstance?.controller?.animateTo(currentPos)
                                        hasCenteredMap = true
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
                                    onClick = { mainMapInstance?.controller?.zoomIn() },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground.copy(alpha = 0.9f)),
                                    shape = RoundedCornerShape(50.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Text("➕", fontSize = 18.sp)
                                }

                                // ➖ Zoom Out
                                Button(
                                    onClick = { mainMapInstance?.controller?.zoomOut() },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground.copy(alpha = 0.9f)),
                                    shape = RoundedCornerShape(50.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Text("➖", fontSize = 18.sp)
                                }
                            }

                            if (duellLaeuft) {
                                DuelInfoPanel(viewModel)
                            }
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
                                onClick = { viewModel.duellBeenden(success = false) },
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
                            hasCenteredMap = false // Automatisch zentrieren beim Wechseln zur Karte
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

    // Dialog zur Auswahl des gewünschten Duells vor Spielstart (Solid dark container für perfekte Lesbarkeit)
    if (showSelectDuelDialog) {
        AlertDialog(
            onDismissRequest = { showSelectDuelDialog = false },
            title = {
                Text(
                    text = "Duell auswählen",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Wähle aus, welches Duell gestartet werden soll:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    viewModel.duelle.forEach { duell ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    hasCenteredMap = false // Zentrierung zurücksetzen für das neue Duell
                                    viewModel.duellStarten(duell)
                                    showSelectDuelDialog = false
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = duell.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${duell.spotsAnzahl} Spots | ${duell.zeitLimitMinuten} Min",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSelectDuelDialog = false }) {
                    Text("Abbrechen", color = TeRunBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkBackground,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// Info-Panel-Card mit solidem, dunklem Hintergrund (nicht transparent) für perfekte Lesbarkeit auf der hellen Karte
@Composable
fun BoxScope.DuelInfoPanel(viewModel: KarteViewModel = viewModel()) {
    val active = viewModel.aktivesDuell
    val currentPos = viewModel.spielerPosition
    val count = active?.spotsAnzahl ?: 3
    val allCaptured = active != null && (1..count).all { idx ->
        when (idx) {
            1 -> viewModel.spot1Captured
            2 -> viewModel.spot2Captured
            3 -> viewModel.spot3Captured
            4 -> viewModel.spot4Captured
            5 -> viewModel.spot5Captured
            else -> true
        }
    }
    val distanzText = if (active != null && currentPos != null) {
        val targetLat: Double
        val targetLng: Double
        when {
            count >= 1 && !viewModel.spot1Captured -> { targetLat = active.spot1Lat; targetLng = active.spot1Lng }
            count >= 2 && !viewModel.spot2Captured -> { targetLat = active.spot2Lat; targetLng = active.spot2Lng }
            count >= 3 && !viewModel.spot3Captured -> { targetLat = active.spot3Lat; targetLng = active.spot3Lng }
            count >= 4 && !viewModel.spot4Captured -> { targetLat = active.spot4Lat; targetLng = active.spot4Lng }
            count >= 5 && !viewModel.spot5Captured -> { targetLat = active.spot5Lat; targetLng = active.spot5Lng }
            else -> {
                val finishPos = viewModel.startPositionGeo ?: GeoPoint(50.9348, 6.9852)
                targetLat = finishPos.latitude
                targetLng = finishPos.longitude
            }
        }
        val dist = calculateDistance(currentPos.latitude, currentPos.longitude, targetLat, targetLng)
        String.format("%.0f m", dist)
    } else {
        "---"
    }

    Card(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkBackground.copy(alpha = 0.93f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aktives Duell",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val cap = (if (viewModel.spot1Captured) 1 else 0) +
                      (if (viewModel.spot2Captured) 1 else 0) +
                      (if (viewModel.spot3Captured) 1 else 0) +
                      (if (viewModel.spot4Captured) 1 else 0) +
                      (if (viewModel.spot5Captured) 1 else 0)
            DuelStatusRow(
                label = "Team Blau (Du)",
                value = "$cap / $count Spots",
                color = ActiveGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            DuelStatusRow(
                label = "Nächstes Ziel",
                value = if (allCaptured) "Ziel-Beacon" else "Nächster Spot",
                color = SpotBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            DuelStatusRow(
                label = "Distanz verbleibend",
                value = distanzText,
                color = SpotOrange
            )
        }
    }
}

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
        Canvas(modifier = Modifier.size(9.dp)) {
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

// End-Screen
@Composable
fun EndScreen(
    ergebnisse: List<Ergebnis>,
    onZurueck: () -> Unit
) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuellErstellenScreen(
    onDismiss: () -> Unit,
    onSave: (name: String, zeitLimitMinuten: Int, spots: List<GeoPoint>, gegner: String) -> Unit,
    spielerPosition: GeoPoint,
    friends: List<String>
) {
    var nameInput by remember { mutableStateOf("") }
    var gegnerInput by remember { mutableStateOf("") }
    var stundenInput by remember { mutableStateOf("0") }
    var minutenInput by remember { mutableStateOf("15") }
    
    var spotSearchInput by remember { mutableStateOf("") }
    val addedSpots = remember { mutableStateListOf<Pair<String, GeoPoint>>() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var mapChooserInstance by remember { mutableStateOf<MapView?>(null) }

    // Dynamische Suchvorschläge von Nominatim
    val dynamicSuggestions = remember { mutableStateListOf<Pair<Pair<String, String>, GeoPoint>>() }
    var isSearching by remember { mutableStateOf(false) }

    // Live-Abfrage mit Debounce (500ms)
    LaunchedEffect(spotSearchInput) {
        val trimmed = spotSearchInput.trim()
        if (trimmed.length >= 3) {
            isSearching = true
            delay(500)
            val results = searchPlacesNominatim(trimmed)
            dynamicSuggestions.clear()
            dynamicSuggestions.addAll(results)
            isSearching = false
        } else {
            dynamicSuggestions.clear()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Neues Duell konfigurieren",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(14.dp))

                // 1. Name des Duells
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

                Spacer(modifier = Modifier.height(12.dp))

                // 1b. Gegner (Team oder User) hinzufügen
                OutlinedTextField(
                    value = gegnerInput,
                    onValueChange = { gegnerInput = it },
                    label = { Text("Gegner (Team- oder Username)", color = Color.White.copy(alpha = 0.5f)) },
                    placeholder = { Text("z.B. UserTwo oder Team Rot") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TeRunBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )

                if (friends.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Schnellauswahl aus Freunden:",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        friends.forEach { friend ->
                            AssistChip(
                                onClick = { gegnerInput = friend },
                                label = { Text(friend, color = Color.White) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color.White.copy(alpha = 0.08f)
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Zeitbegrenzung (Stunden und Minuten)
                Text(
                    text = "Zeitbegrenzung (Dauer)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = stundenInput,
                        onValueChange = { stundenInput = it },
                        label = { Text("Stunden", color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = TeRunBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = minutenInput,
                        onValueChange = { minutenInput = it },
                        label = { Text("Minuten", color = Color.White.copy(alpha = 0.5f)) },
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

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Spots anlegen
                Text(
                    text = "Spots festlegen (maximal 5)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Search Bar + Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = spotSearchInput,
                        onValueChange = { spotSearchInput = it },
                        label = { Text("Spot suchen (z.B. Kino Gummersbach)", color = Color.White.copy(alpha = 0.5f)) },
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
                    Button(
                        onClick = {
                            if (spotSearchInput.isNotBlank()) {
                                if (addedSpots.size >= 5) {
                                    Toast.makeText(context, "Maximal 5 Spots erlaubt!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                
                                // Geocoding im IO Thread
                                coroutineScope.launch {
                                    val query = spotSearchInput.trim()
                                    val foundList = searchPlacesNominatim(query)
                                    val bestGeo = foundList.firstOrNull()?.second ?: run {
                                        val randomLat = spielerPosition.latitude + java.util.concurrent.ThreadLocalRandom.current().nextDouble(-0.001, 0.001)
                                        val randomLng = spielerPosition.longitude + java.util.concurrent.ThreadLocalRandom.current().nextDouble(-0.001, 0.001)
                                        GeoPoint(randomLat, randomLng)
                                    }
                                    addedSpots.add(query to bestGeo)
                                    Toast.makeText(context, "Spot '$query' hinzugefügt!", Toast.LENGTH_SHORT).show()
                                    spotSearchInput = ""
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TeRunBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // Autocomplete Vorschlagsliste
                if (isSearching) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        color = TeRunBlue,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }

                if (dynamicSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column {
                            dynamicSuggestions.forEach { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (addedSpots.size >= 5) {
                                                Toast.makeText(context, "Maximal 5 Spots erlaubt!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                addedSpots.add(suggestion.first.first to suggestion.second)
                                                Toast.makeText(context, "Spot '${suggestion.first.first}' hinzugefügt!", Toast.LENGTH_SHORT).show()
                                                spotSearchInput = ""
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Ort",
                                        tint = TeRunBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = suggestion.first.first, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(text = suggestion.first.second, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Karte-Wählen Button & Dialog
                var showMapChooser by remember { mutableStateOf(false) }
                val mapSelectedSpots = remember { mutableStateListOf<Pair<String, GeoPoint>>() }

                TeRunButton(
                    text = "📍 Ort auf Karte wählen",
                    onClick = {
                        mapSelectedSpots.clear()
                        mapSelectedSpots.addAll(addedSpots)
                        showMapChooser = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (showMapChooser) {
                    Dialog(
                        onDismissRequest = { showMapChooser = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = DarkBackground
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AndroidView(
                                    factory = { ctx ->
                                        MapView(ctx).apply {
                                            setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                                            setMultiTouchControls(true)
                                            setBuiltInZoomControls(false)
                                            controller.setZoom(17.5)
                                            controller.setCenter(spielerPosition)

                                            // Compass needle overlay
                                            val compass = CompassOverlay(ctx, InternalCompassOrientationProvider(ctx), this).apply {
                                                enableCompass()
                                            }
                                            overlays.add(compass)

                                            mapChooserInstance = this
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    update = { map ->
                                        map.overlays.clear()

                                        // Retain compass overlay during updates
                                        val compass = CompassOverlay(context, InternalCompassOrientationProvider(context), map).apply {
                                            enableCompass()
                                        }
                                        map.overlays.add(compass)

                                        // Klick-Listener hinzufügen
                                        val receiver = object : org.osmdroid.events.MapEventsReceiver {
                                            override fun singleTapConfirmedHelper(p: org.osmdroid.util.GeoPoint): Boolean {
                                                if (mapSelectedSpots.size >= 5) {
                                                    Toast.makeText(context, "Maximal 5 Spots erlaubt!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val spotNumber = mapSelectedSpots.size + 1
                                                    mapSelectedSpots.add("Spot $spotNumber" to GeoPoint(p.latitude, p.longitude))
                                                }
                                                return true
                                            }
                                            override fun longPressHelper(p: org.osmdroid.util.GeoPoint): Boolean {
                                                return false
                                            }
                                        }
                                        val eventsOverlay = org.osmdroid.views.overlay.MapEventsOverlay(receiver)
                                        map.overlays.add(eventsOverlay)

                                        // Eigener Spieler-Marker zur Orientierung
                                        val playerMarker = org.osmdroid.views.overlay.Marker(map).apply {
                                            position = spielerPosition
                                            title = "Deine Position"
                                            setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                                        }
                                        map.overlays.add(playerMarker)

                                        // Die gesetzten Pins zeichnen
                                        mapSelectedSpots.forEachIndexed { index, spot ->
                                            val marker = org.osmdroid.views.overlay.Marker(map).apply {
                                                position = spot.second
                                                title = spot.first
                                                subDescription = "Spot ${index + 1}"
                                                icon = ContextCompat.getDrawable(context, R.drawable.ic_flag_uncaptured)
                                            }
                                            map.overlays.add(marker)
                                        }
                                        map.invalidate()
                                    }
                                )

                                // Karten-Steuerungsknöpfe auf der rechten Seite (Locate, Zoom In, Zoom Out)
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 🎯 Zentrieren
                                    Button(
                                        onClick = {
                                            mapChooserInstance?.controller?.animateTo(spielerPosition)
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
                                        onClick = { mapChooserInstance?.controller?.zoomIn() },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkBackground.copy(alpha = 0.9f)),
                                        shape = RoundedCornerShape(50.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Text("➕", fontSize = 18.sp)
                                    }

                                    // ➖ Zoom Out
                                    Button(
                                        onClick = { mapChooserInstance?.controller?.zoomOut() },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkBackground.copy(alpha = 0.9f)),
                                        shape = RoundedCornerShape(50.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Text("➖", fontSize = 18.sp)
                                    }
                                }

                                // Obere Titelleiste mit Infotext
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkBackground.copy(alpha = 0.85f))
                                        .padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Spots direkt antippen (${mapSelectedSpots.size}/5)",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        IconButton(onClick = { showMapChooser = false }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Schließen",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Tippe auf die Karte, um bis zu 5 Spots als Nadeln zu platzieren.",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }

                                // Untere Steuerungsleiste
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .background(DarkBackground.copy(alpha = 0.93f))
                                        .padding(16.dp)
                                        .fillMaxWidth()
                                ) {
                                    TeRunButton(
                                        text = "Übernehmen (${mapSelectedSpots.size} Spots)",
                                        onClick = {
                                            addedSpots.clear()
                                            addedSpots.addAll(mapSelectedSpots)
                                            showMapChooser = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    TextButton(
                                        onClick = { mapSelectedSpots.clear() },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text("Auswahl zurücksetzen", color = Color.Red, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Liste der hinzugefügten Spots
                if (addedSpots.isEmpty()) {
                    Text(
                        text = "Noch keine Spots hinzugefügt.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    addedSpots.forEachIndexed { idx, spot ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}. ${spot.first}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = String.format("%.4f, %.4f", spot.second.latitude, spot.second.longitude),
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                IconButton(
                                    onClick = { addedSpots.removeAt(idx) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Spot löschen",
                                        tint = Color.Red.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Buttons zum Speichern oder Abbrechen
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Text("Abbrechen", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = {
                    val hours = stundenInput.toIntOrNull() ?: 0
                    val minutes = minutenInput.toIntOrNull() ?: 15
                    val totalMinutes = hours * 60 + minutes
                    
                    if (nameInput.isNotBlank() && addedSpots.isNotEmpty()) {
                        onSave(nameInput, totalMinutes, addedSpots.map { it.second }, gegnerInput.trim())
                    } else {
                        Toast.makeText(context, "Bitte einen Namen eingeben und mindestens einen Spot hinzufügen!", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TeRunBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Text("Erstellen", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Nominatim HTTP Suchabfrage
suspend fun searchPlacesNominatim(query: String): List<Pair<Pair<String, String>, GeoPoint>> = withContext(Dispatchers.IO) {
    if (query.length < 3) return@withContext emptyList()
    try {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = java.net.URL("https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=5")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "TeRunApp/1.0 (com.example.terun)")
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        
        if (conn.responseCode == 200) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(response)
            val results = mutableListOf<Pair<Pair<String, String>, GeoPoint>>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val displayName = obj.optString("display_name", "")
                val lat = obj.optDouble("lat", 0.0)
                val lon = obj.optDouble("lon", 0.0)
                
                val parts = displayName.split(",", limit = 2)
                val title = parts.getOrNull(0)?.trim() ?: displayName
                val subtitle = parts.getOrNull(1)?.trim() ?: ""
                
                results.add(Pair(Pair(title, subtitle), GeoPoint(lat, lon)))
            }
            results
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuelleTabContent(
    viewModel: KarteViewModel,
    onNavigateToKarte: () -> Unit
) {
    var showCreateDuelScreen by remember { mutableStateOf(false) }

    if (showCreateDuelScreen) {
        DuellErstellenScreen(
            onDismiss = { showCreateDuelScreen = false },
            onSave = { name, totalMinutes, spots, gegner ->
                viewModel.erstelleDuell(name, totalMinutes, spots, gegner)
                showCreateDuelScreen = false
            },
            spielerPosition = viewModel.spielerPosition ?: GeoPoint(50.9348, 6.9852),
            friends = viewModel.freunde
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Button zum Öffnen des neuen Erstellungs-Screens
            TeRunButton(
                text = "⚔ Neues Duell erstellen",
                onClick = { showCreateDuelScreen = true },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Verfügbare Duelle",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            viewModel.duelle.forEach { duell ->
                val hrs = duell.zeitLimitMinuten / 60
                val mins = duell.zeitLimitMinuten % 60
                val durationText = if (hrs > 0) "${hrs} Std ${mins} Min" else "${mins} Min"

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
                                text = "${duell.spotsAnzahl} Spots | Dauer: $durationText",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                            if (duell.gegner.isNotEmpty()) {
                                Text(
                                    text = "⚔ Gegner: ${duell.gegner}",
                                    color = SpotOrange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Lösch-Button für Duelle (endgültig entfernen aus Room)
                        IconButton(
                            onClick = { viewModel.loescheDuell(duell) },
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Duell löschen",
                                tint = Color.Red.copy(alpha = 0.75f)
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
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilTabContent(
    viewModel: KarteViewModel,
    onLogout: () -> Unit = {}
) {
    var editMode by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(viewModel.spielerName) }
    var teamInput by remember { mutableStateOf(viewModel.teamName) }

    var notificationsEnabled by remember { mutableStateOf(true) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
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
            StatRow(label = "Zurückgelegte Distanz", value = String.format(java.util.Locale.US, "%.2f km", viewModel.spielerGesamtDistanz))
            StatRow(label = "Absolvierte Duelle", value = viewModel.absolvierteDuelleCount.toString())
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Einstellungs-Bereich
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Einstellungen",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Abmelden
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogout() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Abmelden",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.sp
                )
                Text(
                    text = ">",
                    color = Color.White.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Profil löschen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.loescheProfil()
                        Toast.makeText(context, "Spielerprofil gelöscht!", Toast.LENGTH_SHORT).show()
                        onLogout()
                    }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profil löschen",
                    color = Color.Red.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = ">",
                    color = Color.Red.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Freunde verwalten ---
        var friendNameInput by remember { mutableStateOf("") }
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Freunde verwalten",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = friendNameInput,
                    onValueChange = { friendNameInput = it },
                    placeholder = { Text("Spielername eingeben") },
                    singleLine = true,
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
                Button(
                    onClick = {
                        val inputTrimmed = friendNameInput.trim()
                        if (inputTrimmed.isEmpty()) {
                            Toast.makeText(context, "Name darf nicht leer sein!", Toast.LENGTH_SHORT).show()
                        } else if (inputTrimmed.equals(viewModel.spielerName, ignoreCase = true)) {
                            Toast.makeText(context, "Du kannst dich nicht selbst hinzufügen!", Toast.LENGTH_SHORT).show()
                        } else if (viewModel.freunde.contains(inputTrimmed)) {
                            Toast.makeText(context, "Bereits in der Freundesliste!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.fuegeFreundHinzu(inputTrimmed) { success ->
                                if (success) {
                                    Toast.makeText(context, "Freund erfolgreich hinzugefügt!", Toast.LENGTH_SHORT).show()
                                    friendNameInput = ""
                                } else {
                                    Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TeRunBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("Hinzufügen", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Freundesliste (${viewModel.freunde.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (viewModel.freunde.isEmpty()) {
                Text(
                    text = "Noch keine Freunde hinzugefügt.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            } else {
                viewModel.freunde.forEach { friendName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = friendName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { viewModel.loescheFreund(friendName) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Freund löschen",
                                tint = Color.Red.copy(alpha = 0.75f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                val mins = viewModel.verbleibendeZeit / 60
                val secs = viewModel.verbleibendeZeit % 60
                val timeString = String.format("%02d:%02d", mins, secs)
                "Duell läuft ($timeString)"
            } else {
                "TeRun"
            },
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .background(
                    color = if (duellLaeuft) BadgeGruen else Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            val capturedText = if (duellLaeuft && viewModel.aktivesDuell != null) {
                val active = viewModel.aktivesDuell!!
                val count = active.spotsAnzahl
                val cap = (if (viewModel.spot1Captured) 1 else 0) +
                          (if (viewModel.spot2Captured) 1 else 0) +
                          (if (viewModel.spot3Captured) 1 else 0) +
                          (if (viewModel.spot4Captured) 1 else 0) +
                          (if (viewModel.spot5Captured) 1 else 0)
                "$cap / $count Spots"
            } else {
                "Kein Duell aktiv"
            }
            Text(
                text = capturedText,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}