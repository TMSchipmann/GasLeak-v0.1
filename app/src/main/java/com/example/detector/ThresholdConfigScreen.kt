package com.example.detector

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThresholdConfigScreen(navController: NavController) {
    val db = FirebaseDatabase.getInstance().reference
    val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
    val userKey = email.replace("@", "_at_").replace(".", "_dot_")

    val niveles = listOf("Bajo", "Medio", "Alto")
    var seleccionado by remember { mutableStateOf("Medio") }
    var showMessage by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val umbralesPredefinidos = mapOf(
        "Bajo" to Pair(120, 25),
        "Medio" to Pair(200, 40),
        "Alto" to Pair(500, 60)
    )

    // Precarga el nivel actual desde Firebase
    LaunchedEffect(Unit) {
        val nivelRef = db.child("umbrales").child(userKey).child("nivel")
        nivelRef.get().addOnSuccessListener { snapshot ->
            val nivelGuardado = snapshot.getValue(String::class.java)
            if (nivelGuardado in niveles) {
                seleccionado = nivelGuardado!!
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administrar umbrales") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Selecciona un nivel de umbral:", style = MaterialTheme.typography.titleMedium)

            Button(onClick = { menuExpanded = true }) {
                Text(seleccionado)
            }

            if (menuExpanded) {
                AlertDialog(
                    onDismissRequest = { menuExpanded = false },
                    confirmButton = {},
                    title = { Text("Selecciona un nivel de umbral") },
                    text = {
                        Column {
                            niveles.forEach { nivel ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            seleccionado = nivel
                                            menuExpanded = false
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    RadioButton(
                                        selected = seleccionado == nivel,
                                        onClick = {
                                            seleccionado = nivel
                                            menuExpanded = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(nivel)
                                }
                            }
                        }
                    }
                )
            }

            Button(
                onClick = {
                    val (mq2, mq7) = umbralesPredefinidos[seleccionado]!!
                    db.child("umbrales").child(userKey).apply {
                        child("mq2").setValue(mq2)
                        child("mq7").setValue(mq7)
                        child("nivel").setValue(seleccionado)
                    }
                    showMessage = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar umbrales")
            }

            if (showMessage) {
                Text("Umbrales guardados correctamente.", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
