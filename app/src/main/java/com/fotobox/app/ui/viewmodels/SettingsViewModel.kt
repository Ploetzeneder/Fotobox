package com.fotobox.app.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fotobox.app.data.models.CountdownDuration
import com.fotobox.app.data.models.FotoboxSettings
import com.fotobox.app.data.models.FrameStyle
import com.fotobox.app.data.models.PhotoFilter
import com.fotobox.app.data.models.StripBackground
import com.fotobox.app.data.models.StripLayout
import com.fotobox.app.data.repository.FotoboxRepository
import com.fotobox.app.network.DalleClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: FotoboxRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(repository.loadSettings())
    val settings: StateFlow<FotoboxSettings> = _settings.asStateFlow()

    private val _aiGenerating = MutableStateFlow(false)
    val aiGenerating: StateFlow<Boolean> = _aiGenerating.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val _logoExists = MutableStateFlow(repository.logoFile().exists())
    val logoExists: StateFlow<Boolean> = _logoExists.asStateFlow()
    val logoFilePath: String get() = repository.logoFile().absolutePath

    fun updateEventName(name: String) = update { it.copy(eventName = name) }
    fun updateCountdown(d: CountdownDuration) = update { it.copy(countdownDuration = d) }
    fun updateLayout(l: StripLayout) = update { it.copy(stripLayout = l) }
    fun updateFilter(f: PhotoFilter) = update { it.copy(defaultFilter = f) }
    fun updateFlash(v: Boolean) = update { it.copy(useFlash = v) }
    fun updateShowLiveFilter(v: Boolean) = update { it.copy(showLiveFilter = v) }
    fun updateKiosk(v: Boolean) = update { it.copy(kioskMode = v) }
    fun updateSettingsPin(pin: String) = update { it.copy(settingsPin = pin) }
    fun updateAutoPrint(v: Boolean) = update { it.copy(autoPrint = v) }
    fun updatePrintCopies(n: Int) = update { it.copy(printCopies = n.coerceIn(1, 5)) }
    fun updateAutoReturnDelay(seconds: Int) = update { it.copy(autoReturnDelay = seconds) }
    fun updateIdleSlideshowDelay(seconds: Int) = update { it.copy(idleSlideshowDelay = seconds) }
    fun updateStripBackground(bg: StripBackground) = update { it.copy(stripBackground = bg) }
    fun updateAiApiKey(key: String) = update { it.copy(aiApiKey = key) }
    fun updateAutoUpload(v: Boolean) = update { it.copy(autoUploadCloud = v) }
    fun updateLocalServerPort(port: Int) = update { it.copy(localServerPort = port.coerceIn(1024, 65535)) }
    fun updateFrameStyle(style: FrameStyle) = update { it.copy(frameStyle = style) }

    fun generateAiBackground() {
        val key = _settings.value.aiApiKey
        if (key.isBlank()) { _aiError.value = "OpenAI API-Key fehlt"; return }
        viewModelScope.launch {
            _aiGenerating.value = true
            _aiError.value = null
            val bitmap = DalleClient(key).generateBackground(_settings.value.eventName)
            if (bitmap != null) {
                DalleClient.saveBitmap(context, bitmap)
                bitmap.recycle()
                _aiError.value = null
            } else {
                _aiError.value = "Generierung fehlgeschlagen. API-Key prüfen."
            }
            _aiGenerating.value = false
        }
    }

    fun clearAiBackground() {
        DalleClient.cacheFile(context).delete()
    }

    fun setLogo(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@withContext
                    repository.logoFile().writeBytes(bytes)
                    _logoExists.value = true
                } catch (_: Exception) {}
            }
        }
    }

    fun clearLogo() {
        repository.logoFile().delete()
        _logoExists.value = false
    }

    fun save() = repository.saveSettings(_settings.value)

    private fun update(block: (FotoboxSettings) -> FotoboxSettings) {
        _settings.update(block)
        repository.saveSettings(_settings.value)
    }
}
