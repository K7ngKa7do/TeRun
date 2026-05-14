// Datei: RegisterScreen.kt
// Paket: com.example.terun

package com.example.terun

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
@Composable
fun RegisterScreen(
    onRegisterClicked: () -> Unit,
    onSignInClicked: () -> Unit
) {
    // Box füllt den ganzen Bildschirm mit dunklem Hintergrund
    // Quelle: moco202613composablesmodifier.pdf — fillMaxSize, background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Column stapelt alle Elemente untereinander
        // Quelle: moco202612creatingcomposables.pdf — Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
        ) {

            // Logo
            // Quelle: TeRunLogo.kt — wiederverwendbare Composable Funktion
            TeRunLogo(size = 100.dp)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "TeRun",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Titel
            // Quelle: moco202612creatingcomposables.pdf — Text
            Text(
                text = "Konto erstellen",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(20.dp))

            // State für alle Eingabefelder
            // Quelle: moco202614recompositionstates.pdf — remember, mutableStateOf
            var spielername by remember { mutableStateOf("") }
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var passwordWiederholen by remember { mutableStateOf("") }

            // Spielername Eingabefeld
            // Externe Quelle: developer.android.com/develop/ui/compose/text/user-input
            OutlinedTextField(
                value = spielername,
                onValueChange = { spielername = it },
                placeholder = {
                    Text(
                        text = "Spielername",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Person,
                        contentDescription = "Spielername",
                        tint = Color.White.copy(alpha = 0.3f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TeRunBlue,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = TeRunBlue,
                    focusedContainerColor = Color.White.copy(alpha = 0.06f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.06f)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // E-Mail Eingabefeld
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = {
                    Text(
                        text = "E-Mail",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Email,
                        contentDescription = "E-Mail",
                        tint = Color.White.copy(alpha = 0.3f)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TeRunBlue,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = TeRunBlue,
                    focusedContainerColor = Color.White.copy(alpha = 0.06f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.06f)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Passwort Eingabefeld
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = {
                    Text(
                        text = "Passwort",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                        contentDescription = "Passwort",
                        tint = Color.White.copy(alpha = 0.3f)
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TeRunBlue,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = TeRunBlue,
                    focusedContainerColor = Color.White.copy(alpha = 0.06f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.06f)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Passwort wiederholen Eingabefeld
            OutlinedTextField(
                value = passwordWiederholen,
                onValueChange = { passwordWiederholen = it },
                placeholder = {
                    Text(
                        text = "Passwort wiederholen",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                        contentDescription = "Passwort wiederholen",
                        tint = Color.White.copy(alpha = 0.3f)
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TeRunBlue,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = TeRunBlue,
                    focusedContainerColor = Color.White.copy(alpha = 0.06f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.06f)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Konto erstellen Button
            // Quelle: moco202612creatingcomposables.pdf — Button
            Button(
                onClick = onRegisterClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeRunBlue
                )
            ) {
                Text(
                    text = "Konto erstellen",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hinweis für bestehende Nutzer
            // Quelle: moco202613composablesmodifier.pdf — clickable
            Row {
                Text(
                    text = "Bereits ein Konto? ",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
                Text(
                    text = "Jetzt anmelden",
                    fontSize = 12.sp,
                    color = TeRunBlue,
                    modifier = Modifier.clickable { onSignInClicked() }
                )
            }
        }
    }
}

// Quelle: moco202612creatingcomposables.pdf — @Preview
@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen(
        onRegisterClicked = {},
        onSignInClicked = {}
    )
}

