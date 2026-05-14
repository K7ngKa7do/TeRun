// Datei: TeRunLogo.kt
// Paket: com.example.terun
// Quelle: moco202612creatingcomposables.pdf — wiederverwendbare Composable Funktion

package com.example.terun

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

// Quelle: moco202612creatingcomposables.pdf — @Composable, wiederverwendbare Funktion
// Mit size-Parameter kann man das Logo in verschiedenen Groessen einsetzen
@Composable
fun TeRunLogo(size: Dp = 88.dp) {

    // Box ist der blaue Hintergrund des Logos
    // Quelle: moco202613composablesmodifier.pdf — size, clip, background
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.23f))
            .background(TeRunBlue),
        contentAlignment = Alignment.Center
    ) {
        // Canvas zeichnet das T
        // Quelle: developer.android.com/develop/ui/compose/graphics/draw/overview
        Canvas(modifier = Modifier.size(size * 0.64f)) {
            val w = this.size.width
            val h = this.size.height

            // Linker oberer Balken mit Rundung
            val topLeft = Path().apply {
                moveTo(w * 0.08f, h * 0.15f)
                cubicTo(w * 0.10f, h * 0.08f, w * 0.16f, h * 0.06f, w * 0.24f, h * 0.06f)
                lineTo(w * 0.42f, h * 0.06f)
                cubicTo(w * 0.35f, h * 0.10f, w * 0.31f, h * 0.17f, w * 0.30f, h * 0.25f)
                lineTo(w * 0.09f, h * 0.25f)
                cubicTo(w * 0.04f, h * 0.25f, w * 0.03f, h * 0.18f, w * 0.08f, h * 0.15f)
                close()
            }
            drawPath(topLeft, color = Color.White)

            // Rechter oberer Balken mit Rundung
            val topRight = Path().apply {
                moveTo(w * 0.48f, h * 0.06f)
                lineTo(w * 0.90f, h * 0.06f)
                cubicTo(w * 0.98f, h * 0.06f, w * 0.99f, h * 0.25f, w * 0.89f, h * 0.25f)
                lineTo(w * 0.52f, h * 0.25f)
                cubicTo(w * 0.48f, h * 0.18f, w * 0.49f, h * 0.11f, w * 0.48f, h * 0.06f)
                close()
            }
            drawPath(topRight, color = Color.White)

            // Schraeger Stamm des T
            val stem = Path().apply {
                moveTo(w * 0.50f, h * 0.25f)
                cubicTo(w * 0.48f, h * 0.30f, w * 0.46f, h * 0.36f, w * 0.44f, h * 0.42f)
                lineTo(w * 0.30f, h * 0.84f)
                cubicTo(w * 0.27f, h * 0.94f, w * 0.22f, h * 0.98f, w * 0.13f, h * 0.98f)
                lineTo(w * 0.03f, h * 0.98f)
                lineTo(w * 0.28f, h * 0.25f)
                lineTo(w * 0.50f, h * 0.25f)
                close()
            }
            drawPath(stem, color = Color.White)
        }
    }
}

