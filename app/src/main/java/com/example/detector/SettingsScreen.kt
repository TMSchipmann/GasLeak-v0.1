package com.example.detector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            SettingsBottomNavigationBar(current = "settings", navController = navController)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("threshold_config") }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Administrar umbrales", style = MaterialTheme.typography.titleMedium)
                    Text("Establecer niveles de alerta para MQ-2 y MQ-7", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("bluetoothConfig") }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Configurar ESP32 por BLE", style = MaterialTheme.typography.titleMedium)
                    Text("Reenviar red Wi-Fi y correo al dispositivo", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cerrar sesión", style = MaterialTheme.typography.titleMedium)
                    Text("Cerrar sesión y volver a la pantalla de inicio", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun SettingsBottomNavigationBar(current: String, navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            selected = current == "main",
            onClick = { navController.navigate("main") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") }
        )
        NavigationBarItem(
            selected = current == "history",
            onClick = { navController.navigate("history") },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Historial") },
            label = { Text("Historial") }
        )
        NavigationBarItem(
            selected = current == "settings",
            onClick = { /* Ya estás aquí */ },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Configuración") },
            label = { Text("Configuración") }
        )
        NavigationBarItem(
            selected = current == "alertHistory",
            onClick = { navController.navigate("alertHistory") },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Alertas") },
            label = { Text("Alertas") }
        )
    }
}
