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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        org.osmdroid.config.Configuration.getInstance().userAgentValue = "com.example.terun"
        enableEdgeToEdge()
        setContent { TeRunTheme { TeRunApp() } }
    }
}

@Composable
fun TeRunApp() {
    val context = LocalContext.current
    val repository = remember { SpielRepository(context) }
    LaunchedEffect(Unit) { repository.prepopulateBenutzer() }

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = LoginRoute) {

        composable<LoginRoute> {
            LoginScreen(
                onSignInClicked = { navController.navigate(SignInRoute) },
                onRegisterClicked = { navController.navigate(RegisterRoute) }
            )
        }

        composable<SignInRoute> {
            SignInScreen(
                repository = repository,
                onSignInClicked = { navController.navigate(HomeRoute) },
                onRegisterClicked = { navController.navigate(RegisterRoute) }
            )
        }

        composable<RegisterRoute> {
            RegisterScreen(
                repository = repository,
                onRegisterClicked = { navController.navigate(HomeRoute) },
                onSignInClicked = { navController.navigate(SignInRoute) }
            )
        }

        composable<HomeRoute> {
            KarteScreen(onLogout = {
                navController.navigate(LoginRoute) { popUpTo(HomeRoute) { inclusive = true } }
            })
        }
    }
}