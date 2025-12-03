package com.example.detector

import android.content.Intent
import android.graphics.Color
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

data class Reading(
    val mq2: Int,
    val mq7: Int,
    val timestamp: Long,
    val fecha: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val db = FirebaseDatabase.getInstance().reference
    val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
    val userKey = userEmail.replace("@", "_at_").replace(".", "_dot_")

    var readings by remember { mutableStateOf<List<Reading>>(emptyList()) }
    var filter by remember { mutableStateOf("ambos") }
    var expanded by remember { mutableStateOf(false) }
    var showGraph by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // 🔹 Cargar lecturas desde Firebase
    LaunchedEffect(Unit) {
        val userRef = db.child("usuarios").child(userKey)
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newList = mutableListOf<Reading>()
                val seenTimestamps = mutableSetOf<Long>()

                for (entry in snapshot.children) {
                    val mq2 = entry.child("mq2_ppm").getValue(Double::class.java)?.toInt()
                    val mq7 = entry.child("mq7_ppm").getValue(Double::class.java)?.toInt()
                    val fechaStr = entry.child("fecha").getValue(String::class.java) ?: ""
                    val timestampRaw = entry.child("timestamp").getValue(Long::class.java)
                    val timestamp = timestampRaw ?: System.currentTimeMillis()

                    if (mq2 != null && mq7 != null && timestamp !in seenTimestamps) {
                        val fecha = when {
                            fechaStr.isNotBlank() -> fechaStr
                            timestamp > 1000000000000L -> sdf.format(Date(timestamp))
                            else -> "No sincronizado"
                        }

                        newList.add(Reading(mq2, mq7, timestamp, fecha))
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
                    // 🔹 Botón de gráfica a la izquierda del filtro
                    IconButton(onClick = { showGraph = !showGraph }) {
                        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Mostrar gráfico")
                    }
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay datos disponibles", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                // 🔹 Mostrar gráfico si el usuario lo activó
                if (showGraph) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Evolución de concentraciones de gas",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    AndroidView(
                        factory = { context ->
                            LineChart(context).apply {
                                val entriesMq2 = readings.mapIndexed { index, r ->
                                    Entry(index.toFloat(), r.mq2.toFloat())
                                }
                                val entriesMq7 = readings.mapIndexed { index, r ->
                                    Entry(index.toFloat(), r.mq7.toFloat())
                                }

                                val dataSetMq2 = LineDataSet(entriesMq2, "MQ-2 (ppm)").apply {
                                    color = Color.BLUE
                                    setCircleColor(Color.BLUE)
                                    lineWidth = 2f
                                }
                                val dataSetMq7 = LineDataSet(entriesMq7, "MQ-7 (ppm)").apply {
                                    color = Color.RED
                                    setCircleColor(Color.RED)
                                    lineWidth = 2f
                                }

                                val dataSets = ArrayList<ILineDataSet>().apply {
                                    add(dataSetMq2)
                                    add(dataSetMq7)
                                }

                                data = LineData(dataSets)
                                description = Description().apply { text = "Lecturas históricas" }
                                axisRight.isEnabled = false
                                invalidate()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .padding(8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // 🔹 Botón para exportar PDF
                Button(
                    onClick = {
                        val historico = readings.map {
                            HistoricalData(
                                fecha = it.fecha,
                                mq2 = it.mq2.toDouble(),
                                mq7 = it.mq7.toDouble(),
                                usuario = userEmail
                                    .replace("_at_", "@")
                                    .replace("_dot_", ".")
                            )
                        }

                        val alertRef = db.child("alertas").child(userKey)
                        val sdfAlert = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                        alertRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val alertList = mutableListOf<AlertData>()
                                if (!snapshot.exists()) {
                                    Toast.makeText(context, "No hay alertas registradas", Toast.LENGTH_SHORT).show()
                                }

                                for (alert in snapshot.children) {
                                    val mq2 = alert.child("mq2").getValue(Double::class.java)
                                    val mq7 = alert.child("mq7").getValue(Double::class.java)
                                    val timestamp = alert.child("timestamp").getValue(Long::class.java)
                                        ?: System.currentTimeMillis()
                                    val fechaAlerta = if (timestamp > 1000000000000L)
                                        sdfAlert.format(Date(timestamp)) else "No sincronizado"

                                    if (mq2 != null) {
                                        alertList.add(AlertData(fecha = fechaAlerta, tipo = "MQ-2", valor = mq2))
                                    }
                                    if (mq7 != null) {
                                        alertList.add(AlertData(fecha = fechaAlerta, tipo = "MQ-7", valor = mq7))
                                    }
                                }

                                val file = PdfGenerator.generateHistoryPdf(context, historico, alertList)
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    val chooser = Intent.createChooser(intent, "Abrir PDF con...")
                                    context.startActivity(chooser)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "No se encontró ninguna app para abrir PDF", Toast.LENGTH_LONG).show()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(context, "Error al cargar alertas", Toast.LENGTH_SHORT).show()
                            }
                        })
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📄 Exportar PDF")
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(readings) { reading ->
                        Text("📅 ${reading.fecha}", style = MaterialTheme.typography.bodyMedium)
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
