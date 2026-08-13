package com.example.terun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * =====================================================================
 * TeRunLogo – Wiederverwendbares Composable für das App-Logo
 * =====================================================================
 *
 * VORLESUNG 12 – Creating Composables:
 * Ein Composable sollte klein, wiederverwendbar und mit Parametern steuerbar sein.
 * TeRunLogo hat einen Parameter 'size', mit dem die Größe angepasst werden kann.
 * So kann dasselbe Logo auf verschiedenen Screens in unterschiedlichen Größen verwendet werden.
 *
 * VORLESUNG 13 – Modifier:
 * Modifier werden verkettet (chained) und bestimmen Größe, Form und Hintergrund.
 * .size() → Abmessungen | .clip() → abgerundete Ecken | .background() → Farbe
 */
@Composable
fun TeRunLogo(size: Dp = 88.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .background(TeRunBlue),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "T",
            color = Color.White,
            fontSize = (size.value * 0.62f).sp,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center
        )
    }
}

