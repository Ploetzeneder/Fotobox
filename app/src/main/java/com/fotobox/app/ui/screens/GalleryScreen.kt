package com.fotobox.app.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlin.math.abs
import kotlinx.coroutines.launch
import com.fotobox.app.data.models.PhotoSession
import com.fotobox.app.ui.viewmodels.GalleryViewModel
import com.fotobox.app.utils.ShareUtils
import com.fotobox.app.utils.printStrip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GalleryScreen(
    onBack: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var deleteTarget by remember { mutableStateOf<PhotoSession?>(null) }
    var fullscreenSession by remember { mutableStateOf<PhotoSession?>(null) }
    var exporting by remember { mutableStateOf(false) }
    var exportPendingAfterPermission by remember { mutableStateOf(false) }

    val writePermission = rememberPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    fun doExport() {
        exporting = true
        scope.launch {
            val count = viewModel.exportAll(context)
            exporting = false
            snackbarHostState.showSnackbar(
                if (count > 0) "$count Fotos in Galerie gespeichert" else "Export fehlgeschlagen"
            )
        }
    }

    LaunchedEffect(writePermission.status.isGranted) {
        if (exportPendingAfterPermission) {
            exportPendingAfterPermission = false
            if (writePermission.status.isGranted) {
                doExport()
            } else {
                snackbarHostState.showSnackbar("Speicher-Berechtigung verweigert")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f)
            )
            if (sessions.isNotEmpty()) {
                IconButton(
                    onClick = {
                        if (!exporting) {
                            val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                                    && !writePermission.status.isGranted
                            if (needsPermission) {
                                exportPendingAfterPermission = true
                                writePermission.launchPermissionRequest()
                            } else {
                                doExport()
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Download,
                        "In Galerie exportieren",
                        tint = if (exporting) MaterialTheme.colorScheme.onBackground.copy(0.4f)
                               else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
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
                        onShare = { session.stripFilePath?.let { ShareUtils.sharePhoto(context, it) } },
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

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
    )

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
                onShare = { session.stripFilePath?.let { ShareUtils.sharePhoto(context, it) } },
                onPrint = {
                    session.stripFilePath?.let { path ->
                        (context as? ComponentActivity)?.let { act ->
                            printStrip(act, path, viewModel.printCopies)
                            viewModel.recordPrint(session)
                        }
                    }
                },
                onSave = {
                    scope.launch {
                        val ok = viewModel.exportSession(context, session)
                        snackbarHostState.showSnackbar(
                            if (ok) "In Galerie gespeichert" else "Export fehlgeschlagen"
                        )
                    }
                },
                onDelete = { deleteTarget = session }
            )
        }
    }
    } // end outer Box
}

@Composable
private fun FullscreenPhotoViewer(
    session: PhotoSession,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    var offsetY by remember { mutableStateOf(0f) }
    val bgAlpha = (1f - (abs(offsetY) / 600f)).coerceIn(0.5f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(bgAlpha * 0.96f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (abs(offsetY) > 150f) onDismiss() else offsetY = 0f
                    },
                    onDragCancel = { offsetY = 0f },
                    onVerticalDrag = { _, delta -> offsetY += delta }
                )
            }
    ) {
        // Strip image centered
        AsyncImage(
            model = session.stripFilePath,
            contentDescription = "Fotostreifen",
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 80.dp)
                .offset { IntOffset(0, offsetY.toInt()) },
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
            ActionButton(onClick = onSave, icon = { Icon(Icons.Default.Download, "In Galerie speichern", tint = Color.White, modifier = Modifier.size(20.dp)) })
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
                    .aspectRatio(0.6f)
                    .background(MaterialTheme.colorScheme.surface),
                contentScale = ContentScale.Fit
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

