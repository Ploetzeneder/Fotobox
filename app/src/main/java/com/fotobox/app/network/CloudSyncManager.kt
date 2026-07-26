package com.fotobox.app.network

import com.fotobox.app.data.models.AudioRecording
import com.fotobox.app.data.models.CountdownDuration
import com.fotobox.app.data.models.PhotoFilter
import com.fotobox.app.data.models.StripLayout
import com.fotobox.app.data.repository.FotoboxRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncStatus { IDLE, SYNCING, SUCCESS, ERROR, OFFLINE }

@Singleton
class CloudSyncManager @Inject constructor(
    private val repository: FotoboxRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var api: FotobiechenApi? = null
    @Volatile private var boxId: String = ""

    private val _syncStatus = MutableStateFlow(SyncStatus.OFFLINE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    val isConnected: Boolean get() = api != null && boxId.isNotEmpty()

    fun configure(baseUrl: String, token: String, boxId: String) {
        if (token.isEmpty() || boxId.isEmpty()) {
            _syncStatus.value = SyncStatus.OFFLINE
            api = null
            this.boxId = ""
            return
        }
        api = FotobiechenApi(baseUrl, token)
        this.boxId = boxId
        _syncStatus.value = SyncStatus.IDLE
    }

    fun syncSessionAsync(sessionId: Long) {
        val currentApi = api ?: return
        val currentBoxId = boxId
        scope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            var ok = true
            try {
                val session = repository.getSession(sessionId) ?: return@launch
                val photos = repository.getPhotosForSession(sessionId)
                for (photo in photos) {
                    val file = File(photo.filePath)
                    if (file.exists()) {
                        currentApi.uploadPhoto(currentBoxId, sessionId, file)
                            ?: run { ok = false }
                    }
                }
                session.stripFilePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        currentApi.uploadStrip(currentBoxId, sessionId, file)
                            ?: run { ok = false }
                    }
                }
            } catch (_: Exception) { ok = false }
            _syncStatus.value = if (ok) SyncStatus.SUCCESS else SyncStatus.ERROR
        }
    }

    fun syncAudioAsync(recording: AudioRecording) {
        val currentApi = api ?: return
        val currentBoxId = boxId
        scope.launch {
            val file = File(recording.filePath)
            if (file.exists()) {
                currentApi.uploadAudio(currentBoxId, file, recording.durationMs)
            }
        }
    }

    private fun downloadLogoAsync(url: String) {
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { URL(url).readBytes() }
                repository.logoFile().writeBytes(bytes)
            } catch (_: Exception) {}
        }
    }

    suspend fun fetchAndApplySettings(): Boolean {
        val currentApi = api ?: return false
        return try {
            val cloud = currentApi.getSettings(boxId) ?: return false
            val current = repository.loadSettings()
            val layout = runCatching { StripLayout.valueOf(cloud.stripLayout) }.getOrNull() ?: current.stripLayout
            val filter = runCatching { PhotoFilter.valueOf(cloud.defaultFilter) }.getOrNull() ?: current.defaultFilter
            val countdown = CountdownDuration.entries.minByOrNull {
                kotlin.math.abs(it.seconds - cloud.countdownSeconds)
            } ?: current.countdownDuration
            repository.saveSettings(
                current.copy(
                    eventName = cloud.eventName.ifEmpty { current.eventName },
                    stripLayout = layout,
                    defaultFilter = filter,
                    countdownDuration = countdown,
                    autoPrint = cloud.autoPrint,
                    printCopies = cloud.printCopies,
                    cloudLogoUrl = cloud.logoUrl,
                )
            )
            if (cloud.logoUrl.isNotEmpty() && cloud.logoUrl != current.cloudLogoUrl) {
                downloadLogoAsync(cloud.logoUrl)
            }
            true
        } catch (_: Exception) { false }
    }
}
