package com.example.detector

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

@Composable
fun MainScreen(navController: NavController) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val email = auth.currentUser?.email ?: "Desconocido"
    val db = FirebaseDatabase.getInstance().reference
    val userKey = email.replace("@", "_at_").replace(".", "_dot_")

    val mq2Ppm = remember { mutableStateOf(0.0) }
    val mq7Ppm = remember { mutableStateOf(0.0) }
    var umbralMQ2 by remember { mutableStateOf(200) }
    var umbralMQ7 by remember { mutableStateOf(65) }
    var nivelSeleccionado by remember { mutableStateOf("Medio") }

    var lastUpdateTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var sensorInactive by remember { mutableStateOf(false) }
    var notificacionEnviada by remember { mutableStateOf(false) }

    // CANAL DE NOTIFICACIÓN
    LaunchedEffect(Unit) {
        val channelId = "gas_alert_channel"
        val channel = NotificationChannel(
            channelId,
            "Alertas de Gas",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "Canal para alertas de fuga de gas"
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    // CARGA DE UMBRALES
    LaunchedEffect(Unit) {
        val umbralRef = db.child("umbrales").child(userKey)
        umbralRef.child("mq2").get().addOnSuccessListener {
            it.getValue(Int::class.java)?.let { v -> umbralMQ2 = v }
        }
        umbralRef.child("mq7").get().addOnSuccessListener {
            it.getValue(Int::class.java)?.let { v -> umbralMQ7 = v }
        }
        umbralRef.child("nivel").get().addOnSuccessListener {
            it.getValue(String::class.java)?.let { v -> nivelSeleccionado = v }
        }
    }

    // ESCUCHA DE DATOS
    DisposableEffect(Unit) {
        val query = db.child("usuarios").child(userKey).orderByKey().limitToLast(1)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val mq2Value = child.child("mq2_ppm").getValue(Double::class.java)
                        ?: child.child("mq2").getValue(Double::class.java)
                    val mq7Value = child.child("mq7_ppm").getValue(Double::class.java)
                        ?: child.child("mq7").getValue(Double::class.java)

                    if (mq2Value != null && mq7Value != null) {
                        mq2Ppm.value = mq2Value
                        mq7Ppm.value = mq7Value
                        lastUpdateTime = System.currentTimeMillis()
                        sensorInactive = false
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                println("❌ Firebase error: ${error.message}")
            }
        }

        query.addValueEventListener(listener)
        onDispose { query.removeEventListener(listener) }
    }

    // VERIFICAR INACTIVIDAD
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            if (System.currentTimeMillis() - lastUpdateTime > 120_000) {
                sensorInactive = true
            }
        }
    }

    // CÁLCULO DE COLORES
    val mq2Color = when {
        mq2Ppm.value < umbralMQ2 -> Color.Green
        mq2Ppm.value < umbralMQ2 + 200 -> Color(0xFFFFA000)
        else -> Color.Red
    }
    val mq7Color = when {
        mq7Ppm.value < umbralMQ7 -> Color.Green
        mq7Ppm.value < umbralMQ7 + 20 -> Color(0xFFFFA000)
        else -> Color.Red
    }

    // NOTIFICACIÓN Y GUARDAR ALERTA
    if ((mq2Ppm.value > umbralMQ2 || mq7Ppm.value > umbralMQ7) && !notificacionEnviada) {
        val notification = NotificationCompat.Builder(context, "gas_alert_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Fuga de gas detectada")
            .setContentText("Se ha superado el umbral en MQ-2 o MQ-7")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(1001, notification)
        notificacionEnviada = true

        // ✅ GUARDAR ALERTA EN FIREBASE
        val alerta = mapOf(
            "timestamp" to System.currentTimeMillis(),
            "mq2" to mq2Ppm.value,
            "mq7" to mq7Ppm.value
        )
        db.child("alertas").child(userKey).push().setValue(alerta)
    }

    // RESETEO DE NOTIFICACIÓN
    LaunchedEffect(mq2Ppm.value, mq7Ppm.value) {
        if (mq2Ppm.value < umbralMQ2 && mq7Ppm.value < umbralMQ7) {
            delay(5_000)
            if (mq2Ppm.value < umbralMQ2 && mq7Ppm.value < umbralMQ7) {
                notificacionEnviada = false
            }
        }
    }

    // UI PRINCIPAL
    Scaffold(
        bottomBar = {
            MainBottomNavigationBar(current = "main", navController = navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Bienvenido: $email", style = MaterialTheme.typography.titleMedium)
            Text("Nivel de umbral: $nivelSeleccionado", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))

            if (sensorInactive) {
                Text(
                    "⚠️ Sin nuevos datos desde el dispositivo",
                    color = Color(0xFFFFA000),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(16.dp))
            }

            if (mq2Ppm.value > umbralMQ2 || mq7Ppm.value > umbralMQ7) {
                Text(
                    "⚠️ ¡Peligro de gas detectado!",
                    color = Color.Red,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(16.dp))
            }

            Gauge(value = mq2Ppm.value.toInt(), label = "MQ-2 (ppm)", color = mq2Color)
            Spacer(Modifier.height(24.dp))
            Gauge(value = mq7Ppm.value.toInt(), label = "MQ-7 (ppm)", color = mq7Color)
        }
    }
}

/* ---------- NAVIGATION BAR ---------- */
@Composable
fun MainBottomNavigationBar(current: String, navController: NavController) {
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
