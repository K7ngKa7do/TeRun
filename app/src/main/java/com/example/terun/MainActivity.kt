package com.example.terun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.terun.ui.theme.TeRunTheme
import kotlinx.serialization.Serializable

@Serializable object LoginRoute
@Serializable object SignInRoute
@Serializable object RegisterRoute
@Serializable object HomeRoute

/**
 * MainActivity — Haupteinstiegspunkt der Android-App (Single Activity-Muster).
 * Hier wird das OSMDroid Karten-Repository konfiguriert und das Theme geladen.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // OSMDroid Karten-Konfiguration (Vermeidet Kachel-Ladefehler durch User-Agent Definition)
        org.osmdroid.config.Configuration.getInstance().userAgentValue = "com.example.terun"
        
        // Ränder bis zum Bildschirmrand ausnutzen (Edge-to-Edge)
        enableEdgeToEdge()
        
        // Compose-Layout mit benutzerdefiniertem TeRun-Design rendern
        setContent { TeRunTheme { TeRunApp() } }
    }
}

/**
 * TeRunApp — Zentraler Navigations-Graph der Anwendung.
 * Verwaltet alle Routen und stellt sicher, dass das SpielRepository als Singleton-Instanz geteilt wird.
 */
@Composable
fun TeRunApp() {
    val context = LocalContext.current
    // Repository wird hier deklariert und an alle Screens übergeben
    val repository = remember { SpielRepository(context) }
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = LoginRoute) {

        // 1. Willkommensscreen (Login / Register Auswahl)
        composable<LoginRoute> {
            LoginScreen(
                onSignInClicked = { navController.navigate(SignInRoute) },
                onRegisterClicked = { navController.navigate(RegisterRoute) }
            )
        }

        // 2. Anmelde-Screen (SignIn)
        composable<SignInRoute> {
            SignInScreen(
                repository = repository,
                onSignInClicked = { navController.navigate(HomeRoute) },
                onRegisterClicked = { navController.navigate(RegisterRoute) }
            )
        }

        // 3. Registrierungs-Screen (Register)
        composable<RegisterRoute> {
            RegisterScreen(
                repository = repository,
                onRegisterClicked = { navController.navigate(HomeRoute) },
                onSignInClicked = { navController.navigate(SignInRoute) }
            )
        }

        // 4. Hauptbildschirm (Karte & Spiel-Steuerung)
        composable<HomeRoute> {
            KarteScreen(onLogout = {
                // Bei Logout wird der gesamte Backstack gelöscht, um unbefugten Rückweg zu sperren
                navController.navigate(LoginRoute) {
                    popUpTo(HomeRoute) { inclusive = true }
                }
            })
        }
    }
}