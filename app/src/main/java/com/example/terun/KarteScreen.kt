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

                                    val gpsPos = viewModel.spielerPosition
                                    if (gpsPos != null && !hasCenteredMap) {
                                        mapView.controller.setCenter(gpsPos)
                                        hasCenteredMap = true
                                    } else if (gpsPos == null && !hasCenteredMap) {
                                        mapView.controller.setCenter(GeoPoint(50.9348, 6.9852))
                                    }

                                    val currentPos = gpsPos ?: GeoPoint(50.9348, 6.9852)

                                    // 1. Eigener Spieler-Marker auf der Karte
                                    val playerMarker = Marker(mapView).apply {
                                        position = currentPos
                                        title = viewModel.spielerName
                                        icon = createUserLocationDot(context)
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    }
                                    mapView.overlays.add(playerMarker)

                                    val active = viewModel.aktivesDuell
                                    if (duellLaeuft && active != null) {
                                        // 2. Ziel-Beacon (Startpunkt des Duells)
                                        val finishPos = viewModel.startPositionGeo ?: GeoPoint(50.9348, 6.9852)
                                        val finishMarker = Marker(mapView).apply {
                                            position = finishPos
                                            title = "Ziel-Beacon (Startort)"
                                            icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_myplaces)
                                        }
                                        mapView.overlays.add(finishMarker)

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
                                            if (viewModel.routePoints.isNotEmpty()) {
                                                setPoints(viewModel.routePoints)
                                            } else {
                                                setPoints(listOf(currentPos, nextSpotGeo))
                                            }
                                            outlinePaint.color = android.graphics.Color.parseColor("#0088FF")
                                            outlinePaint.strokeWidth = 8f
                                            outlinePaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(15f, 15f), 0f)
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



// End-Screen
@Composable
fun EndScreen(
    ergebnisse: List<Ergebnis>,
    onZurueck: () -> Unit
) {
    // Sortiert nach: nicht aufgegeben zuerst, dann nach Anzahl der eroberten Spots absteigend
    val sortiert = ergebnisse.sortedWith(
        compareBy<Ergebnis> { it.aufgegeben }
            .thenByDescending { it.spots }
    )
    val winner = sortiert.firstOrNull { !it.aufgegeben } ?: sortiert.firstOrNull()

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
            text = "Ergebnis",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Große Winner Card
        if (winner != null) {
            val announcement = if (winner.aufgegeben) {
                "Kein Gewinner (alle aufgegeben)"
            } else {
                "${winner.name} gewinnt!"
            }

            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Gewinner",
                        color = SpotOrange,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = announcement,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!winner.aufgegeben) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${winner.spots} Spots erreicht",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Platzierungen:",
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(sortiert) { index, ergebnis ->
                ErgebnisZeile(
                    platz = index + 1,
                    name = ergebnis.name,
                    spots = ergebnis.spots,
                    aufgegeben = ergebnis.aufgegeben
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
    spots: Int,
    aufgegeben: Boolean
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
            text = if (aufgegeben) "Aufgegeben" else "$spots Spots",
            color = if (aufgegeben) Color.Red.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuellErstellenScreen(
    onDismiss: () -> Unit,
    onSave: (name: String, zeitLimitMinuten: Int, spots: List<GeoPoint>, gegner: String) -> Unit,
    spielerPosition: GeoPoint,
    currentUserName: String,
    searchUsers: suspend (String) -> List<String>
) {
    var nameInput by remember { mutableStateOf("") }
    var gegnerSearchQuery by remember { mutableStateOf("") }
    val selectedGegner = remember { mutableStateListOf<String>() }
    val gegnerSuggestions = remember { mutableStateListOf<String>() }
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

    // Gegner-Live-Abfrage (Autocomplete aus Datenbank)
    LaunchedEffect(gegnerSearchQuery) {
        val query = gegnerSearchQuery.trim()
        if (query.isNotEmpty()) {
            val matches = searchUsers(query)
            gegnerSuggestions.clear()
            // Exclude current user and already selected opponents
            gegnerSuggestions.addAll(
                matches.filter { it != currentUserName && !selectedGegner.contains(it) }
            )
        } else {
            gegnerSuggestions.clear()
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

                // 1b. Gegner hinzufügen (bis zu 6 Benutzer)
                OutlinedTextField(
                    value = gegnerSearchQuery,
                    onValueChange = { gegnerSearchQuery = it },
                    label = { Text("Gegner suchen & hinzufügen (maximal 6)", color = Color.White.copy(alpha = 0.5f)) },
                    placeholder = { Text("Benutzername eintippen...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = TeRunBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )

                // Autocomplete-Vorschläge
                if (gegnerSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column {
                            gegnerSuggestions.forEach { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (selectedGegner.size >= 6) {
                                                Toast.makeText(context, "Maximal 6 Gegner erlaubt!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                selectedGegner.add(suggestion)
                                                gegnerSearchQuery = ""
                                            }
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text(text = suggestion, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Ausgewählte Gegner anzeigen
                if (selectedGegner.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ausgewählte Gegner (${selectedGegner.size}/6):",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedGegner.forEach { name ->
                            Row(
                                modifier = Modifier
                                    .background(TeRunBlue.copy(alpha = 0.18f), shape = RoundedCornerShape(16.dp))
                                    .border(1.dp, TeRunBlue.copy(alpha = 0.4f), shape = RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Entfernen",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { selectedGegner.remove(name) }
                                )
                            }
                        }
                    }
                }



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
                        onValueChange = { newVal ->
                            val filtered = newVal.filter { it.isDigit() }
                            if (filtered.length <= 2) {
                                val num = filtered.toIntOrNull()
                                if (num == null || num <= 24) {
                                    stundenInput = filtered
                                }
                            }
                        },
                        label = { Text("Stunden", color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        onValueChange = { newVal ->
                            val filtered = newVal.filter { it.isDigit() }
                            if (filtered.length <= 2) {
                                val num = filtered.toIntOrNull()
                                if (num == null || num <= 60) {
                                    minutenInput = filtered
                                }
                            }
                        },
                        label = { Text("Minuten", color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                            icon = createUserLocationDot(context)
                                            setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_CENTER)
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
                    val minutes = minutenInput.toIntOrNull() ?: 0
                    val totalMinutes = hours * 60 + minutes
                    
                    if (nameInput.isBlank()) {
                        Toast.makeText(context, "Bitte einen Duell-Namen eingeben!", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    if (totalMinutes <= 0) {
                        Toast.makeText(context, "Die Dauer muss mindestens 1 Minute sein!", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    if (selectedGegner.isEmpty()) {
                        Toast.makeText(context, "Bitte mindestens einen Gegner hinzufügen!", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    if (addedSpots.isEmpty()) {
                        Toast.makeText(context, "Bitte mindestens einen Spot hinzufügen!", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    onSave(nameInput, totalMinutes, addedSpots.map { it.second }, selectedGegner.joinToString(", "))
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
            spielerPosition = viewModel.spielerPosition ?: GeoPoint(0.0, 0.0),
            currentUserName = viewModel.spielerName,
            searchUsers = { viewModel.sucheBenutzerNamen(it) }
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
    var showDeleteConfirmation by remember { mutableStateOf(false) }

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
                } else {
                    Text(
                        text = viewModel.spielerName,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
                        showDeleteConfirmation = true
                    }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showDeleteConfirmation) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirmation = false },
                        title = {
                            Text(
                                text = "Konto löschen?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = "Sind Sie sicher, dass Sie Ihr Konto löschen wollen? Diese Aktion kann nicht rückgängig gemacht werden.",
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteConfirmation = false
                                    viewModel.loescheProfil {
                                        Toast.makeText(context, "Konto erfolgreich gelöscht!", Toast.LENGTH_SHORT).show()
                                        onLogout()
                                    }
                                }
                            ) {
                                Text("Löschen", color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirmation = false }) {
                                Text("Abbrechen", color = Color.White.copy(alpha = 0.6f))
                            }
                        },
                        containerColor = DarkBackground,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
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
                } else {
                    nameInput = viewModel.spielerName
                }
                editMode = !editMode
            },
            isPositiveAlternative = editMode,
            modifier = Modifier.fillMaxWidth()
        )
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

fun createUserLocationDot(context: Context): android.graphics.drawable.Drawable {
    val density = context.resources.displayMetrics.density
    val size = (24 * density).toInt()
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    // Outer light blue semi-transparent glow/circle (Google Maps style)
    paint.color = android.graphics.Color.parseColor("#440088FF")
    canvas.drawCircle(size / 2f, size / 2f, 11 * density, paint)

    // White outline circle
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, 7.5f * density, paint)

    // Central solid blue dot
    paint.color = android.graphics.Color.parseColor("#0088FF")
    canvas.drawCircle(size / 2f, size / 2f, 5.5f * density, paint)

    return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
}