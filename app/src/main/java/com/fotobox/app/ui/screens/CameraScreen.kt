package com.fotobox.app.ui.screens

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.fotobox.app.camera.CameraState
import com.fotobox.app.camera.CameraType
import com.fotobox.app.camera.CameraViewModel
import com.fotobox.app.data.models.PhotoFilter
import com.fotobox.app.ui.components.BetweenShotsOverlay
import com.fotobox.app.ui.components.CountdownOverlay
import com.fotobox.app.ui.components.FlashEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onPhotosTaken: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    LaunchedEffect(uiState.state) {
        if (uiState.state is CameraState.Done) {
            onPhotosTaken((uiState.state as CameraState.Done).sessionId)
        }
    }

    if (!cameraPermission.status.isGranted) {
        PermissionDeniedScreen(onBack)
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        val selectedCameraInfo = uiState.selectedCamera?.cameraInfo
        CameraPreview(
            cameraSelector = selectedCameraInfo?.cameraSelector ?: CameraSelector.DEFAULT_BACK_CAMERA,
            onCaptureBound = { viewModel.setImageCapture(it) },
            onCamerasDetected = { viewModel.setAvailableCameras(it) }
        )

        AnimatedContent(
            targetState = uiState.state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "camera_state"
        ) { state ->
            when (state) {
                is CameraState.Idle -> IdleOverlay(
                    filters = PhotoFilter.entries,
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = viewModel::selectFilter,
                    onStart = viewModel::startSession,
                    cameras = uiState.cameras.map { it.type },
                    selectedCameraIndex = uiState.selectedCameraIndex,
                    onSelectCamera = viewModel::selectCamera,
                    onBack = onBack,
                    layoutLabel = uiState.selectedLayout.label,
                    photoCount = uiState.selectedLayout.photoCount
                )
                is CameraState.Countdown -> CountdownOverlay(
                    seconds = state.seconds,
                    photoIndex = state.photoIndex,
                    totalPhotos = state.totalPhotos
                )
                is CameraState.BetweenShots -> BetweenShotsOverlay(state.takenCount, state.totalPhotos)
                is CameraState.FlashEffect -> FlashEffect(visible = true)
                is CameraState.Capturing -> Box(modifier = Modifier.fillMaxSize())
                is CameraState.Processing -> ProcessingOverlay(state.progress)
                is CameraState.Error -> ErrorOverlay(state.message, viewModel::resetToIdle)
                else -> Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun CameraPreview(
    cameraSelector: CameraSelector,
    onCaptureBound: (ImageCapture) -> Unit,
    onCamerasDetected: (List<androidx.camera.core.CameraInfo>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(cameraSelector) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()

        // Alle verfügbaren Kameras melden (USB + eingebaut)
        onCamerasDetected(cameraProvider.availableCameraInfos)

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        cameraProvider.unbindAll()
        try {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
            onCaptureBound(imageCapture)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun IdleOverlay(
    filters: List<PhotoFilter>,
    selectedFilter: PhotoFilter,
    onFilterSelected: (PhotoFilter) -> Unit,
    onStart: () -> Unit,
    cameras: List<CameraType>,
    selectedCameraIndex: Int,
    onSelectCamera: (Int) -> Unit,
    onBack: () -> Unit,
    layoutLabel: String,
    photoCount: Int
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.Black.copy(0.55f), CircleShape)
                    .size(48.dp)
            ) {
                Icon(Icons.Default.ArrowBack, "Zurück", tint = Color.White)
            }

            Text(
                text = "$layoutLabel · $photoCount Fotos",
                style = MaterialTheme.typography.labelLarge.copy(color = Color.White),
                modifier = Modifier
                    .background(Color.Black.copy(0.55f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            // Kamera-Auswahl (zeigt USB-Kamera zuerst)
            if (cameras.size > 1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(cameras) { i, cam ->
                        CameraChip(
                            type = cam,
                            isSelected = i == selectedCameraIndex,
                            onClick = { onSelectCamera(i) }
                        )
                    }
                }
            } else {
                cameras.firstOrNull()?.let {
                    CameraChip(type = it, isSelected = true, onClick = {})
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Filter-Auswahl
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(filters) { filter ->
                    FilterChip(
                        filter = filter,
                        isSelected = filter == selectedFilter,
                        onClick = { onFilterSelected(filter) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Auslöser
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onStart),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .border(3.dp, Color.White, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Tippen zum Starten",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun CameraChip(type: CameraType, isSelected: Boolean, onClick: () -> Unit) {
    val icon: ImageVector = when (type) {
        CameraType.USB -> Icons.Default.Usb
        CameraType.FRONT -> Icons.Default.FrontHand
        CameraType.BACK -> Icons.Default.PhoneAndroid
        CameraType.UNKNOWN -> Icons.Default.CameraAlt
    }
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(0.55f)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Text(type.label, style = MaterialTheme.typography.labelLarge.copy(color = Color.White))
    }
}

@Composable
private fun FilterChip(filter: PhotoFilter, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(0.55f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(filter.label, style = MaterialTheme.typography.labelLarge.copy(color = Color.White))
    }
}

@Composable
private fun ProcessingOverlay(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(72.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Streifen wird erstellt…",
                style = MaterialTheme.typography.titleLarge.copy(color = Color.White)
            )
        }
    }
}

@Composable
private fun ErrorOverlay(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Fehler", style = MaterialTheme.typography.headlineLarge.copy(color = Color.Red))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("Nochmal versuchen", color = Color.White)
            }
        }
    }
}

@Composable
private fun PermissionDeniedScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Kamera-Berechtigung fehlt",
                style = MaterialTheme.typography.headlineMedium.copy(color = Color.White)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Bitte erlaube den Kamera-Zugriff in den Einstellungen.",
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}
