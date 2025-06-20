package com.example.detector

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings

data class Alerta(val mq2: Double, val mq7: Double, val timestamp: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertHistoryScreen(navController: NavController) {
    val db = FirebaseDatabase.getInstance().reference
    val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
    val userKey = email.replace("@", "_at_").replace(".", "_dot_")
    var alertas by remember { mutableStateOf<List<Alerta>>(emptyList()) }

    // Leer desde /alertas/<userKey>
    LaunchedEffect(Unit) {
        val ref = db.child("alertas").child(userKey)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = mutableListOf<Alerta>()
                for (hijo in snapshot.children) {
                    val mq2 = hijo.child("mq2").getValue(Double::class.java) ?: 0.0
                    val mq7 = hijo.child("mq7").getValue(Double::class.java) ?: 0.0
                    val timestamp = hijo.child("timestamp").getValue(Long::class.java) ?: 0L

                    lista.add(Alerta(mq2, mq7, timestamp))
                }
                alertas = lista.sortedByDescending { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Alertas") }
            )
        },
        bottomBar = {
            AlertBottomNavigationBar(navController)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (alertas.isEmpty()) {
                Text("Sin alertas registradas.")
            } else {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                LazyColumn {
                    items(alertas) { alerta ->
                        Text("⚠ MQ-2: ${alerta.mq2.toInt()} ppm | MQ-7: ${alerta.mq7.toInt()} ppm")
                        Text("Fecha: ${sdf.format(Date(alerta.timestamp))}")
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AlertBottomNavigationBar(navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("main") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("history") },
            icon = { Icon(Icons.Default.List, contentDescription = "Historial") },
            label = { Text("Historial") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("settings") },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Configuración") },
            label = { Text("Configuración") }
        )
        NavigationBarItem(
            selected = true,
            onClick = { /* Ya estás en alertas */ },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Alertas") },
            label = { Text("Alertas") }
        )
    }
}
