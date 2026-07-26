package com.fotobox.app.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fotobox.app.data.models.PhotoSession
import com.fotobox.app.data.repository.FotoboxRepository
import com.fotobox.app.utils.BitmapUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: FotoboxRepository
) : ViewModel() {

    val sessions: StateFlow<List<PhotoSession>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val printCopies: Int get() = repository.loadSettings().printCopies.coerceAtLeast(1)

    fun deleteSession(session: PhotoSession) {
        viewModelScope.launch { repository.deleteSession(session) }
    }

    fun recordPrint(session: PhotoSession) {
        viewModelScope.launch { repository.incrementPrintCount(session.id) }
    }

    suspend fun exportAll(context: Context): Int = withContext(Dispatchers.IO) {
        val currentSessions = sessions.value
        var exported = 0
        currentSessions.forEach { session ->
            val path = session.stripFilePath ?: return@forEach
            if (!File(path).exists()) return@forEach
            val name = File(path).name
            if (BitmapUtils.exportToGallery(context, path, name)) exported++
        }
        exported
    }
}
