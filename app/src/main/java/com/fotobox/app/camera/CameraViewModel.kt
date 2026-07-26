package com.fotobox.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fotobox.app.data.models.Photo
import com.fotobox.app.data.models.PhotoFilter
import com.fotobox.app.data.models.StripLayout
import com.fotobox.app.data.repository.FotoboxRepository
import com.fotobox.app.utils.BitmapUtils
import com.fotobox.app.utils.FilterProcessor
import com.fotobox.app.utils.StripComposer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CameraState {
    data object Idle : CameraState()
    data class Countdown(val seconds: Int, val photoIndex: Int, val totalPhotos: Int) : CameraState()
    data class BetweenShots(val takenCount: Int, val totalPhotos: Int) : CameraState()
    data object Capturing : CameraState()
    data object FlashEffect : CameraState()
    data class Processing(val progress: Float) : CameraState()
    data class Done(val sessionId: Long) : CameraState()
    data class Error(val message: String) : CameraState()
}

data class CameraUiState(
    val state: CameraState = CameraState.Idle,
    val selectedFilter: PhotoFilter = PhotoFilter.NONE,
    val selectedLayout: StripLayout = StripLayout.STRIP_4,
    val useFrontCamera: Boolean = false,
    val capturedCount: Int = 0
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: FotoboxRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val capturedBitmaps = mutableListOf<Bitmap>()
    private var imageCapture: ImageCapture? = null

    init {
        val settings = repository.loadSettings()
        _uiState.update {
            it.copy(
                selectedFilter = settings.defaultFilter,
                selectedLayout = settings.stripLayout
            )
        }
    }

    fun setImageCapture(capture: ImageCapture) {
        imageCapture = capture
    }

    fun selectFilter(filter: PhotoFilter) = _uiState.update { it.copy(selectedFilter = filter) }

    fun selectLayout(layout: StripLayout) = _uiState.update { it.copy(selectedLayout = layout) }

    fun toggleCamera() = _uiState.update { it.copy(useFrontCamera = !it.useFrontCamera) }

    fun startSession() {
        if (_uiState.value.state !is CameraState.Idle) return
        val settings = repository.loadSettings()
        val countdownSeconds = settings.countdownDuration.seconds
        val photoCount = _uiState.value.selectedLayout.photoCount
        capturedBitmaps.clear()

        viewModelScope.launch {
            repeat(photoCount) { index ->
                for (sec in countdownSeconds downTo 1) {
                    _uiState.update {
                        it.copy(state = CameraState.Countdown(sec, index + 1, photoCount))
                    }
                    delay(1000L)
                }

                _uiState.update { it.copy(state = CameraState.Capturing) }
                capturePhoto()

                _uiState.update { it.copy(state = CameraState.FlashEffect) }
                delay(200L)

                if (index < photoCount - 1) {
                    _uiState.update {
                        it.copy(
                            state = CameraState.BetweenShots(index + 1, photoCount),
                            capturedCount = index + 1
                        )
                    }
                    delay(1500L)
                }
            }

            buildStrip()
        }
    }

    private suspend fun capturePhoto() {
        val capture = imageCapture ?: return
        val capturedBitmap = kotlinx.coroutines.suspendCancellableCoroutine<Bitmap?> { cont ->
            capture.takePicture(
                androidx.camera.core.ImageCapture.OutputFileOptions.Builder(
                    createTempFile(context)
                ).build(),
                context.mainExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val uri = output.savedUri ?: return cont.resume(null) {}
                        val bitmap = BitmapFactory.decodeStream(
                            context.contentResolver.openInputStream(uri)
                        )
                        // Mirror front camera horizontally
                        val finalBitmap = if (_uiState.value.useFrontCamera && bitmap != null) {
                            val matrix = Matrix().apply { preScale(-1f, 1f) }
                            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
                        } else bitmap
                        cont.resume(finalBitmap) {}
                    }

                    override fun onError(exception: ImageCaptureException) {
                        cont.resume(null) {}
                    }
                }
            )
        }
        capturedBitmap?.let { capturedBitmaps.add(it) }
    }

    private suspend fun buildStrip() {
        if (capturedBitmaps.isEmpty()) {
            _uiState.update { it.copy(state = CameraState.Error("Keine Fotos aufgenommen")) }
            return
        }

        _uiState.update { it.copy(state = CameraState.Processing(0f)) }

        val settings = repository.loadSettings()
        val sessionId = repository.createSession(_uiState.value.selectedLayout)
        val filter = _uiState.value.selectedFilter

        val filteredBitmaps = capturedBitmaps.mapIndexed { i, bmp ->
            _uiState.update { it.copy(state = CameraState.Processing((i + 1f) / capturedBitmaps.size * 0.7f)) }
            FilterProcessor.applyFilter(bmp, filter)
        }

        val photos = filteredBitmaps.mapIndexed { i, bmp ->
            val path = BitmapUtils.saveBitmap(context, bmp, "photo_${sessionId}")
            Photo(sessionId = sessionId, filePath = path, filter = filter, orderIndex = i)
        }
        repository.savePhotos(photos)

        _uiState.update { it.copy(state = CameraState.Processing(0.8f)) }

        val strip = StripComposer.compose(
            filteredBitmaps,
            _uiState.value.selectedLayout,
            eventName = settings.eventName
        )
        val stripPath = BitmapUtils.saveBitmap(context, strip, "strip_${sessionId}")
        repository.updateStripPath(sessionId, stripPath)

        _uiState.update { it.copy(state = CameraState.Done(sessionId)) }
    }

    fun resetToIdle() {
        capturedBitmaps.clear()
        _uiState.update { it.copy(state = CameraState.Idle, capturedCount = 0) }
    }

    private fun createTempFile(context: Context): java.io.File {
        val dir = java.io.File(context.cacheDir, "capture").apply { mkdirs() }
        return java.io.File.createTempFile("cap_", ".jpg", dir)
    }
}
