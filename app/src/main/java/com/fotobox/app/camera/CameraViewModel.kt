package com.fotobox.app.camera

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fotobox.app.data.models.Photo
import com.fotobox.app.data.models.PhotoFilter
import com.fotobox.app.data.models.StripLayout
import com.fotobox.app.data.repository.FotoboxRepository
import com.fotobox.app.utils.BitmapUtils
import com.fotobox.app.utils.FilterProcessor
import com.fotobox.app.utils.ShutterTrigger
import com.fotobox.app.utils.StripComposer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

enum class CameraType(val label: String) {
    USB("USB-Kamera"),
    FRONT("Frontkamera"),
    BACK("Rückkamera"),
    UNKNOWN("Kamera")
}

data class CameraOption(
    val index: Int,
    val type: CameraType,
    val cameraInfo: CameraInfo
)

data class CameraUiState(
    val state: CameraState = CameraState.Idle,
    val selectedFilter: PhotoFilter = PhotoFilter.NONE,
    val selectedLayout: StripLayout = StripLayout.STRIP_4,
    val cameras: List<CameraOption> = emptyList(),
    val selectedCameraIndex: Int = 0,
    val capturedCount: Int = 0,
    val showLiveFilter: Boolean = true
) {
    val selectedCamera: CameraOption? get() = cameras.getOrNull(selectedCameraIndex)
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: FotoboxRepository,
    private val shutterTrigger: ShutterTrigger
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
                selectedLayout = settings.stripLayout,
                showLiveFilter = settings.showLiveFilter
            )
        }
        viewModelScope.launch {
            shutterTrigger.events.collect {
                if (_uiState.value.state is CameraState.Idle) startSession()
            }
        }
    }

    fun setImageCapture(capture: ImageCapture) {
        imageCapture = capture
    }

    fun setAvailableCameras(cameraInfos: List<CameraInfo>) {
        val options = cameraInfos.mapIndexed { i, info ->
            val type = when (info.lensFacing) {
                CameraSelector.LENS_FACING_EXTERNAL -> CameraType.USB
                CameraSelector.LENS_FACING_FRONT -> CameraType.FRONT
                CameraSelector.LENS_FACING_BACK -> CameraType.BACK
                else -> CameraType.UNKNOWN
            }
            CameraOption(i, type, info)
        }
        _uiState.update { current ->
            // Only auto-select best camera on first detection; preserve user choice afterwards
            val bestIndex = if (current.cameras.isEmpty()) {
                options.indexOfFirst { it.type == CameraType.USB }.let { if (it >= 0) it else 0 }
            } else {
                current.selectedCameraIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))
            }
            current.copy(cameras = options, selectedCameraIndex = bestIndex)
        }
    }

    fun selectCamera(index: Int) = _uiState.update { it.copy(selectedCameraIndex = index) }

    fun selectFilter(filter: PhotoFilter) = _uiState.update { it.copy(selectedFilter = filter) }

    fun selectLayout(layout: StripLayout) = _uiState.update { it.copy(selectedLayout = layout) }

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

                if (settings.useFlash) {
                    _uiState.update { it.copy(state = CameraState.FlashEffect) }
                    delay(220L)
                }

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
        val tempFile = createTempFile(context)

        val saved = suspendCancellableCoroutine<Boolean> { cont ->
            capture.takePicture(
                ImageCapture.OutputFileOptions.Builder(tempFile).build(),
                context.mainExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        cont.resume(true) {}
                    }
                    override fun onError(e: ImageCaptureException) {
                        cont.resume(false) {}
                    }
                }
            )
        }
        if (!saved) return
        val bitmap = withContext(Dispatchers.IO) {
            val bmp = BitmapUtils.loadBitmapExifCorrected(tempFile.absolutePath)
            tempFile.delete()
            bmp
        }
        bitmap?.let { capturedBitmaps.add(it) }
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
        val layout = _uiState.value.selectedLayout

        var filteredBitmaps: List<Bitmap> = emptyList()
        try {
            filteredBitmaps = withContext(Dispatchers.Default) {
                capturedBitmaps.mapIndexed { i, bmp ->
                    _uiState.update {
                        it.copy(state = CameraState.Processing((i + 1f) / capturedBitmaps.size * 0.6f))
                    }
                    FilterProcessor.applyFilter(bmp, filter)
                }
            }
            val photos = withContext(Dispatchers.IO) {
                filteredBitmaps.mapIndexed { i, bmp ->
                    val path = BitmapUtils.saveBitmap(context, bmp, "photo_$sessionId")
                    Photo(sessionId = sessionId, filePath = path, filter = filter, orderIndex = i)
                }
            }
            repository.savePhotos(photos)

            _uiState.update { it.copy(state = CameraState.Processing(0.75f)) }

            val logoBitmap = withContext(Dispatchers.IO) {
                val logoFile = repository.logoFile()
                if (logoFile.exists()) BitmapUtils.loadBitmap(logoFile.absolutePath) else null
            }
            val strip = withContext(Dispatchers.Default) {
                StripComposer.compose(
                    filteredBitmaps,
                    layout,
                    eventName = settings.eventName,
                    backgroundColor = settings.stripBackground.colorArgb,
                    logoBitmap = logoBitmap
                )
            }
            logoBitmap?.recycle()
            val stripPath = withContext(Dispatchers.IO) {
                BitmapUtils.saveBitmap(context, strip, "strip_$sessionId")
            }
            strip.recycle()
            repository.updateStripPath(sessionId, stripPath)
            _uiState.update { it.copy(state = CameraState.Done(sessionId)) }
        } catch (e: Exception) {
            _uiState.update { it.copy(state = CameraState.Error("Fehler beim Erstellen (${e.message})")) }
        } finally {
            filteredBitmaps.forEach { if (!it.isRecycled) it.recycle() }
            capturedBitmaps.filter { !it.isRecycled }.forEach { it.recycle() }
            capturedBitmaps.clear()
        }
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
