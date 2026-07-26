package com.fotobox.app.network

import com.fotobox.app.data.models.AudioRecording
import com.fotobox.app.data.repository.FotoboxRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncStatus { IDLE, SYNCING, SUCCESS, ERROR, OFFLINE }

@Singleton
class CloudSyncManager @Inject constructor(
    private val repository: FotoboxRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var api: FotobiechenApi? = null
    private var boxId: String = ""

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

    suspend fun fetchAndApplySettings(): Boolean {
        val currentApi = api ?: return false
        return try {
            val cloud = currentApi.getSettings(boxId) ?: return false
            val current = repository.loadSettings()
            repository.saveSettings(
                current.copy(
                    eventName = cloud.eventName.ifEmpty { current.eventName },
                    autoPrint = cloud.autoPrint,
                    printCopies = cloud.printCopies,
                )
            )
            true
        } catch (_: Exception) { false }
    }
}
