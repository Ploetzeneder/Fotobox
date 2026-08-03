package com.fotobox.app.ui.screens

import android.Manifest
import android.content.Context
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
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Usb
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
import com.fotobox.app.ui.viewmodels.MicStatus
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun makeRecorder(context: Context): MediaRecorder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
    else @Suppress("DEPRECATION") MediaRecorder()

private fun MediaRecorder.prepareAudio(outputFile: File, maxDurationMs: Int = 0) {
    setAudioSource(MediaRecorder.AudioSource.MIC)
    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
    setAudioEncodingBitRate(128000)
    setAudioSamplingRate(44100)
    if (maxDurationMs > 0) setMaxDuration(maxDurationMs)
    setOutputFile(outputFile.absolutePath)
    prepare()
    start()
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AudioGuestbookScreen(
    onBack: () -> Unit,
    viewModel: AudioGuestbookViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val recordings by viewModel.recordings.collectAsState()
    val hasGreeting by viewModel.hasGreeting.collectAsState()
    val micStatus by viewModel.micStatus.collectAsState()
    val scope = rememberCoroutineScope()

    val MAX_SECONDS = 60L

    // Guest recording state
    var isRecording by remember { mutableStateOf(false) }
    var isPreviewing by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableLongStateOf(0L) }
    var previewFile by remember { mutableStateOf<File?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var guestName by remember { mutableStateOf("") }
    var savedDurationSec by remember { mutableLongStateOf(0L) }

    // List playback state (separate from preview player)
    var listPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingRecordingId by remember { mutableStateOf<Long?>(null) }

    // Greeting state
    var isGreetingPlaying by remember { mutableStateOf(false) }
    var isRecordingGreeting by remember { mutableStateOf(false) }
    var greetingRecordingSeconds by remember { mutableLongStateOf(0L) }
    var greetingPreviewFile by remember { mutableStateOf<File?>(null) }
    var greetingRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var greetingPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.apply { try { stop() } catch (_: Exception) {}; release() }
            player?.apply { try { stop() } catch (_: Exception) {}; release() }
            greetingRecorder?.apply { try { stop() } catch (_: Exception) {}; release() }
            greetingPlayer?.apply { try { stop() } catch (_: Exception) {}; release() }
            listPlayer?.apply { try { stop() } catch (_: Exception) {}; release() }
        }
    }

    LaunchedEffect(Unit) {
        if (!micPermission.status.isGranted) micPermission.launchPermissionRequest()
        val gFile = viewModel.greetingFile()
        if (gFile.exists()) {
            try {
                val p = MediaPlayer().apply {
                    setDataSource(gFile.absolutePath)
                    prepare()
                    setOnCompletionListener { isGreetingPlaying = false }
                    start()
                }
                greetingPlayer = p
                isGreetingPlaying = true
            } catch (_: Exception) {}
        }
    }

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "scale"
    )
    val micBg by animateColorAsState(
        targetValue = if (isRecording) Color(0xFFE91E1E) else MaterialTheme.colorScheme.primary,
        animationSpec = tween(300), label = "mic_color"
    )
    val greetingMicBg by animateColorAsState(
        targetValue = if (isRecordingGreeting) Color(0xFFE91E1E) else Color(0xFFF59E0B),
        animationSpec = tween(300), label = "greeting_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0A1A), Color(0xFF1A0A1A))))
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
                MicStatusBadge(micStatus)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    "${recordings.size}",
                    style = MaterialTheme.typography.labelLarge.copy(color = Color.White.copy(0.5f))
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Host Greeting Section
            GreetingSection(
                hasGreeting = hasGreeting,
                isPlaying = isGreetingPlaying,
                isRecording = isRecordingGreeting,
                recordingSeconds = greetingRecordingSeconds,
                previewFile = greetingPreviewFile,
                maxSeconds = MAX_SECONDS,
                micBg = greetingMicBg,
                pulseScale = if (isRecordingGreeting) pulseScale else 1f,
                onTogglePlay = {
                    if (isGreetingPlaying) {
                        greetingPlayer?.apply { try { stop() } catch (_: Exception) {}; release() }
                        greetingPlayer = null
                        isGreetingPlaying = false
                    } else {
                        val gFile = viewModel.greetingFile()
                        if (gFile.exists()) {
                            try {
                                val p = MediaPlayer().apply {
                                    setDataSource(gFile.absolutePath)
                                    prepare()
                                    setOnCompletionListener { isGreetingPlaying = false }
                                    start()
                                }
                                greetingPlayer = p
                                isGreetingPlaying = true
                            } catch (_: Exception) {}
                        }
                    }
                },
                onStartRecord = {
                    if (micPermission.status.isGranted) {
                        val file = viewModel.newAudioFile()
                        greetingPreviewFile = file
                        val rec = makeRecorder(context).apply { prepareAudio(file) }
                        greetingRecorder = rec
                        isRecordingGreeting = true
                        scope.launch {
                            while (isRecordingGreeting && greetingRecordingSeconds < MAX_SECONDS) {
                                delay(1000)
                                if (isRecordingGreeting) greetingRecordingSeconds++
                            }
                            if (isRecordingGreeting) {
                                try { greetingRecorder?.apply { stop(); release() } } catch (_: Exception) {}
                                greetingRecorder = null
                                isRecordingGreeting = false
                                greetingRecordingSeconds = 0
                            }
                        }
                    }
                },
                onStopRecord = {
                    try { greetingRecorder?.apply { stop(); release() } } catch (_: Exception) {}
                    greetingRecorder = null
                    isRecordingGreeting = false
                    greetingRecordingSeconds = 0
                },
                onSave = {
                    greetingPreviewFile?.let { viewModel.saveGreeting(it) }
                    greetingPreviewFile = null
                },
                onDiscard = {
                    greetingPreviewFile?.delete()
                    greetingPreviewFile = null
                },
                onDelete = { viewModel.deleteGreeting() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Guest Recording Section
            if (micPermission.status.isGranted) {
                GuestRecordingSection(
                    isBlocked = isGreetingPlaying || isRecordingGreeting,
                    isRecording = isRecording,
                    isPreviewing = isPreviewing,
                    recordingSeconds = recordingSeconds,
                    previewFile = previewFile,
                    maxSeconds = MAX_SECONDS,
                    micBg = micBg,
                    pulseScale = if (isRecording) pulseScale else 1f,
                    onToggleMic = {
                        if (isRecording) {
                            try { recorder?.apply { stop(); release() } } catch (_: Exception) {}
                            recorder = null
                            isRecording = false
                            savedDurationSec = recordingSeconds
                            recordingSeconds = 0
                            // isPreviewing stays false — user taps Play explicitly to listen
                        } else if (previewFile == null) {
                            val file = viewModel.newAudioFile()
                            previewFile = file
                            val rec = makeRecorder(context).apply {
                                prepareAudio(file, MAX_SECONDS.toInt() * 1000)
                            }
                            recorder = rec
                            isRecording = true
                            scope.launch {
                                while (isRecording && recordingSeconds < MAX_SECONDS) {
                                    delay(1000)
                                    if (isRecording) recordingSeconds++
                                }
                                if (isRecording) {
                                    try { recorder?.apply { stop(); release() } } catch (_: Exception) {}
                                    recorder = null
                                    isRecording = false
                                    savedDurationSec = recordingSeconds
                                    recordingSeconds = 0
                                    // isPreviewing stays false: user taps Play explicitly to listen
                                }
                            }
                        }
                    },
                    onPreviewPlay = {
                        if (isPreviewing) {
                            player?.apply { try { stop() } catch (_: Exception) {}; release() }
                            player = null
                            isPreviewing = false
                        } else {
                            val p = MediaPlayer().apply {
                                setDataSource(previewFile!!.absolutePath)
                                prepare()
                                setOnCompletionListener { isPreviewing = false }
                                start()
                            }
                            player = p
                            isPreviewing = true
                        }
                    },
                    guestName = guestName,
                    onGuestNameChange = { guestName = it },
                    onSave = { name ->
                        previewFile?.let { viewModel.saveRecording(it, savedDurationSec * 1000, name) }
                        previewFile = null
                        savedDurationSec = 0
                        isPreviewing = false
                        guestName = ""
                        player?.apply { try { stop() } catch (_: Exception) {}; release() }
                        player = null
                    },
                    onDiscard = {
                        previewFile?.delete()
                        previewFile = null
                        savedDurationSec = 0
                        isPreviewing = false
                        guestName = ""
                        player?.apply { try { stop() } catch (_: Exception) {}; release() }
                        player = null
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                            isPlaying = playingRecordingId == rec.id,
                            onTogglePlay = {
                                if (playingRecordingId == rec.id) {
                                    listPlayer?.apply { try { stop() } catch (_: Exception) {}; release() }
                                    listPlayer = null
                                    playingRecordingId = null
                                } else {
                                    listPlayer?.apply { try { stop() } catch (_: Exception) {}; release() }
                                    playingRecordingId = rec.id
                                    listPlayer = MediaPlayer().apply {
                                        setDataSource(rec.filePath)
                                        prepare()
                                        setOnCompletionListener { playingRecordingId = null }
                                        start()
                                    }
                                }
                            },
                            onDelete = {
                                if (playingRecordingId == rec.id) {
                                    listPlayer?.apply { try { stop() } catch (_: Exception) {}; release() }
                                    listPlayer = null
                                    playingRecordingId = null
                                }
                                viewModel.deleteRecording(rec)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MicStatusBadge(micStatus: MicStatus) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (micStatus == MicStatus.USB) Color(0xFF1B4020).copy(0.8f) else Color.White.copy(0.08f)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            if (micStatus == MicStatus.USB) Icons.Default.Usb else Icons.Default.Mic,
            null,
            tint = if (micStatus == MicStatus.USB) Color(0xFF4CAF50) else Color.White.copy(0.5f),
            modifier = Modifier.size(14.dp)
        )
        Text(
            if (micStatus == MicStatus.USB) "USB-Mikrofon" else "Intern",
            style = MaterialTheme.typography.labelLarge.copy(
                color = if (micStatus == MicStatus.USB) Color(0xFF4CAF50) else Color.White.copy(0.5f),
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun GreetingSection(
    hasGreeting: Boolean,
    isPlaying: Boolean,
    isRecording: Boolean,
    recordingSeconds: Long,
    previewFile: File?,
    maxSeconds: Long,
    micBg: Color,
    pulseScale: Float,
    onTogglePlay: () -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF59E0B).copy(0.08f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.RecordVoiceOver, null,
                tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                "Begrüßungsnachricht",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold
                )
            )
        }
        Text(
            "Wird automatisch für jeden Gast abgespielt",
            style = MaterialTheme.typography.labelLarge.copy(color = Color.White.copy(0.4f))
        )
        Spacer(modifier = Modifier.height(12.dp))

        when {
            previewFile != null -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Aufnahme bereit",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onSave,
                        modifier = Modifier.background(Color(0xFFF59E0B), CircleShape).size(44.dp)
                    ) {
                        Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onDiscard,
                        modifier = Modifier.background(Color(0xFF4A1010), CircleShape).size(44.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
            isRecording -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$recordingSeconds / $maxSeconds Sek.",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { recordingSeconds / maxSeconds.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = Color(0xFFE91E1E),
                        trackColor = Color.White.copy(0.2f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(micBg)
                            .clickable(onClick = onStopRecord),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
            hasGreeting -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier.background(Color(0xFFF59E0B).copy(0.2f), CircleShape).size(48.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            null, tint = Color(0xFFF59E0B)
                        )
                    }
                    Text(
                        if (isPlaying) "Wird abgespielt…" else "Begrüßung vorhanden ✓",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = if (isPlaying) Color(0xFFF59E0B) else Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.background(Color(0xFF4A1010), CircleShape).size(40.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(micBg)
                            .clickable(onClick = onStartRecord),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Text(
                        "Begrüßung aufnehmen",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(0.7f))
                    )
                }
            }
        }
    }
}

@Composable
private fun GuestRecordingSection(
    isBlocked: Boolean,
    isRecording: Boolean,
    isPreviewing: Boolean,
    recordingSeconds: Long,
    previewFile: File?,
    maxSeconds: Long,
    micBg: Color,
    pulseScale: Float,
    guestName: String,
    onGuestNameChange: (String) -> Unit,
    onToggleMic: () -> Unit,
    onPreviewPlay: () -> Unit,
    onSave: (String) -> Unit,
    onDiscard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(if (isBlocked) 0.04f else 0.07f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isBlocked) {
            Icon(
                Icons.Default.MicOff, null,
                tint = Color.White.copy(0.3f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Bitte warten…",
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(0.4f))
            )
            return
        }

        when {
            isRecording -> {
                Text(
                    "$recordingSeconds / $maxSeconds Sek.",
                    style = MaterialTheme.typography.titleLarge.copy(color = Color.White),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LinearProgressIndicator(
                    progress = { recordingSeconds / maxSeconds.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = Color(0xFFE91E1E),
                    trackColor = Color.White.copy(0.2f)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
            previewFile != null -> {
                Text(
                    "Nachricht aufgenommen ✓",
                    style = MaterialTheme.typography.titleLarge.copy(color = Color(0xFF4CAF50))
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            else -> {
                Text(
                    "Halte den Knopf um aufzunehmen",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(0.6f))
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        Box(
            modifier = Modifier
                .scale(pulseScale)
                .size(100.dp)
                .clip(CircleShape)
                .background(micBg)
                .clickable(enabled = previewFile == null || isRecording, onClick = onToggleMic),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                null, tint = Color.White, modifier = Modifier.size(44.dp)
            )
        }

        if (previewFile != null && !isRecording) {
            Spacer(modifier = Modifier.height(20.dp))
            androidx.compose.material3.OutlinedTextField(
                value = guestName,
                onValueChange = onGuestNameChange,
                placeholder = {
                    Text("Name (optional)", style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(0.35f)))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(0.2f),
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = onPreviewPlay,
                    modifier = Modifier.background(Color.White.copy(0.1f), CircleShape).size(52.dp)
                ) {
                    Icon(
                        if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                        "Anhören", tint = Color.White
                    )
                }
                IconButton(
                    onClick = { onSave(guestName) },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape).size(52.dp)
                ) {
                    Icon(Icons.Default.Save, "Speichern", tint = Color.White)
                }
                IconButton(
                    onClick = onDiscard,
                    modifier = Modifier.background(Color(0xFF4A1010), CircleShape).size(52.dp)
                ) {
                    Icon(Icons.Default.Delete, "Verwerfen", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun AudioRecordingItem(
    recording: AudioRecording,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onDelete: () -> Unit
) {
    val durationSec = recording.durationMs / 1000
    val date = SimpleDateFormat("dd.MM.yy · HH:mm", Locale.getDefault())
        .format(Date(recording.createdAt))
    val playBg = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(0.2f)
    val playTint = if (isPlaying) Color.White else MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPlaying) Color.White.copy(0.12f) else Color.White.copy(0.07f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onTogglePlay,
            modifier = Modifier.background(playBg, CircleShape).size(44.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                if (isPlaying) "Stoppen" else "Abspielen",
                tint = playTint
            )
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                if (recording.guestName.isNotEmpty()) recording.guestName else "Gast",
                style = MaterialTheme.typography.titleLarge.copy(color = Color.White)
            )
            Text(
                buildString {
                    append(date)
                    append(" · ")
                    append(durationSec)
                    append("s")
                    if (isPlaying) append(" ▶")
                },
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else Color.White.copy(0.5f)
                )
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, "Löschen", tint = Color.Red.copy(0.7f))
        }
    }
}
