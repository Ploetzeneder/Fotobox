package com.fotobox.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fotobox.app.data.models.Photo
import com.fotobox.app.data.repository.FotoboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhotoReviewUiState(
    val photos: List<Photo> = emptyList(),
    val stripPath: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class PhotoReviewViewModel @Inject constructor(
    private val repository: FotoboxRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoReviewUiState())
    val uiState: StateFlow<PhotoReviewUiState> = _uiState.asStateFlow()

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            val session = repository.getSession(sessionId)
            val photos = repository.getPhotosForSession(sessionId)
            _uiState.value = PhotoReviewUiState(
                photos = photos,
                stripPath = session?.stripFilePath,
                isLoading = false
            )
        }
    }
}
