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
                            viewModel.fuegeFreundHinzu(inputTrimmed) { result ->
                                when (result) {
                                    "SUCCESS" -> {
                                        Toast.makeText(context, "Freundschaftsanfrage gesendet!", Toast.LENGTH_SHORT).show()
                                        friendNameInput = ""
                                    }
                                    "ALREADY_SENT" -> {
                                        Toast.makeText(context, "Freundschaftsanfrage bereits geschickt!", Toast.LENGTH_SHORT).show()
                                    }
                                    "ALREADY_FRIENDS" -> {
                                        Toast.makeText(context, "Ihr seid bereits befreundet!", Toast.LENGTH_SHORT).show()
                                    }
                                    "SELF_REQUEST" -> {
                                        Toast.makeText(context, "Du kannst dich nicht selbst hinzufügen!", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        Toast.makeText(context, "Spieler nicht gefunden", Toast.LENGTH_SHORT).show()
                                    }
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

            // --- Eingehende Freundschaftsanfragen ---
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "📩 Eingehende Freundschaftsanfragen (${viewModel.ausstehendeFreundesanfragen.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (viewModel.ausstehendeFreundesanfragen.isEmpty()) {
                Text(
                    text = "Keine eingehenden Anfragen.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            } else {
                viewModel.ausstehendeFreundesanfragen.forEach { senderName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = senderName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { viewModel.antworteAufFreundesanfrage(senderName, akzeptiert = true) },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Zustimmen", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = { viewModel.antworteAufFreundesanfrage(senderName, akzeptiert = false) },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Ablehnen", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // --- Gesendete (ausstehende) Freundschaftsanfragen ---
            // [Kompatibilität 27.08.2026] Dieser vorhandene Bereich bleibt im Code erhalten,
            // ist aber deaktiviert, weil der aktuelle KarteViewModel-Stand die dafür verwendeten
            // States/Funktionen gesendeteFreundesanfragen, resetAlleFreundschaftsanfragen()
            // und zieheFreundesanfrageZurueck(...) nicht enthält.
//             Spacer(modifier = Modifier.height(16.dp))
//             HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
//             Spacer(modifier = Modifier.height(12.dp))
//             Row(
//                 modifier = Modifier.fillMaxWidth(),
//                 horizontalArrangement = Arrangement.SpaceBetween,
//                 verticalAlignment = Alignment.CenterVertically
//             ) {
//                 Text(
//                     text = "📤 Gesendete Anfragen (${viewModel.gesendeteFreundesanfragen.size})",
//                     color = Color.White,
//                     fontWeight = FontWeight.Bold,
//                     fontSize = 14.sp
//                 )
//                 TextButton(
//                     onClick = {
//                         viewModel.resetAlleFreundschaftsanfragen()
//                         Toast.makeText(context, "Anfragen zurückgesetzt!", Toast.LENGTH_SHORT).show()
//                     },
//                     contentPadding = PaddingValues(0.dp)
//                 ) {
//                     Text("↺ Alle zurücksetzen", color = SpotOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
//                 }
//             }
//             Spacer(modifier = Modifier.height(8.dp))
//             if (viewModel.gesendeteFreundesanfragen.isEmpty()) {
//                 Text(
//                     text = "Keine ausstehenden gesendeten Anfragen.",
//                     color = Color.White.copy(alpha = 0.5f),
//                     fontSize = 12.sp
//                 )
//             } else {
//                 viewModel.gesendeteFreundesanfragen.forEach { receiverName ->
//                     Row(
//                         modifier = Modifier
//                             .fillMaxWidth()
//                             .padding(vertical = 4.dp),
//                         horizontalArrangement = Arrangement.SpaceBetween,
//                         verticalAlignment = Alignment.CenterVertically
//                     ) {
//                         Column {
//                             Text(
//                                 text = receiverName,
//                                 color = Color.White,
//                                 fontSize = 13.sp,
//                                 fontWeight = FontWeight.Bold
//                             )
//                             Text(
//                                 text = "Wartet auf Antwort...",
//                                 color = Color.White.copy(alpha = 0.5f),
//                                 fontSize = 11.sp
//                             )
//                         }
//                         TextButton(
//                             onClick = {
//                                 viewModel.zieheFreundesanfrageZurueck(receiverName)
//                                 Toast.makeText(context, "Anfrage an $receiverName zurückgezogen!", Toast.LENGTH_SHORT).show()
//                             },
//                             contentPadding = PaddingValues(horizontal = 8.dp),
//                             modifier = Modifier.height(32.dp)
//                         ) {
//                             Text("Zurückziehen", color = Color.Red.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
//                         }
//                     }
//                 }
//             }
//
//             Spacer(modifier = Modifier.height(16.dp))
//             HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
//             Spacer(modifier = Modifier.height(12.dp))
//

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
