package com.fotobox.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.fotobox.app.network.DalleClient
import com.fotobox.app.network.SyncStatus
import com.fotobox.app.ui.viewmodels.HomeViewModel

@Composable
fun HomeScreen(
    onStartCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartSlideShow: () -> Unit,
    onOpenAudioGuestbook: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val sessionCount by viewModel.sessionCount.collectAsState()
    val audioCount by viewModel.audioCount.collectAsState()
    val eventName by viewModel.eventName.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val kioskMode by viewModel.kioskMode.collectAsState()
    val settingsPin by viewModel.settingsPin.collectAsState()
    val context = LocalContext.current

    var showPinDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "rotate"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse"
    )

    val aiBackground = DalleClient.cacheFile(context)

    Box(modifier = Modifier.fillMaxSize()) {

        if (aiBackground.exists()) {
            AsyncImage(
                model = aiBackground,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            AnimatedBackground(rotation = rotation)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )

        if (syncStatus != SyncStatus.OFFLINE) {
            CloudStatusBadge(
                status = syncStatus,
                modifier = Modifier.align(Alignment.TopStart).padding(20.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NavButton(icon = Icons.Default.Collections,
                label = if (sessionCount > 0) "$sessionCount Fotos" else "Galerie",
                onClick = onOpenGallery)
            NavButton(icon = Icons.Default.Slideshow, label = "Slideshow", onClick = onStartSlideShow)
            NavButton(
                icon = Icons.Default.Mic,
                label = if (audioCount > 0) "$audioCount Stimmen" else "Gästebuch",
                highlight = audioCount > 0,
                onClick = onOpenAudioGuestbook
            )
            NavButton(icon = Icons.Default.Settings, label = "Einstellungen", onClick = {
                if (kioskMode && settingsPin.isNotEmpty()) showPinDialog = true else onOpenSettings()
            })
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (eventName.isNotEmpty()) {
                Text(
                    text = eventName.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 6.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                "FOTOBOX",
                style = MaterialTheme.typography.displayLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 8.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Das ultimative Foto-Erlebnis",
                style = MaterialTheme.typography.titleLarge.copy(color = Color.White.copy(0.55f))
            )
            Spacer(modifier = Modifier.height(56.dp))

            Box(
                modifier = Modifier
                    .scale(pulse)
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                        )
                    )
                    .clickable(onClick = onStartCamera),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(60.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "START",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp,
                            fontSize = 22.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Tippe um Fotos aufzunehmen",
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(0.45f)),
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 20.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatusChip(
                text = if (sessionCount > 0) "$sessionCount Aufnahmen" else "Keine Aufnahmen",
                color = Color.White.copy(0.25f)
            )
            StatusChip(text = "Fotobox v1.0", color = Color.White.copy(0.15f))
            StatusChip(
                text = if (audioCount > 0) "$audioCount Stimmen" else "Gästebuch leer",
                color = Color.White.copy(0.25f)
            )
        }
    }

    if (showPinDialog) {
        var enteredPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPinDialog = false; enteredPin = ""; pinError = false },
            title = { Text("Einstellungen entsperren") },
            text = {
                Column {
                    Text(
                        "Bitte PIN eingeben",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) enteredPin = it },
                        label = { Text("PIN") },
                        singleLine = true,
                        isError = pinError,
                        supportingText = if (pinError) {
                            { Text("Falscher PIN") }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (enteredPin == settingsPin) {
                        showPinDialog = false
                        enteredPin = ""
                        pinError = false
                        onOpenSettings()
                    } else {
                        pinError = true
                    }
                }) {
                    Text("Entsperren")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false; enteredPin = ""; pinError = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun CloudStatusBadge(status: SyncStatus, modifier: Modifier = Modifier) {
    val (icon, color, label) = when (status) {
        SyncStatus.IDLE -> Triple(Icons.Default.Cloud, Color.White.copy(0.5f), "Cloud")
        SyncStatus.SYNCING -> Triple(Icons.Default.Sync, Color(0xFF42A5F5), "Syncing…")
        SyncStatus.SUCCESS -> Triple(Icons.Default.CheckCircle, Color(0xFF66BB6A), "Gespeichert")
        SyncStatus.ERROR -> Triple(Icons.Default.SyncProblem, Color(0xFFEF5350), "Fehler")
        SyncStatus.OFFLINE -> Triple(Icons.Default.Cloud, Color.Transparent, "")
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(0.4f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelLarge.copy(color = color))
    }
}

@Composable
private fun AnimatedBackground(rotation: Float) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0A0520), Color(0xFF150A2A))))
        rotate(rotation, pivot = androidx.compose.ui.geometry.Offset(cx, cy)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(0.35f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(cx - 200f, cy - 150f),
                    radius = 500f
                ),
                radius = 500f,
                center = androidx.compose.ui.geometry.Offset(cx - 200f, cy - 150f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondary.copy(0.3f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(cx + 250f, cy + 200f),
                    radius = 400f
                ),
                radius = 400f,
                center = androidx.compose.ui.geometry.Offset(cx + 250f, cy + 200f)
            )
        }
    }
}

@Composable
private fun NavButton(icon: ImageVector, label: String, highlight: Boolean = false, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlight) MaterialTheme.colorScheme.primary.copy(0.25f) else Color.Black.copy(0.4f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon, label,
            tint = if (highlight) MaterialTheme.colorScheme.primary else Color.White.copy(0.85f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(
                color = if (highlight) MaterialTheme.colorScheme.primary else Color.White.copy(0.6f),
                fontSize = 10.sp
            )
        )
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(color = Color.White.copy(0.7f)))
    }
}
