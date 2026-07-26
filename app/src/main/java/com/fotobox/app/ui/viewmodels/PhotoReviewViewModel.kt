package com.fotobox.app.ui.viewmodels

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fotobox.app.data.models.Photo
import com.fotobox.app.data.repository.FotoboxRepository
import com.fotobox.app.network.CloudSyncManager
import com.fotobox.app.network.LocalPhotoServer
import com.fotobox.app.utils.QrUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PhotoReviewUiState(
    val photos: List<Photo> = emptyList(),
    val stripPath: String? = null,
    val qrBitmap: Bitmap? = null,
    val isLoading: Boolean = true,
    val shouldAutoPrint: Boolean = false,
    val printCopies: Int = 1,
    val autoReturnDelay: Int = 0
)

@HiltViewModel
class PhotoReviewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: FotoboxRepository,
    private val cloudSync: CloudSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoReviewUiState())
    val uiState: StateFlow<PhotoReviewUiState> = _uiState.asStateFlow()

    private val server by lazy {
        LocalPhotoServer(context, repository.loadSettings().localServerPort)
    }

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            val session = repository.getSession(sessionId)
            val photos = repository.getPhotosForSession(sessionId)
            val settings = repository.loadSettings()
            _uiState.value = PhotoReviewUiState(
                photos = photos,
                stripPath = session?.stripFilePath,
                isLoading = false,
                shouldAutoPrint = settings.autoPrint,
                printCopies = settings.printCopies,
                autoReturnDelay = settings.autoReturnDelay
            )
            if (settings.autoUploadCloud) {
                cloudSync.syncSessionAsync(sessionId)
            }
        }
    }

    fun generateQrUrl(sessionId: Long) {
        viewModelScope.launch {
            server.start()
            val url = server.stripUrl(sessionId) ?: return@launch
            val qr = withContext(Dispatchers.Default) {
                QrUtils.generateStyledQrBitmap(url, "Foto herunterladen")
            }
            _uiState.value = _uiState.value.copy(qrBitmap = qr)
        }
    }

    override fun onCleared() {
        server.stop()
        super.onCleared()
    }
}
