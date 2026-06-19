// Datei: MapComponents.kt
// Paket: com.example.terun
// Quelle: developer.android.com/develop/ui/compose/graphics/draw/overview — Custom Drawing mit Canvas und Path
// Quelle: moco202612creatingcomposables.pdf — Custom Graphics & Layouts

package com.example.terun

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Zeichnet eine hochmoderne Radar-Karte mit dezentem Raster und Koordinaten-Rasterpunkten
@Composable
fun TeRunMapBackground(showStreets: Boolean = false) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridColor = Color.White.copy(alpha = 0.04f)
        val dotColor = Color.White.copy(alpha = 0.12f)

        // 1. Feines Linienraster zeichnen
        drawLine(gridColor, Offset(size.width * 0.30f, 0f), Offset(size.width * 0.30f, size.height), 2f)
        drawLine(gridColor, Offset(size.width * 0.64f, 0f), Offset(size.width * 0.64f, size.height), 2f)
        drawLine(gridColor, Offset(0f, size.height * 0.36f), Offset(size.width, size.height * 0.36f), 2f)
        drawLine(gridColor, Offset(0f, size.height * 0.70f), Offset(size.width, size.height * 0.70f), 2f)

        // 2. Rasterpunkte an den Kreuzungen (High-Tech-Blueprint-Stil)
        val intersections = listOf(
            Offset(size.width * 0.30f, size.height * 0.36f),
            Offset(size.width * 0.30f, size.height * 0.70f),
            Offset(size.width * 0.64f, size.height * 0.36f),
            Offset(size.width * 0.64f, size.height * 0.70f)
        )
        intersections.forEach { pt ->
            drawCircle(color = dotColor, radius = 5f, center = pt)
        }

        // 3. Im aktiven Zustand: Zusätzliche digitale "Straßen-Pfade" einblenden
        if (showStreets) {
            drawLine(
                color = Color.White.copy(alpha = 0.10f),
                start = Offset(0f, size.height * 0.32f),
                end = Offset(size.width, size.height * 0.32f),
                strokeWidth = 14f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(size.width * 0.18f, size.height * 0.82f),
                end = Offset(size.width * 0.78f, size.height * 0.22f),
                strokeWidth = 10f
            )
        }
    }
}

// Zeichnet eine Stecknadel (Spot) mit pulsierender Neon-Aura
@Composable
fun MapSpot(
    x: Float,
    y: Float,
    color: Color,
    hasGlow: Boolean = false
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * x, size.height * y)

        // Neon-Aura zeichnen (Fading-Kreise)
        if (hasGlow) {
            drawCircle(
                color = color.copy(alpha = 0.08f),
                radius = 48f,
                center = center
            )
            drawCircle(
                color = color.copy(alpha = 0.15f),
                radius = 28f,
                center = center
            )
        }

        // Nadelkopf (Haupt-Spot)
        drawCircle(
            color = color,
            radius = 11f,
            center = center
        )

        // Innerer leuchtender Punkt
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = 4f,
            center = center
        )

        // Nadelstiel
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x, center.y + 22f),
            strokeWidth = 4f
        )
    }
}

// Zeichnet einen Positions-Marker für Spieler mit Radar-Pulsringen
@Composable
fun PlayerMarker(
    x: Float,
    y: Float,
    color: Color,
    hasInnerDot: Boolean = false
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width * x, size.height * y)

        // Radar-Pulsringe (Aura) um die Spieler-Position
        drawCircle(
            color = color.copy(alpha = 0.05f),
            radius = 60f,
            center = center,
            style = Stroke(width = 2f)
        )
        drawCircle(
            color = color.copy(alpha = 0.12f),
            radius = 36f,
            center = center,
            style = Stroke(width = 3f)
        )

        // Weißer Außenrand
        drawCircle(
            color = Color.White,
            radius = 16f,
            center = center
        )

        // Farbiger innerer Punkt (z.B. Teamfarbe)
        drawCircle(
            color = color,
            radius = 11f,
            center = center
        )

        // Zusätzlicher weißer Kern (für aktiven Modus)
        if (hasInnerDot) {
            drawCircle(
                color = Color.White,
                radius = 4.5f,
                center = center
            )
        }
    }
}

// Zeichnet einen hochmodernen Ziel-Beacon (Leuchtfeuer) statt einer einfachen Flagge
@Composable
fun FinishFlag(
    x: Float,
    y: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val base = Offset(size.width * x, size.height * y)

        // 1. Horizontale Checkpoint-Ringe am Boden
        drawCircle(
            color = Color.Red.copy(alpha = 0.08f),
            radius = 50f,
            center = base,
            style = Stroke(width = 2f)
        )
        drawCircle(
            color = Color.Red.copy(alpha = 0.18f),
            radius = 28f,
            center = base,
            style = Stroke(width = 3f)
        )

        // 2. Vertikaler Laser-Beam (Signalstrahl)
        drawLine(
            color = Color.Red.copy(alpha = 0.35f),
            start = base,
            end = Offset(base.x, base.y - 70f),
            strokeWidth = 6f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.8f),
            start = base,
            end = Offset(base.x, base.y - 70f),
            strokeWidth = 2f
        )

        // 3. Leuchtender Laser-Kopf oben
        drawCircle(
            color = Color.Red,
            radius = 8f,
            center = Offset(base.x, base.y - 70f)
        )
        drawCircle(
            color = Color.White,
            radius = 3f,
            center = Offset(base.x, base.y - 70f)
        )
    }
}

// Wiederverwendbarer Button mit horizontalem Farbverlauf im Sci-Fi-Look
@Composable
fun TeRunButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isNegative: Boolean = false,
    isPositiveAlternative: Boolean = false
) {
    val gradientColors = when {
        isNegative -> listOf(AufgebenRot, AufgebenRotLight)
        isPositiveAlternative -> listOf(ActiveGreen, BadgeGruen)
        else -> listOf(TeRunBlue, TeRunBlueLight)
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(52.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(colors = gradientColors),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Transparenter Glasmorphismus-Container passend zu den Login-Eingabefeldern
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
