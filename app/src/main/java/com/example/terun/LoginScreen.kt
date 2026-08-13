package com.example.terun

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * =====================================================================
 * LoginScreen – Willkommensscreen der TeRun-App
 * =====================================================================
 *
 * VORLESUNG 11 – Declarative UI:
 * Die gesamte Oberfläche wird deklarativ beschrieben: Box, Column, Text, Button.
 * Kein manuelles Setzen von Werten (setText) – stattdessen beschreibt die Funktion
 * wie die UI aussehen soll, und Compose rendert sie automatisch.
 *
 * VORLESUNG 12 – Creating Composables:
 * LoginScreen ist ein zusammengesetztes Composable aus TeRunLogo, Text und Button.
 * @Preview erlaubt eine Vorschau im Android Studio ohne die App zu starten.
 *
 * VORLESUNG 14 – State Hoisting:
 * Die Button-Callbacks (onSignInClicked, onRegisterClicked) werden als Parameter
 * von außen übergeben. LoginScreen hat keinen eigenen Zustand → stateless.
 *
 * VORLESUNG 16 – Navigation:
 * Die Callbacks navigieren im NavHost zu SignInRoute bzw. RegisterRoute.
 * LoginScreen selbst kennt den NavController NICHT (State Hoisting Prinzip).
 */
@Composable
fun LoginScreen(
    onSignInClicked: () -> Unit,
    onRegisterClicked: () -> Unit
) {
    // Äußerer Container für den gesamten Screen
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
            // TeRun-Logo aus TeRunLogo.kt
            TeRunLogo(size = 88.dp)

            Spacer(modifier = Modifier.height(20.dp))

            // Großer App-Name
            Text(
                text = "TeRun",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            // App-Untertitel
            Text(
                text = "Territory Run",
                fontSize = 17.sp,
                color = Color.White.copy(alpha = 0.45f)
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Anmelden Button (Primary Button)
            Button(
                onClick = onSignInClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeRunBlue
                )
            ) {
                Text(
                    text = "Anmelden",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Registrieren Button (Outlined Button)
            OutlinedButton(
                onClick = onRegisterClicked,
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
                    text = "Registrieren",
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
        onSignInClicked = {},
        onRegisterClicked = {}
    )
}
