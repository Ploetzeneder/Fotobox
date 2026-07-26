package com.fotobox.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fotobox.app.data.models.PhotoSession
import com.fotobox.app.ui.viewmodels.GalleryViewModel
import com.fotobox.app.utils.printStrip
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GalleryScreen(
    onBack: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val context = LocalContext.current
    var deleteTarget by remember { mutableStateOf<PhotoSession?>(null) }
    var fullscreenSession by remember { mutableStateOf<PhotoSession?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Zurück", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "Galerie (${sessions.size})",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Noch keine Fotos",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Starte die Kamera um Fotos aufzunehmen",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sessions) { session ->
                    GalleryItem(
                        session = session,
                        onTap = { if (session.stripFilePath != null) fullscreenSession = session },
                        onShare = { session.stripFilePath?.let { sharePhoto(context, it) } },
                        onDelete = { deleteTarget = session }
                    )
                }
            }
        }
    }

    deleteTarget?.let { session ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Foto löschen?") },
            text = { Text("Diese Aufnahme wird unwiderruflich gelöscht.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSession(session)
                    deleteTarget = null
                    if (fullscreenSession?.id == session.id) fullscreenSession = null
                }) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Abbrechen") }
            }
        )
    }

    // Fullscreen viewer
    AnimatedVisibility(
        visible = fullscreenSession != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        fullscreenSession?.let { session ->
            BackHandler { fullscreenSession = null }
            FullscreenPhotoViewer(
                session = session,
                onDismiss = { fullscreenSession = null },
                onShare = { session.stripFilePath?.let { sharePhoto(context, it) } },
                onPrint = {
                    session.stripFilePath?.let { path ->
                        (context as? ComponentActivity)?.let { act -> printStrip(act, path) }
                    }
                },
                onDelete = { deleteTarget = session }
            )
        }
    }
}

@Composable
private fun FullscreenPhotoViewer(
    session: PhotoSession,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.96f))
            .clickable(onClick = onDismiss)
    ) {
        // Strip image centered
        AsyncImage(
            model = session.stripFilePath,
            contentDescription = "Fotostreifen",
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 80.dp)
                .clickable { /* consume click so background doesn't dismiss */ },
            contentScale = ContentScale.Fit
        )

        // Top-right controls
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ActionButton(onClick = onShare, icon = { Icon(Icons.Default.Share, "Teilen", tint = Color.White, modifier = Modifier.size(20.dp)) })
            ActionButton(onClick = onPrint, icon = { Icon(Icons.Default.Print, "Drucken", tint = Color.White, modifier = Modifier.size(20.dp)) })
            ActionButton(onClick = onDelete, icon = { Icon(Icons.Default.Delete, "Löschen", tint = Color(0xFFEF5350), modifier = Modifier.size(20.dp)) })
            ActionButton(onClick = onDismiss, icon = { Icon(Icons.Default.Close, "Schließen", tint = Color.White, modifier = Modifier.size(22.dp)) })
        }

        // Date at bottom
        Text(
            formatDate(session.createdAt),
            style = MaterialTheme.typography.labelLarge.copy(color = Color.White.copy(0.5f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }
}

@Composable
private fun ActionButton(onClick: () -> Unit, icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(0.6f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun GalleryItem(
    session: PhotoSession,
    onTap: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box {
            AsyncImage(
                model = session.stripFilePath ?: "",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.6f),
                contentScale = ContentScale.Crop
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Color.Black.copy(0.5f))
                    .padding(2.dp)
            ) {
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Share, "Teilen", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Löschen", tint = Color.Red, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                text = formatDate(session.createdAt),
                style = MaterialTheme.typography.labelLarge.copy(color = Color.White),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(0.5f))
                    .padding(4.dp)
            )
        }
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd.MM.yy · HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun sharePhoto(context: Context, path: String) {
    val file = File(path)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, "Foto teilen"))
}
