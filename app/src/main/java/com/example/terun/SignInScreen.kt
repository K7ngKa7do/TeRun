// Datei: SignInScreen.kt
// Paket: com.example.terun

package com.example.terun

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    repository: SpielRepository,
    onSignInClicked: () -> Unit,      // Wird aufgerufen wenn Anmelden erfolgreich
    onRegisterClicked: () -> Unit     // Wird aufgerufen wenn "Jetzt registrieren" geklickt
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 44.dp)
        ) {
            TeRunLogo(size = 100.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "TeRun",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Willkommen zurück",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Melde dich mit deinem Konto an",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // E-Mail Eingabefeld
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    errorMessage = null 
                },
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                modifier = Modifier.fillMaxWidth().focusRequester(emailFocusRequester),
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
                onValueChange = { 
                    password = it
                    errorMessage = null 
                },
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth().focusRequester(passwordFocusRequester),
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

            Spacer(modifier = Modifier.height(6.dp))

            // Rote Fehlermeldung
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Anmelden Button
            Button(
                onClick = {
                    errorMessage = null
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Bitte E-Mail und Passwort eingeben!"
                        return@Button
                    }
                    coroutineScope.launch {
                        val user = repository.holeBenutzer(email.trim())
                        if (user == null) {
                            errorMessage = "E-Mail nicht registriert!"
                        } else if (user.passwort != password) {
                            errorMessage = "Falsches Passwort!"
                        } else {
                            // Erfolg: Email als stabilen Account-Key setzen
                            repository.setAccountKey(user.email)
                            // Display-Name nur beim ersten Login setzen (Standard = DB-Name)
                            if (repository.ladeSpielerName().isBlank() || repository.ladeSpielerName() == "Spieler") {
                                repository.speichereSpielerName(user.name)
                            }
                            onSignInClicked()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeRunBlue
                )
            ) {
                Text(
                    text = "Anmelden",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row {
                Text(
                    text = "Noch kein Konto? ",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
                Text(
                    text = "Jetzt registrieren",
                    fontSize = 12.sp,
                    color = TeRunBlue,
                    modifier = Modifier.clickable { onRegisterClicked() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignInScreenPreview() {
    SignInScreen(
        repository = SpielRepository(LocalContext.current),
        onSignInClicked = {},
        onRegisterClicked = {}
    )
}