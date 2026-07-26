package com.fotobox.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.fotobox.app.data.db.PhotoDao
import com.fotobox.app.data.db.PhotoSessionDao
import com.fotobox.app.data.models.CountdownDuration
import com.fotobox.app.data.models.FotoboxSettings
import com.fotobox.app.data.models.Photo
import com.fotobox.app.data.models.PhotoFilter
import com.fotobox.app.data.models.PhotoSession
import com.fotobox.app.data.models.StripLayout
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FotoboxRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionDao: PhotoSessionDao,
    private val photoDao: PhotoDao
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("fotobox_settings", Context.MODE_PRIVATE)

    fun getAllSessions(): Flow<List<PhotoSession>> = sessionDao.getAllSessions()

    suspend fun createSession(layout: StripLayout): Long =
        sessionDao.insert(PhotoSession(stripLayout = layout))

    suspend fun getSession(id: Long): PhotoSession? = sessionDao.getById(id)

    suspend fun updateStripPath(sessionId: Long, path: String) =
        sessionDao.updateStripPath(sessionId, path)

    suspend fun deleteSession(session: PhotoSession) {
        val photos = photoDao.getPhotosForSession(session.id)
        photos.forEach { photo ->
            try { java.io.File(photo.filePath).delete() } catch (_: Exception) {}
        }
        session.stripFilePath?.let { try { java.io.File(it).delete() } catch (_: Exception) {} }
        sessionDao.delete(session)
    }

    suspend fun savePhotos(photos: List<Photo>): List<Long> =
        photoDao.insertAll(photos)

    suspend fun getPhotosForSession(sessionId: Long): List<Photo> =
        photoDao.getPhotosForSession(sessionId)

    fun getPhotosForSessionFlow(sessionId: Long): Flow<List<Photo>> =
        photoDao.getPhotosForSessionFlow(sessionId)

    fun loadSettings(): FotoboxSettings {
        return FotoboxSettings(
            countdownDuration = CountdownDuration.valueOf(
                prefs.getString("countdown", CountdownDuration.SHORT.name)!!
            ),
            stripLayout = StripLayout.valueOf(
                prefs.getString("strip_layout", StripLayout.STRIP_4.name)!!
            ),
            defaultFilter = PhotoFilter.valueOf(
                prefs.getString("default_filter", PhotoFilter.NONE.name)!!
            ),
            useFlash = prefs.getBoolean("use_flash", true),
            kioskMode = prefs.getBoolean("kiosk_mode", false),
            eventName = prefs.getString("event_name", "") ?: "",
            showLiveFilter = prefs.getBoolean("show_live_filter", true),
            autoPrint = prefs.getBoolean("auto_print", false),
            printCopies = prefs.getInt("print_copies", 1)
        )
    }

    fun saveSettings(settings: FotoboxSettings) {
        prefs.edit().apply {
            putString("countdown", settings.countdownDuration.name)
            putString("strip_layout", settings.stripLayout.name)
            putString("default_filter", settings.defaultFilter.name)
            putBoolean("use_flash", settings.useFlash)
            putBoolean("kiosk_mode", settings.kioskMode)
            putString("event_name", settings.eventName)
            putBoolean("show_live_filter", settings.showLiveFilter)
            putBoolean("auto_print", settings.autoPrint)
            putInt("print_copies", settings.printCopies)
            apply()
        }
    }
}
