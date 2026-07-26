package com.fotobox.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fotobox.app.data.models.AudioRecording
import com.fotobox.app.data.repository.FotoboxRepository
import com.fotobox.app.network.CloudSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AudioGuestbookViewModel @Inject constructor(
    private val repository: FotoboxRepository,
    private val cloudSync: CloudSyncManager
) : ViewModel() {

    val recordings: StateFlow<List<AudioRecording>> = repository.getAllAudioRecordings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun newAudioFile(): File = repository.newAudioFile()

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
}
