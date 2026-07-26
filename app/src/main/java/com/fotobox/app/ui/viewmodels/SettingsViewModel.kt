package com.fotobox.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.fotobox.app.data.models.CountdownDuration
import com.fotobox.app.data.models.FotoboxSettings
import com.fotobox.app.data.models.PhotoFilter
import com.fotobox.app.data.models.StripLayout
import com.fotobox.app.data.repository.FotoboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: FotoboxRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(repository.loadSettings())
    val settings: StateFlow<FotoboxSettings> = _settings.asStateFlow()

    fun updateEventName(name: String) = update { it.copy(eventName = name) }
    fun updateCountdown(d: CountdownDuration) = update { it.copy(countdownDuration = d) }
    fun updateLayout(l: StripLayout) = update { it.copy(stripLayout = l) }
    fun updateFilter(f: PhotoFilter) = update { it.copy(defaultFilter = f) }
    fun updateFlash(v: Boolean) = update { it.copy(useFlash = v) }
    fun updateKiosk(v: Boolean) = update { it.copy(kioskMode = v) }

    fun save() = repository.saveSettings(_settings.value)

    private fun update(block: (FotoboxSettings) -> FotoboxSettings) {
        _settings.update(block)
        repository.saveSettings(_settings.value)
    }
}
