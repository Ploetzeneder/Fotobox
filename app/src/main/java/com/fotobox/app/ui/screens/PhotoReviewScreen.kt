package com.fotobox.app.ui.screens

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fotobox.app.ui.viewmodels.PhotoReviewViewModel
import com.fotobox.app.utils.QrUtils
import com.fotobox.app.utils.ShareUtils
import com.fotobox.app.utils.printStrip
import kotlinx.coroutines.delay

@Composable
fun PhotoReviewScreen(
    sessionId: Long,
    onRetake: () -> Unit,
    onDone: () -> Unit,
    viewModel: PhotoReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showQr by remember { mutableStateOf(false) }
    var autoPrintDone by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableIntStateOf(0) }

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
        viewModel.generateQrUrl(sessionId)
    }

    LaunchedEffect(uiState.stripPath, uiState.isLoading) {
        if (!autoPrintDone && !uiState.isLoading && uiState.shouldAutoPrint && uiState.stripPath != null) {
            autoPrintDone = true
            (context as? ComponentActivity)?.let {
                printStrip(it, uiState.stripPath!!, uiState.printCopies.coerceAtLeast(1))
                viewModel.recordPrint()
            }
        }
    }

    LaunchedEffect(uiState.autoReturnDelay, uiState.isLoading) {
        val totalSeconds = uiState.autoReturnDelay
        if (totalSeconds <= 0 || uiState.isLoading) return@LaunchedEffect
        secondsLeft = totalSeconds
        for (i in totalSeconds downTo 1) {
            secondsLeft = i
            delay(1000L)
        }
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Geschafft! 🎉",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Streifen-Vorschau
            uiState.stripPath?.let { path ->
                AsyncImage(
                    model = path,
                    contentDescription = "Fotostreifen",
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Einzelfotos
            if (uiState.photos.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(uiState.photos) { photo ->
                        AsyncImage(
                            model = photo.filePath,
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Aktions-Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onRetake,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Neu")
                }

                // QR-Code Button
                Button(
                    onClick = { showQr = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1565C0)
                    )
                ) {
                    Icon(Icons.Default.QrCode, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("QR laden")
                }

                Button(
                    onClick = { uiState.stripPath?.let { ShareUtils.sharePhoto(context, it) } },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Teilen")
                }

                Button(
                    onClick = {
                        uiState.stripPath?.let { path ->
                            (context as? ComponentActivity)?.let { act ->
                                printStrip(act, path, uiState.printCopies.coerceAtLeast(1))
                                viewModel.recordPrint()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(if (uiState.printCopies > 1) "×${uiState.printCopies}" else "Drucken")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Fertig — Nächste Gruppe", style = MaterialTheme.typography.titleLarge)
            }
        }

        // Auto-return countdown badge
        if (uiState.autoReturnDelay > 0 && !uiState.isLoading && secondsLeft > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(0.1f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "Weiter in ${secondsLeft}s",
                    style = MaterialTheme.typography.labelLarge.copy(color = Color.White.copy(0.5f))
                )
            }
        }

        // QR-Code Overlay
        AnimatedVisibility(
            visible = showQr,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)),
            exit = scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.88f))
                    .clickable { showQr = false },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    uiState.qrBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(280.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "QR-Code scannen zum Herunterladen",
                            style = MaterialTheme.typography.titleLarge.copy(color = Color.White)
                        )
                        Text(
                            "Funktioniert im gleichen WLAN",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White.copy(0.5f)
                            )
                        )
                    } ?: run {
                        Text(
                            "Kein WLAN verbunden",
                            style = MaterialTheme.typography.headlineMedium.copy(color = Color.White)
                        )
                        Text(
                            "WLAN verbinden um QR-Download zu nutzen",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White.copy(0.6f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Icon(
                        Icons.Default.Close, "Schließen",
                        tint = Color.White.copy(0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        "Tippen zum Schließen",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White.copy(0.4f)
                        )
                    )
                }
            }
        }
    }
}

