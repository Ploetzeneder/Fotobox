package com.fotobox.app.ui.viewmodels

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fotobox.app.data.models.AudioRecording
import com.fotobox.app.data.repository.FotoboxRepository
import com.fotobox.app.network.CloudSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

enum class MicStatus { BUILTIN, USB }

@HiltViewModel
class AudioGuestbookViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: FotoboxRepository,
    private val cloudSync: CloudSyncManager
) : ViewModel() {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _micStatus = MutableStateFlow(detectMicStatus())
    val micStatus: StateFlow<MicStatus> = _micStatus.asStateFlow()

    private val _hasGreeting = MutableStateFlow(repository.greetingFile().exists())
    val hasGreeting: StateFlow<Boolean> = _hasGreeting.asStateFlow()

    val recordings: StateFlow<List<AudioRecording>> = repository.getAllAudioRecordings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            _micStatus.value = detectMicStatus()
        }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            _micStatus.value = detectMicStatus()
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
    }

    private fun detectMicStatus(): MicStatus {
        val usbTypes = setOf(
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY
        )
        val hasUsb = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).any { it.type in usbTypes }
        return if (hasUsb) MicStatus.USB else MicStatus.BUILTIN
    }

    fun newAudioFile(): File = repository.newAudioFile()
    fun greetingFile(): File = repository.greetingFile()

    fun saveGreeting(file: File) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { file.copyTo(repository.greetingFile(), overwrite = true) }
            _hasGreeting.value = true
        }
    }

    fun deleteGreeting() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.greetingFile().delete() }
            _hasGreeting.value = false
        }
    }

    fun saveRecording(file: File, durationMs: Long) {
        viewModelScope.launch {
            val recording = AudioRecording(filePath = file.absolutePath, durationMs = durationMs)
            repository.saveAudioRecording(recording)
            cloudSync.syncAudioAsync(recording)
        }
    }

    fun deleteRecording(recording: AudioRecording) {
        viewModelScope.launch { repository.deleteAudioRecording(recording) }
    }

    override fun onCleared() {
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
        super.onCleared()
    }
}
