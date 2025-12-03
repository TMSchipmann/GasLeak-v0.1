package com.example.detector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.detector.ui.theme.DetectorTheme
import com.google.firebase.auth.FirebaseAuth


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DetectorApp()
        }
    }
}

@Composable
fun DetectorApp() {
    val navController = rememberNavController()

    DetectorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NavHost(navController = navController, startDestination = "splash") {
                composable("splash") {
                    SplashScreen(navController)
                }
                composable("login") {
                    LoginScreen(navController)
                }
                composable("register") {
                    RegisterScreen(navController)
                }
                composable("bluetoothConfig") {
                    BluetoothBleScreen(navController)
                }
                composable("main") {
                    MainScreen(navController)
                }
                composable("history") {
                    HistoryScreen(navController)
                }
                composable("settings") {
                    SettingsScreen(navController)
                }
                composable("threshold_config") {
                    ThresholdConfigScreen(navController)
                }
                composable("alertHistory") {
                    AlertHistoryScreen(navController)
                }
                composable("resetPassword") {
                    ResetPasswordScreen(navController, FirebaseAuth.getInstance())
                }

            }
        }
    }
}
