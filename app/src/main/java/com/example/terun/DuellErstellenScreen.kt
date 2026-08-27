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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuellErstellenScreen(
    onDismiss: () -> Unit,
    onSave: (name: String, zeitLimitMinuten: Int, spots: List<LatLng>, gegner: String) -> Unit,
    spielerPosition: LatLng,
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
    val addedSpots = remember { mutableStateListOf<Pair<String, LatLng>>() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Dynamische Suchvorschläge von Nominatim
    val dynamicSuggestions = remember { mutableStateListOf<Pair<Pair<String, String>, LatLng>>() }
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
                                        val randomLat = (spielerPosition?.latitude ?: 50.9348) + java.util.concurrent.ThreadLocalRandom.current().nextDouble(-0.001, 0.001)
                                        val randomLng = (spielerPosition?.longitude ?: 6.9852) + java.util.concurrent.ThreadLocalRandom.current().nextDouble(-0.001, 0.001)
                                        LatLng(randomLat, randomLng)
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
                val mapSelectedSpots = remember { mutableStateListOf<Pair<String, LatLng>>() }

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
                                // Google Maps Karte für Spot-Auswahl
                                val chooserCameraState = rememberCameraPositionState {
                                    val startPos = spielerPosition ?: LatLng(50.9348, 6.9852)
                                    position = CameraPosition.fromLatLngZoom(startPos, 17f)
                                }
                                val chooserScope = rememberCoroutineScope()

                                GoogleMap(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraPositionState = chooserCameraState,
                                    properties = MapProperties(isMyLocationEnabled = false),
                                    uiSettings = MapUiSettings(zoomControlsEnabled = false),
                                    onMapClick = { latLng ->
                                        if (mapSelectedSpots.size >= 5) {
                                            Toast.makeText(context, "Maximal 5 Spots erlaubt!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val spotNumber = mapSelectedSpots.size + 1
                                            mapSelectedSpots.add("Spot $spotNumber" to latLng)
                                        }
                                    }
                                ) {
                                    // Eigener Spieler-Marker zur Orientierung
                                    val playerPos = spielerPosition
                                    if (playerPos != null) {
                                        Marker(
                                            state = MarkerState(position = playerPos),
                                            title = "Deine Position",
                                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                                        )
                                    }
                                    // Gesetzte Spot-Pins anzeigen
                                    mapSelectedSpots.forEachIndexed { index, spot ->
                                        Marker(
                                            state = MarkerState(position = spot.second),
                                            title = spot.first,
                                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                                        )
                                    }
                                }

                                // Karten-Steuerungsknöpfe auf der rechten Seite
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 🎯 Zentrieren
                                    Button(
                                        onClick = {
                                            val pos = spielerPosition ?: LatLng(50.9348, 6.9852)
                                            chooserScope.launch {
                                                chooserCameraState.animate(CameraUpdateFactory.newLatLngZoom(pos, 17f))
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
                                            chooserScope.launch {
                                                chooserCameraState.animate(CameraUpdateFactory.zoomIn())
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
                                            chooserScope.launch {
                                                chooserCameraState.animate(CameraUpdateFactory.zoomOut())
                                            }
                                        },
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
suspend fun searchPlacesNominatim(query: String): List<Pair<Pair<String, String>, LatLng>> = withContext(Dispatchers.IO) {
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
            val results = mutableListOf<Pair<Pair<String, String>, LatLng>>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val displayName = obj.optString("display_name", "")
                val lat = obj.optDouble("lat", 0.0)
                val lon = obj.optDouble("lon", 0.0)
                
                val parts = displayName.split(",", limit = 2)
                val title = parts.getOrNull(0)?.trim() ?: displayName
                val subtitle = parts.getOrNull(1)?.trim() ?: ""
                
                results.add(Pair(Pair(title, subtitle), LatLng(lat, lon)))
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
