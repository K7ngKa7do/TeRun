package com.example.terun

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 80.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(TeRunBlue),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(56.dp)) {
                    val w = size.width
                    val h = size.height

                    // Linker oberer Balken mit Rundung
                    val topLeft = Path().apply {
                        moveTo(w * 0.08f, h * 0.15f)

                        cubicTo(
                            w * 0.10f, h * 0.08f,
                            w * 0.16f, h * 0.06f,
                            w * 0.24f, h * 0.06f
                        )

                        lineTo(w * 0.42f, h * 0.06f)

                        // geschwungene schräge Lücke im T
                        cubicTo(
                            w * 0.35f, h * 0.10f,
                            w * 0.31f, h * 0.17f,
                            w * 0.30f, h * 0.25f
                        )

                        lineTo(w * 0.09f, h * 0.25f)

                        cubicTo(
                            w * 0.04f, h * 0.25f,
                            w * 0.03f, h * 0.18f,
                            w * 0.08f, h * 0.15f
                        )

                        close()
                    }
                    drawPath(topLeft, color = Color.White)

                    // Rechter oberer Balken mit Rundung
                    val topRight = Path().apply {
                        moveTo(w * 0.48f, h * 0.06f)

                        lineTo(w * 0.90f, h * 0.06f)

                        cubicTo(
                            w * 0.98f, h * 0.06f,
                            w * 0.99f, h * 0.25f,
                            w * 0.89f, h * 0.25f
                        )

                        lineTo(w * 0.52f, h * 0.25f)

                        // geschwungene innere Kante passend zur Lücke
                        cubicTo(
                            w * 0.48f, h * 0.18f,
                            w * 0.49f, h * 0.11f,
                            w * 0.48f, h * 0.06f
                        )

                        close()
                    }
                    drawPath(topRight, color = Color.White)

                    // Schräger Stamm des T
                    val stem = Path().apply {
                        moveTo(w * 0.50f, h * 0.25f)

                        cubicTo(
                            w * 0.48f, h * 0.30f,
                            w * 0.46f, h * 0.36f,
                            w * 0.44f, h * 0.42f
                        )

                        lineTo(w * 0.30f, h * 0.84f)

                        cubicTo(
                            w * 0.27f, h * 0.94f,
                            w * 0.22f, h * 0.98f,
                            w * 0.13f, h * 0.98f
                        )

                        lineTo(w * 0.03f, h * 0.98f)

                        lineTo(w * 0.28f, h * 0.25f)

                        lineTo(w * 0.50f, h * 0.25f)

                        close()
                    }
                    drawPath(stem, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "TeRun",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Territory Run",
                fontSize = 17.sp,
                color = Color.White.copy(alpha = 0.45f)
            )

            Spacer(modifier = Modifier.height(56.dp))

            Button(
                onClick = onStartClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
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

            OutlinedButton(
                onClick = onLoginClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(
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
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onStartClicked = {},
        onLoginClicked = {}
    )
}
