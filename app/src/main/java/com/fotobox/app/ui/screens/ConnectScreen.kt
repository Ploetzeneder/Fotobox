package com.fotobox.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fotobox.app.ui.viewmodels.ConnectViewModel

@Composable
fun ConnectScreen(
    onBack: () -> Unit,
    viewModel: ConnectViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showPassword by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0520), Color(0xFF150A2A))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Zurück", tint = Color.White)
                }
                Text(
                    "Fotobienchen verbinden",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White, fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (state.isConnected) {
                ConnectedCard(
                    customerName = state.customerName,
                    boxId = state.boxId,
                    onDisconnect = { viewModel.disconnect() }
                )
            } else {
                LoginCard(
                    apiUrl = state.apiUrl,
                    email = state.email,
                    password = state.password,
                    showPassword = showPassword,
                    isLoading = state.isLoading,
                    error = state.error,
                    onUrlChange = { viewModel.setUrl(it) },
                    onEmailChange = { viewModel.setEmail(it) },
                    onPasswordChange = { viewModel.setPassword(it) },
                    onTogglePassword = { showPassword = !showPassword },
                    onConnect = { viewModel.connect() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(0.06f))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Was wird synchronisiert?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White, fontWeight = FontWeight.Bold
                        )
                    )
                    listOf(
                        "✓  Event-Einstellungen (Name, Layout, Filter)",
                        "✓  Fotos & Streifen → Cloud-Galerie",
                        "✓  Audio-Gästebuch-Aufnahmen",
                        "✓  Gäste können Fotos online ansehen",
                    ).forEach { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White.copy(0.7f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedCard(
    customerName: String,
    boxId: String,
    onDisconnect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1B5E20).copy(0.5f))
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Default.CheckCircle,
                null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Verbunden!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold
                )
            )
            Text(
                customerName,
                style = MaterialTheme.typography.titleLarge.copy(color = Color.White)
            )
            Text(
                "Box-ID: $boxId",
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(0.5f))
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Trennen", color = Color.White.copy(0.7f))
            }
        }
    }
}

@Composable
private fun LoginCard(
    apiUrl: String,
    email: String,
    password: String,
    showPassword: Boolean,
    isLoading: Boolean,
    error: String?,
    onUrlChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onConnect: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(0.07f))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Cloud, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                "Mit Fotobienchen-Account anmelden",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White, fontWeight = FontWeight.Bold
                )
            )
        }

        OutlinedTextField(
            value = apiUrl,
            onValueChange = onUrlChange,
            label = { Text("API-URL", color = Color.White.copy(0.6f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("E-Mail", color = Color.White.copy(0.6f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Passwort", color = Color.White.copy(0.6f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null, tint = Color.White.copy(0.5f)
                    )
                }
            }
        )

        AnimatedVisibility(error != null) {
            Text(
                error ?: "",
                style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFFEF5350)),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = onConnect,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
            } else {
                Icon(Icons.Default.Cloud, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Verbinden", style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp))
            }
        }
    }
}
