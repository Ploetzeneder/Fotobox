package com.fotobox.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fotobox.app.data.repository.FotoboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlideShowViewModel @Inject constructor(
    private val repository: FotoboxRepository
) : ViewModel() {

    private val _currentImagePath = MutableStateFlow<String?>(null)
    val currentImagePath: StateFlow<String?> = _currentImagePath.asStateFlow()

    private val _hasImages = MutableStateFlow(false)
    val hasImages: StateFlow<Boolean> = _hasImages.asStateFlow()

    private val _eventName = MutableStateFlow("")
    val eventName: StateFlow<String> = _eventName.asStateFlow()

    private val _photoCount = MutableStateFlow(0)
    val photoCount: StateFlow<Int> = _photoCount.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private var imagePaths = listOf<String>()
    private var running = false
    private var started = false

    fun start() {
        val settings = repository.loadSettings()
        _eventName.value = settings.eventName
        if (started) return
        started = true

        // Live updates: when new sessions arrive the slideshow picks them up
        repository.getAllSessions()
            .onEach { sessions ->
                val paths = sessions.mapNotNull { it.stripFilePath }
                val wasEmpty = imagePaths.isEmpty()
                imagePaths = paths
                _hasImages.value = paths.isNotEmpty()
                _photoCount.value = paths.size
                if (wasEmpty && paths.isNotEmpty()) {
                    _currentIndex.value = 0
                    _currentImagePath.value = paths[0]
                    if (!running) {
                        running = true
                        viewModelScope.launch { autoAdvance() }
                    }
                }
                // Clamp index if photos were deleted
                if (_currentIndex.value >= paths.size && paths.isNotEmpty()) {
                    _currentIndex.value = 0
                    _currentImagePath.value = paths[0]
                }
            }
            .launchIn(viewModelScope)
    }

    fun next() {
        if (imagePaths.isEmpty()) return
        val next = (_currentIndex.value + 1) % imagePaths.size
        _currentIndex.value = next
        _currentImagePath.value = imagePaths[next]
    }

    private suspend fun autoAdvance() {
        while (running) {
            delay(5000L)
            next()
        }
    }

    override fun onCleared() {
        running = false
        super.onCleared()
    }
}
