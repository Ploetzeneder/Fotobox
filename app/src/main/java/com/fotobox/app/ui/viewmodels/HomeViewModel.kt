package com.fotobox.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fotobox.app.data.repository.FotoboxRepository
import com.fotobox.app.network.CloudSyncManager
import com.fotobox.app.network.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: FotoboxRepository,
    private val cloudSync: CloudSyncManager
) : ViewModel() {

    val sessionCount: StateFlow<Int> = repository.getAllSessions()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPrints: StateFlow<Int> = repository.getAllSessions()
        .map { sessions -> sessions.sumOf { it.printCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val audioCount: StateFlow<Int> = repository.getAllAudioRecordings()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val syncStatus: StateFlow<SyncStatus> = cloudSync.syncStatus

    private val _eventName = MutableStateFlow("")
    val eventName: StateFlow<String> = _eventName.asStateFlow()

    private val _kioskMode = MutableStateFlow(false)
    val kioskMode: StateFlow<Boolean> = _kioskMode.asStateFlow()

    private val _settingsPin = MutableStateFlow("")
    val settingsPin: StateFlow<String> = _settingsPin.asStateFlow()

    private val _idleSlideshowDelay = MutableStateFlow(0)
    val idleSlideshowDelay: StateFlow<Int> = _idleSlideshowDelay.asStateFlow()

    init {
        loadSettingsIntoState()
    }

    fun refresh() {
        loadSettingsIntoState()
    }

    private fun loadSettingsIntoState() {
        val settings = repository.loadSettings()
        _eventName.value = settings.eventName
        _kioskMode.value = settings.kioskMode
        _settingsPin.value = settings.settingsPin
        _idleSlideshowDelay.value = settings.idleSlideshowDelay
        cloudSync.configure(settings.cloudApiUrl, settings.cloudToken, settings.cloudBoxId)
    }
}
