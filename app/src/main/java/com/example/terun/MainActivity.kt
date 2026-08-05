package com.example.terun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.terun.ui.theme.TeRunTheme
import kotlinx.serialization.Serializable

/**
 * =====================================================================
 * Routen-Definitionen für den Navigations-Graphen
 * =====================================================================
 *
 * VORLESUNG 16 – Navigation (Jetpack Compose Navigation):
 * Jede Route ist ein Kotlin-Objekt mit @Serializable, damit die Navigation
 * typsicher ist. Das bedeutet: der Compiler erkennt Fehler in den Routen
 * schon beim Übersetzen des Codes, nicht erst zur Laufzeit.
 *
 * Alternative zu einfachen Strings ("login", "home"): Strings sind fehleranfällig,
 * da Tippfehler nicht auffallen. Objekte mit @Serializable sind sicherer.
 */
@Serializable object LoginRoute    // Willkommensscreen (Anmelden oder Registrieren)
@Serializable object SignInRoute   // Anmeldescreen (E-Mail + Passwort eingeben)
@Serializable object RegisterRoute // Registrierungsscreen (neues Konto erstellen)
@Serializable object HomeRoute     // Hauptscreen (Karte, Duelle, Profil)

/**
 * =====================================================================
 * MainActivity – Einziger Einstiegspunkt der App (Single Activity Pattern)
 * =====================================================================
 *
 * VORLESUNG 8 – App-Komponenten (Activities):
 * Eine Activity ist die grundlegende UI-Komponente in Android.
 * Das "Single Activity Pattern" bedeutet: die gesamte App hat nur EINE Activity.
 * Alle Screens werden als Composables in diese eine Activity gerendert.
 * Die Navigation zwischen Screens übernimmt der NavController.
 *
 * ComponentActivity:
 * - Basis-Klasse aus Jetpack, unterstützt Compose
 * - onCreate() ist die erste Lifecycle-Methode die aufgerufen wird
 *
 * enableEdgeToEdge():
 * - Lässt die App bis zum Bildschirmrand (Statusleiste/Navigationsleiste) zeichnen
 * - Modernerer Look (kein dunkler Balken oben/unten)
 *
 * setContent { }:
 * - Hier wird der Compose-UI-Baum übergeben (ersetzt das XML-Layout)
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ränder bis zum Bildschirmrand nutzen (Edge-to-Edge Design)
        enableEdgeToEdge()

        // Compose-UI starten mit dem TeRun-Theme (Farben, Typografie)
        setContent {
            TeRunTheme {
                TeRunApp() // Einstiegspunkt der gesamten Compose-UI
            }
        }
    }
}

/**
 * =====================================================================
 * TeRunApp – Zentraler Navigations-Graph der Anwendung
 * =====================================================================
 *
 * VORLESUNG 16 – Navigation:
 * NavHost = der Container der alle Screens verwaltet
 * NavController = steuert die Navigation (vorwärts/rückwärts)
 *
 * Ablauf:
 * 1. Benutzer öffnet App → LoginRoute (Willkommensscreen)
 * 2. Tippen auf "Anmelden" → SignInRoute
 * 3. Tippen auf "Registrieren" → RegisterRoute
 * 4. Nach erfolgreicher Anmeldung → HomeRoute (Hauptscreen)
 *
 * remember { SpielRepository(context) }:
 * Das Repository wird einmal erstellt und für alle Screens geteilt.
 * 'remember' sorgt dafür, dass es bei Recompositions nicht neu erstellt wird.
 */
@Composable
fun TeRunApp() {
    val context = LocalContext.current

    // Repository einmal erstellen und für alle Screens teilen (Singleton in Compose)
    val repository = remember { SpielRepository(context) }

    // NavController steuert die Navigation zwischen den Screens
    val navController = rememberNavController()

    // NavHost definiert alle verfügbaren Routen (Screens) der App
    NavHost(navController = navController, startDestination = LoginRoute) {

        // Screen 1: Willkommensscreen (Login oder Register auswählen)
        composable<LoginRoute> {
            LoginScreen(
                onSignInClicked = { navController.navigate(SignInRoute) },
                onRegisterClicked = { navController.navigate(RegisterRoute) }
            )
        }

        // Screen 2: Anmeldescreen (E-Mail + Passwort)
        composable<SignInRoute> {
            SignInScreen(
                repository = repository,
                onSignInClicked = { navController.navigate(HomeRoute) },
                onRegisterClicked = { navController.navigate(RegisterRoute) }
            )
        }

        // Screen 3: Registrierungsscreen (neues Konto erstellen)
        composable<RegisterRoute> {
            RegisterScreen(
                repository = repository,
                onRegisterClicked = { navController.navigate(HomeRoute) },
                onSignInClicked = { navController.navigate(SignInRoute) }
            )
        }

        // Screen 4: Hauptscreen (Karte, Duelle, Profil)
        composable<HomeRoute> {
            KarteScreen(
                onLogout = {
                    // Beim Logout: kompletten Backstack löschen (HomeRoute inkl.)
                    // Verhindert, dass der Benutzer mit "Zurück" wieder auf den Homescreen kommt
                    navController.navigate(LoginRoute) {
                        popUpTo(HomeRoute) { inclusive = true }
                    }
                }
            )
        }
    }
}