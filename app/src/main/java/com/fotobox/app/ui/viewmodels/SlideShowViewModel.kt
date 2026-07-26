package com.fotobox.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fotobox.app.data.repository.FotoboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    private var imagePaths = listOf<String>()
    private var currentIndex = 0
    private var running = false

    fun start() {
        viewModelScope.launch {
            val sessions = repository.getAllSessions().first()
            imagePaths = sessions.mapNotNull { it.stripFilePath }
            _hasImages.value = imagePaths.isNotEmpty()
            if (imagePaths.isNotEmpty()) {
                _currentImagePath.value = imagePaths[0]
                running = true
                autoAdvance()
            }
        }
    }

    fun next() {
        if (imagePaths.isEmpty()) return
        currentIndex = (currentIndex + 1) % imagePaths.size
        _currentImagePath.value = imagePaths[currentIndex]
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
