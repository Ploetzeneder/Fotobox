package com.fotobox.app.ui.screens

import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fotobox.app.data.models.AudioRecording
import com.fotobox.app.ui.viewmodels.AudioGuestbookViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AudioGuestbookScreen(
    onBack: () -> Unit,
    viewModel: AudioGuestbookViewModel = hiltViewModel()
) {
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val recordings by viewModel.recordings.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isRecording by remember { mutableStateOf(false) }
    var isPreviewing by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableLongStateOf(0L) }
    var previewFile by remember { mutableStateOf<File?>(null) }

    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    val MAX_SECONDS = 60L

    DisposableEffect(Unit) {
        onDispose {
            recorder?.apply { stop(); release() }
            player?.apply { stop(); release() }
        }
    }

    LaunchedEffect(Unit) {
        if (!micPermission.status.isGranted) micPermission.launchPermissionRequest()
    }

    val pulseAnim = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "pulse"
    )
    val micBg by animateColorAsState(
        targetValue = if (isRecording) Color(0xFFE91E1E) else MaterialTheme.colorScheme.primary,
        animationSpec = tween(300),
        label = "mic_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0A0A1A), Color(0xFF1A0A1A)))
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Zurück", tint = Color.White)
                }
                Text(
                    "Audio-Gästebuch",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White, fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "${recordings.size} Nachrichten",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color.White.copy(0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Recording UI
            if (micPermission.status.isGranted) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(0.07f))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isRecording) {
                        Text(
                            "$recordingSeconds / $MAX_SECONDS Sek.",
                            style = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LinearProgressIndicator(
                            progress = { recordingSeconds / MAX_SECONDS.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = Color(0xFFE91E1E),
                            trackColor = Color.White.copy(0.2f)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    } else if (previewFile != null) {
                        Text(
                            "Nachricht aufgenommen ✓",
                            style = MaterialTheme.typography.titleLarge.copy(color = Color(0xFF4CAF50))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Text(
                            "Halte den Knopf um aufzunehmen",
                            style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(0.6f))
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Mikrofon-Button
                    Box(
                        modifier = Modifier
                            .scale(if (isRecording) pulseScale else 1f)
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(micBg)
                            .clickable {
                                if (isRecording) {
                                    // Stop recording
                                    try {
                                        recorder?.apply { stop(); release() }
                                    } catch (_: Exception) {}
                                    recorder = null
                                    isRecording = false
                                    recordingSeconds = 0
                                    isPreviewing = true
                                } else if (previewFile == null) {
                                    // Start recording
                                    if (!micPermission.status.isGranted) return@clickable
                                    val file = viewModel.newAudioFile()
                                    previewFile = file
                                    val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                        MediaRecorder(context)
                                    else
                                        @Suppress("DEPRECATION") MediaRecorder()
                                    rec.apply {
                                        setAudioSource(MediaRecorder.AudioSource.MIC)
                                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                        setAudioEncodingBitRate(128000)
                                        setAudioSamplingRate(44100)
                                        setMaxDuration(MAX_SECONDS.toInt() * 1000)
                                        setOutputFile(file.absolutePath)
                                        prepare()
                                        start()
                                    }
                                    recorder = rec
                                    isRecording = true
                                    scope.launch {
                                        while (isRecording && recordingSeconds < MAX_SECONDS) {
                                            delay(1000)
                                            if (isRecording) recordingSeconds++
                                        }
                                        if (isRecording) {
                                            // Auto-stop at 60s
                                            try { recorder?.apply { stop(); release() } } catch (_: Exception) {}
                                            recorder = null
                                            isRecording = false
                                            recordingSeconds = 0
                                            isPreviewing = true
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    // Preview + Save actions
                    if (previewFile != null && !isRecording) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Anhören
                            IconButton(
                                onClick = {
                                    if (isPreviewing) {
                                        player?.apply { stop(); release() }
                                        player = null
                                        isPreviewing = false
                                    }
                                    val p = MediaPlayer().apply {
                                        setDataSource(previewFile!!.absolutePath)
                                        prepare()
                                        start()
                                        setOnCompletionListener { isPreviewing = false }
                                    }
                                    player = p
                                    isPreviewing = true
                                },
                                modifier = Modifier
                                    .background(Color.White.copy(0.1f), CircleShape)
                                    .size(52.dp)
                            ) {
                                Icon(
                                    if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    "Anhören", tint = Color.White
                                )
                            }

                            // Speichern
                            IconButton(
                                onClick = {
                                    previewFile?.let { file ->
                                        viewModel.saveRecording(file, recordingSeconds * 1000)
                                    }
                                    previewFile = null
                                    isPreviewing = false
                                    player?.apply { stop(); release() }
                                    player = null
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .size(52.dp)
                            ) {
                                Icon(Icons.Default.Save, "Speichern", tint = Color.White)
                            }

                            // Verwerfen
                            IconButton(
                                onClick = {
                                    previewFile?.delete()
                                    previewFile = null
                                    isPreviewing = false
                                    player?.apply { stop(); release() }
                                    player = null
                                },
                                modifier = Modifier
                                    .background(Color(0xFF4A1010), CircleShape)
                                    .size(52.dp)
                            ) {
                                Icon(Icons.Default.Delete, "Verwerfen", tint = Color.White)
                            }
                        }

                        Row(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                "▶ Anhören   💾 Speichern   🗑 Verwerfen",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Color.White.copy(0.4f), fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Liste der Aufnahmen
            if (recordings.isNotEmpty()) {
                Text(
                    "Aufnahmen",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White, fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recordings) { rec ->
                        AudioRecordingItem(
                            recording = rec,
                            onPlay = {
                                player?.apply { stop(); release() }
                                player = MediaPlayer().apply {
                                    setDataSource(rec.filePath)
                                    prepare()
                                    start()
                                }
                            },
                            onDelete = { viewModel.deleteRecording(rec) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioRecordingItem(
    recording: AudioRecording,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val durationSec = recording.durationMs / 1000
    val date = SimpleDateFormat("dd.MM.yy · HH:mm", Locale.getDefault())
        .format(Date(recording.createdAt))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(0.07f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPlay,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(0.2f), CircleShape)
                .size(44.dp)
        ) {
            Icon(Icons.Default.PlayArrow, "Abspielen", tint = MaterialTheme.colorScheme.primary)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                if (recording.guestName.isNotEmpty()) recording.guestName else "Gast",
                style = MaterialTheme.typography.titleLarge.copy(color = Color.White)
            )
            Text(
                "$date · ${durationSec}s",
                style = MaterialTheme.typography.labelLarge.copy(color = Color.White.copy(0.5f))
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, "Löschen", tint = Color.Red.copy(0.7f))
        }
    }
}
