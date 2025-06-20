package com.example.detector

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import androidx.compose.ui.Alignment

data class Reading(val mq2: Int, val mq7: Int, val timestamp: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val db = FirebaseDatabase.getInstance().reference
    val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
    val userKey = userEmail.replace("@", "_at_").replace(".", "_dot_")

    var readings by remember { mutableStateOf<List<Reading>>(emptyList()) }
    var filter by remember { mutableStateOf("ambos") }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val userRef = db.child("usuarios").child(userKey)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newList = mutableListOf<Reading>()
                val seenTimestamps = mutableSetOf<Long>()

                for (entry in snapshot.children) {
                    val mq2 = entry.child("mq2_ppm").getValue(Double::class.java)?.toInt()
                    val mq7 = entry.child("mq7_ppm").getValue(Double::class.java)?.toInt()
                    val timestampRaw = entry.child("timestamp").getValue(Long::class.java)
                    val timestamp = timestampRaw ?: System.currentTimeMillis()

                    if (mq2 != null && mq7 != null && timestamp !in seenTimestamps) {
                        newList.add(Reading(mq2, mq7, timestamp))
                        seenTimestamps.add(timestamp)
                    }
                }

                readings = newList.sortedByDescending { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate("main") {
                            popUpTo("history") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtro")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("MQ-2") },
                            onClick = {
                                filter = "mq2"
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("MQ-7") },
                            onClick = {
                                filter = "mq7"
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ambos") },
                            onClick = {
                                filter = "ambos"
                                expanded = false
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            HistoryBottomNavigationBar(current = "history", navController = navController)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (readings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay datos disponibles", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn {
                    items(readings) { reading ->
                        if (filter == "mq2" || filter == "ambos") {
                            Text("MQ-2: ${reading.mq2} ppm", style = MaterialTheme.typography.bodyLarge)
                        }
                        if (filter == "mq7" || filter == "ambos") {
                            Text("MQ-7: ${reading.mq7} ppm", style = MaterialTheme.typography.bodyLarge)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryBottomNavigationBar(current: String, navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            selected = current == "main",
            onClick = { navController.navigate("main") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") }
        )
        NavigationBarItem(
            selected = current == "history",
            onClick = { /* Ya estás aquí */ },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Historial") },
            label = { Text("Historial") }
        )
        NavigationBarItem(
            selected = current == "settings",
            onClick = { navController.navigate("settings") },
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
