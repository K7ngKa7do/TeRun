package com.example.terun

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val DarkBackground = Color(0xFF0D1B2A)
val TeRunBlue = Color(0xFF1A6FF5)

@Composable
fun LoginScreen(
    onStartClicked: () -> Unit,
    onLoginClicked: () -> Unit
) {
    // Box füllt den ganzen Bildschirm
    // Quelle: moco202613composablesmodifier.pdf — fillMaxSize, background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        // Column stapelt alle Elemente untereinander
        // Quelle: moco202612creatingcomposables.pdf — Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {

            // Logo-Box — blaues Quadrat mit T
            // Quelle: moco202613composablesmodifier.pdf — clip, background, size
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(TeRunBlue),
                contentAlignment = Alignment.Center
            ) {
                // Canvas zeichnet das geschwungene T
                // Quelle: developer.android.com/develop/ui/compose/graphics/draw/overview
                Canvas(modifier = Modifier.size(56.dp)) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(w * 0.15f, h * 0.18f)
                        cubicTo(w * 0.3f, h * 0.08f, w * 0.7f, h * 0.08f, w * 0.88f, h * 0.18f)
                        moveTo(w * 0.55f, h * 0.18f)
                        cubicTo(
                            w * 0.55f, h * 0.45f,
                            w * 0.38f, h * 0.65f,
                            w * 0.18f, h * 0.92f
                        )
                    }
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(
                            width = 7.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }
            } // ← schließt die Logo-Box

            Spacer(modifier = Modifier.height(20.dp))

            // App-Name
            // Quelle: moco202612creatingcomposables.pdf — Text
            Text(
                text = "TeRun",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Untertitel
            Text(
                text = "Territory Run",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.45f)
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Primär-Button
            // Quelle: moco202612creatingcomposables.pdf — Button
            // Externe Quelle: developer.android.com/develop/ui/compose/components/button
            Button(
                onClick = onStartClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeRunBlue
                )
            ) {
                Text(
                    text = "Jetzt starten",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ghost-Button
            // Externe Quelle: developer.android.com/develop/ui/compose/components/button
            OutlinedButton(
                onClick = onLoginClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    Color.White.copy(alpha = 0.4f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Bereits registriert",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        } // ← schließt Column
    } // ← schließt Box
} // ← schließt LoginScreen

// Quelle: moco202612creatingcomposables.pdf — @Preview
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onStartClicked = {},
        onLoginClicked = {}
    )
}


