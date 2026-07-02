// Datei: RegisterScreen.kt
// Paket: com.example.terun

package com.example.terun

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    repository: SpielRepository,
    onRegisterClicked: () -> Unit,
    onSignInClicked: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val spielernameFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val passwordWiederholenFocusRequester = remember { FocusRequester() }

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
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TeRunLogo(size = 100.dp)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "TeRun",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Konto erstellen",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(20.dp))

            var spielername by remember { mutableStateOf("") }
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var passwordWiederholen by remember { mutableStateOf("") }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            // Spielername Eingabefeld
            OutlinedTextField(
                value = spielername,
                onValueChange = { 
                    spielername = it
                    errorMessage = null 
                },
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { emailFocusRequester.requestFocus() }),
                modifier = Modifier.fillMaxWidth().focusRequester(spielernameFocusRequester),
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { passwordWiederholenFocusRequester.requestFocus() }),
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

            Spacer(modifier = Modifier.height(10.dp))

            // Passwort wiederholen Eingabefeld
            OutlinedTextField(
                value = passwordWiederholen,
                onValueChange = { 
                    passwordWiederholen = it
                    errorMessage = null 
                },
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth().focusRequester(passwordWiederholenFocusRequester),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Konto erstellen Button
            Button(
                onClick = {
                    errorMessage = null
                    if (spielername.isBlank() || email.isBlank() || password.isBlank() || passwordWiederholen.isBlank()) {
                        errorMessage = "Bitte alle Felder ausfüllen!"
                        return@Button
                    }
                    if (!isValidEmail(email.trim())) {
                        errorMessage = "Bitte eine gültige E-Mail-Adresse eingeben!"
                        return@Button
                    }
                    if (!isValidPassword(password)) {
                        errorMessage = "Passwort-Sicherheit unzureichend!\n" +
                                "Erforderlich: Mindestens 8 Zeichen, 1 Großbuchstabe, 1 Kleinbuchstabe, 1 Ziffer und 1 Sonderzeichen."
                        return@Button
                    }
                    if (password != passwordWiederholen) {
                        errorMessage = "Die Passwörter stimmen nicht überein!"
                        return@Button
                    }
                    coroutineScope.launch {
                        val existing = repository.holeBenutzer(email.trim())
                        if (existing != null) {
                            errorMessage = "E-Mail ist bereits registriert!"
                        } else {
                            // Erfolg: In Room speichern
                            val newUser = BenutzerEntity(
                                email = email.trim(),
                                name = spielername.trim(),
                                passwort = password
                            )
                            repository.speichereBenutzer(newUser)
                            // Account-Key setzen und Display-Name initialisieren
                            repository.setAccountKey(newUser.email)
                            repository.speichereSpielerName(newUser.name)
                            Toast.makeText(context, "Konto erfolgreich erstellt!", Toast.LENGTH_SHORT).show()
                            onRegisterClicked()
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
                    text = "Konto erstellen",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

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

// Hilfsfunktionen für E-Mail- & Passwort-Validierung nach ISO/NIST Standard
fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    return email.matches(emailRegex)
}

fun isValidPassword(password: String): Boolean {
    // Mindestens 8 Zeichen, 1 Ziffer, 1 Kleinbuchstabe, 1 Großbuchstabe, 1 Sonderzeichen
    val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_\\-*~?§()|/:,.;]).{8,}$".toRegex()
    return password.matches(passwordRegex)
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen(
        repository = SpielRepository(LocalContext.current),
        onRegisterClicked = {},
        onSignInClicked = {}
    )
}
