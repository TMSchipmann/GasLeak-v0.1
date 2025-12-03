package com.example.detector

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val configManager = remember { UserConfigManager(context) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Iniciar Sesión", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = icon, contentDescription = "Ver contraseña")
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(context, "Correo no válido", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            scope.launch {
                                if (configManager.isConfigured()) {
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("bluetoothConfig") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            }
                        } else {
                            val errorMsg = when (task.exception?.message) {
                                "There is no user record corresponding to this identifier. The user may have been deleted." ->
                                    "El usuario no existe"
                                "The password is invalid or the user does not have a password." ->
                                    "Contraseña incorrecta"
                                else -> "Credenciales inválidas"
                            }
                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ingresando...")
            } else {
                Text("Iniciar sesión")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = {
            navController.navigate("register")
        }) {
            Text("¿No tienes cuenta? Crear una")
        }
        TextButton(
            onClick = { navController.navigate("resetPassword") }
        ) {
            Text("¿Olvidaste tu contraseña?")
        }

    }
}
