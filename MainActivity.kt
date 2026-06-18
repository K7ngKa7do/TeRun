// Datei: MainActivity.kt
// Paket: com.example.terun

package com.example.terun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.terun.ui.theme.TeRunTheme
import kotlinx.serialization.Serializable

// Routen — jede Route ist ein eigener Screen
// Quelle: moco202616navigation.pdf — @Serializable Routen
@Serializable object LoginRoute
@Serializable object SignInRoute
@Serializable object RegisterRoute
@Serializable object HomeRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // setContent startet die Compose UI
        // Quelle: moco202608appcomponents.pdf — Activity
        setContent {
            TeRunTheme {
                TeRunApp()
            }
        }
    }
}

// Haupt-Composable mit Navigation
// Quelle: moco202616navigation.pdf — NavController, NavHost
@Composable
fun TeRunApp() {

    // NavController merkt sich welcher Screen gerade aktiv ist
    // Quelle: moco202616navigation.pdf — rememberNavController
    val navController = rememberNavController()

    // NavHost definiert alle Screens und ihre Routen
    // Quelle: moco202616navigation.pdf — NavHost, composable
    NavHost(
        navController = navController,
        startDestination = LoginRoute
    ) {

        // LoginScreen — Startscreen
        composable<LoginRoute> {
            LoginScreen(
                onSignInClicked = {
                    navController.navigate(SignInRoute)
                },
                onRegisterClicked = {
                    navController.navigate(RegisterRoute)
                }
            )
        }

        // SignInScreen — Anmelden
        composable<SignInRoute> {
            SignInScreen(
                onSignInClicked = {
                    navController.navigate(HomeRoute)
                },
                onRegisterClicked = {
                    navController.navigate(RegisterRoute)
                }
            )
        }

        // RegisterScreen — Registrieren
        composable<RegisterRoute> {
            RegisterScreen(
                onRegisterClicked = {
                    navController.navigate(HomeRoute)
                },
                onSignInClicked = {
                    navController.navigate(SignInRoute)
                }
            )
        }

        // HomeScreen — Hauptscreen mit Karte
        composable<HomeRoute> {
            KarteScreen()
        }
    }
}