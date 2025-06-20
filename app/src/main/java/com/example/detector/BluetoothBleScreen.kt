package com.example.detector

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothBleScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var ssid by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var firebasePass by remember { mutableStateOf("") }

    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val email = firebaseUser?.email ?: ""

    var status by remember { mutableStateOf("Selecciona un dispositivo BLE") }
    var connectionStatus by remember { mutableStateOf("") }
    var scanResults by remember { mutableStateOf(listOf<ScanResult>()) }
    val seenAddresses = remember { mutableStateListOf<String>() }
    var showDialog by remember { mutableStateOf(false) }
    var selectedDeviceName by remember { mutableStateOf("") }

    var selectedGatt by remember { mutableStateOf<BluetoothGatt?>(null) }
    var isReadyToSend by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    var ssidChar by remember { mutableStateOf<BluetoothGattCharacteristic?>(null) }
    var passChar by remember { mutableStateOf<BluetoothGattCharacteristic?>(null) }
    var emailChar by remember { mutableStateOf<BluetoothGattCharacteristic?>(null) }
    var passFbChar by remember { mutableStateOf<BluetoothGattCharacteristic?>(null) }

    val serviceUUID = UUID.fromString("0000180C-0000-1000-8000-00805f9b34fb")
    val ssidUUID = UUID.fromString("00002A56-0000-1000-8000-00805f9b34fb")
    val passUUID = UUID.fromString("00002A57-0000-1000-8000-00805f9b34fb")
    val emailUUID = UUID.fromString("00002A58-0000-1000-8000-00805f9b34fb")
    val passFbUUID = UUID.fromString("00002A59-0000-1000-8000-00805f9b34fb")

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        permissionLauncher.launch(permissions)

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(context, "Activa la ubicación (GPS)", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        status = "🔄 Conectando a ${device.name ?: "dispositivo"}..."
        connectionStatus = "🔄 Conectando..."
        isReadyToSend = false
        selectedGatt = null

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, statusInt: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    status = "✅ Conectado a ${device.name}"
                    connectionStatus = "✅ Conectado exitosamente"
                    selectedGatt = gatt
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    status = "🔌 Dispositivo desconectado"
                    isReadyToSend = false
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, statusInt: Int) {
                val service = gatt.getService(serviceUUID) ?: return

                ssidChar = service.getCharacteristic(ssidUUID)
                passChar = service.getCharacteristic(passUUID)
                emailChar = service.getCharacteristic(emailUUID)
                passFbChar = service.getCharacteristic(passFbUUID)

                isReadyToSend = ssidChar != null && passChar != null && emailChar != null && passFbChar != null
                connectionStatus = if (isReadyToSend)
                    "✅ Características listas. Puedes enviar datos."
                else
                    "❌ No se encontraron características válidas."
            }
        }

        device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    fun sendDataToEsp32() {
        val gatt = selectedGatt ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            connectionStatus = "❌ Permiso BLE denegado"
            return
        }

        try {
            isSending = true
            ssidChar?.value = ssid.toByteArray()
            gatt.writeCharacteristic(ssidChar)
            Thread.sleep(200)

            passChar?.value = pass.toByteArray()
            gatt.writeCharacteristic(passChar)
            Thread.sleep(200)

            emailChar?.value = email.toByteArray()
            gatt.writeCharacteristic(emailChar)
            Thread.sleep(200)

            passFbChar?.value = firebasePass.toByteArray()
            gatt.writeCharacteristic(passFbChar)

            connectionStatus = "📤 Datos enviados"

            scope.launch {
                // ✅ Guardar flag de configuración exitosa
                val configManager = UserConfigManager(context)
                configManager.markAsConfigured()

                delay(1000)
                gatt.disconnect()
                gatt.close()
                selectedGatt = null
                isReadyToSend = false
                isSending = false

                navController.navigate("main") {
                    popUpTo("bluetoothConfig") { inclusive = true }
                }
            }
        } catch (e: Exception) {
            connectionStatus = "❌ Error al enviar: ${e.localizedMessage}"
            Log.e("BLE_SEND", "Error: ", e)
            isSending = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        seenAddresses.clear()
        scanResults = emptyList()
        showDialog = true

        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val scanner = adapter.bluetoothLeScanner

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val address = result.device.address
                val name = result.device.name
                if (name != null && !seenAddresses.contains(address)) {
                    seenAddresses.add(address)
                    scanResults = scanResults + result
                }
            }
        }

        scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
        scope.launch {
            delay(8000)
            scanner.stopScan(callback)
        }
    }

    fun getCurrentSsid(): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        return info.ssid.replace("\"", "")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración BLE") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = ssid,
                onValueChange = { ssid = it },
                label = { Text("SSID Wi-Fi") },
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            TextButton(onClick = { ssid = getCurrentSsid() }) {
                Text("Agregar automáticamente")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Contraseña Wi-Fi") },
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = firebasePass,
                onValueChange = { firebasePass = it },
                label = { Text("Contraseña Firebase") },
                modifier = Modifier.fillMaxWidth(0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { startScan() }, modifier = Modifier.fillMaxWidth()) {
                Text("Buscar dispositivos BLE")
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedDeviceName.isNotEmpty()) {
                Text("📡 Dispositivo seleccionado: $selectedDeviceName")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(status)
            Text(connectionStatus)

            val fieldsAreValid = ssid.isNotBlank() && pass.isNotBlank() && firebasePass.isNotBlank()
            Button(
                onClick = {
                    if (fieldsAreValid && selectedGatt != null && isReadyToSend) {
                        sendDataToEsp32()
                    } else {
                        Toast.makeText(context, "Completa todos los campos y selecciona un dispositivo", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSending && fieldsAreValid && selectedGatt != null && isReadyToSend
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviando...")
                } else {
                    Text("Enviar datos")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo("bluetoothConfig") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar sesión")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Dispositivos BLE encontrados") },
            text = {
                LazyColumn {
                    items(scanResults) { result ->
                        val name = result.device.name ?: return@items
                        Text(
                            text = "$name - ${result.device.address}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    connectToDevice(result.device)
                                    selectedDeviceName = name
                                    showDialog = false
                                }
                                .padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
