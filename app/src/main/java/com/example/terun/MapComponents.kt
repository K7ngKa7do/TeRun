// Datei: MapComponents.kt
// Paket: com.example.terun
// Quelle: developer.android.com/develop/ui/compose/graphics/draw/overview — Custom Drawing mit Canvas und Path
// Quelle: moco202612creatingcomposables.pdf — wiederverwendbare Zeichen-Elemente für unsere Karte

package com.example.terun

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

// Zeichnet das Straßen- und Rastergitter der Karte.
// Kann im aktiven Duell-Zustand zusätzlich Straßen (Diagonalen) zeichnen.
@Composable
fun TeRunMapBackground(showStreets: Boolean = false) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val grid = Color.White.copy(alpha = 0.08f)

        // Vertikale Rasterlinien
        drawLine(grid, Offset(size.width * 0.30f, 0f), Offset(size.width * 0.30f, size.height), 3f)
        drawLine(grid, Offset(size.width * 0.64f, 0f), Offset(size.width * 0.64f, size.height), 3f)

        // Horizontale Rasterlinien
        drawLine(grid, Offset(0f, size.height * 0.36f), Offset(size.width, size.height * 0.36f), 3f)
        drawLine(grid, Offset(0f, size.height * 0.70f), Offset(size.width, size.height * 0.70f), 3f)

        // Wenn Straßen angezeigt werden sollen (z.B. im aktiven Duell)
        if (showStreets) {
            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = Offset(0f, size.height * 0.32f),
                end = Offset(size.width, size.height * 0.32f),
                strokeWidth = 14f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.10f),
                start = Offset(size.width * 0.18f, size.height * 0.82f),
                end = Offset(size.width * 0.78f, size.height * 0.22f),
                strokeWidth = 10f
            )
        }
    }
}

// Zeichnet eine Stecknadel (Spot) auf der Karte mit einer optionalen Leuchtaura (Glow)
@Composable
fun MapSpot(
    x: Float,
    y: Float,
    color: Color,
    hasGlow: Boolean = false
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * x, size.height * y)

        if (hasGlow) {
            drawCircle(
                color = color.copy(alpha = 0.25f),
                radius = 24f,
                center = center
            )
        }

        // Spot-Nadelkopf
        drawCircle(
            color = color,
            radius = 10f,
            center = center
        )

        // Spot-Nadelstiel
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x, center.y + 21f),
            strokeWidth = 4f
        )
    }
}

// Zeichnet einen Positions-Marker für Spieler.
// Mit optionalem inneren weißen Kreis (zur Unterscheidung).
@Composable
fun PlayerMarker(
    x: Float,
    y: Float,
    color: Color,
    hasInnerDot: Boolean = false
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * x, size.height * y)

        // Weißer Außenrand
        drawCircle(
            color = Color.White,
            radius = 18f,
            center = center
        )

        // Farbiger innerer Punkt (z.B. Teamfarbe)
        drawCircle(
            color = color,
            radius = 11f,
            center = center
        )

        // Optionaler innerer weißer Kern (wie in DuellLaueftScreen gezeigt)
        if (hasInnerDot) {
            drawCircle(
                color = Color.White,
                radius = 5f,
                center = center
            )
        }
    }
}

// Zeichnet die Zielflagge (das Duell-Ziel).
@Composable
fun FinishFlag(
    x: Float,
    y: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val base = Offset(size.width * x, size.height * y)

        // Flaggenmast
        drawLine(
            color = Color.White,
            start = base,
            end = Offset(base.x, base.y + 35f),
            strokeWidth = 4f
        )

        // Rote Flagge
        drawLine(
            color = Color.Red,
            start = base,
            end = Offset(base.x + 21f, base.y + 8.5f),
            strokeWidth = 8f
        )
    }
}
