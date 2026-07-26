package com.fotobox.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.fotobox.app.data.db.AudioRecordingDao
import com.fotobox.app.data.db.PhotoDao
import com.fotobox.app.data.db.PhotoSessionDao
import com.fotobox.app.data.models.AudioRecording
import com.fotobox.app.data.models.CountdownDuration
import com.fotobox.app.data.models.FotoboxSettings
import com.fotobox.app.data.models.Photo
import com.fotobox.app.data.models.PhotoFilter
import com.fotobox.app.data.models.PhotoSession
import com.fotobox.app.data.models.StripBackground
import com.fotobox.app.data.models.StripLayout
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FotoboxRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionDao: PhotoSessionDao,
    private val photoDao: PhotoDao,
    private val audioDao: AudioRecordingDao
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("fotobox_settings", Context.MODE_PRIVATE)

    // Sessions
    fun getAllSessions(): Flow<List<PhotoSession>> = sessionDao.getAllSessions()
    suspend fun createSession(layout: StripLayout): Long =
        sessionDao.insert(PhotoSession(stripLayout = layout))
    suspend fun getSession(id: Long): PhotoSession? = sessionDao.getById(id)
    suspend fun updateStripPath(sessionId: Long, path: String) =
        sessionDao.updateStripPath(sessionId, path)
    suspend fun incrementPrintCount(sessionId: Long) =
        sessionDao.incrementPrintCount(sessionId)
    suspend fun deleteSession(session: PhotoSession) {
        photoDao.getPhotosForSession(session.id).forEach {
            try { java.io.File(it.filePath).delete() } catch (_: Exception) {}
        }
        session.stripFilePath?.let { try { java.io.File(it).delete() } catch (_: Exception) {} }
        sessionDao.delete(session)
    }

    // Photos
    suspend fun savePhotos(photos: List<Photo>): List<Long> = photoDao.insertAll(photos)
    suspend fun getPhotosForSession(sessionId: Long): List<Photo> =
        photoDao.getPhotosForSession(sessionId)
    fun getPhotosForSessionFlow(sessionId: Long): Flow<List<Photo>> =
        photoDao.getPhotosForSessionFlow(sessionId)

    // Audio Guestbook
    fun getAllAudioRecordings(): Flow<List<AudioRecording>> = audioDao.getAll()
    suspend fun saveAudioRecording(recording: AudioRecording): Long = audioDao.insert(recording)
    suspend fun deleteAudioRecording(recording: AudioRecording) {
        try { java.io.File(recording.filePath).delete() } catch (_: Exception) {}
        audioDao.delete(recording)
    }
    fun newAudioFile(): java.io.File {
        val dir = java.io.File(context.filesDir, "audio").apply { mkdirs() }
        return java.io.File(dir, "voice_${System.currentTimeMillis()}.m4a")
    }

    fun greetingFile(): java.io.File {
        val dir = java.io.File(context.filesDir, "audio").apply { mkdirs() }
        return java.io.File(dir, "greeting.m4a")
    }

    // Settings
    fun loadSettings(): FotoboxSettings = FotoboxSettings(
        countdownDuration = runCatching {
            CountdownDuration.valueOf(prefs.getString("countdown", CountdownDuration.SHORT.name)!!)
        }.getOrDefault(CountdownDuration.SHORT),
        stripLayout = runCatching {
            StripLayout.valueOf(prefs.getString("strip_layout", StripLayout.STRIP_4.name)!!)
        }.getOrDefault(StripLayout.STRIP_4),
        defaultFilter = runCatching {
            PhotoFilter.valueOf(prefs.getString("default_filter", PhotoFilter.NONE.name)!!)
        }.getOrDefault(PhotoFilter.NONE),
        stripBackground = runCatching {
            StripBackground.valueOf(prefs.getString("strip_background", StripBackground.WHITE.name)!!)
        }.getOrDefault(StripBackground.WHITE),
        useFlash = prefs.getBoolean("use_flash", true),
        kioskMode = prefs.getBoolean("kiosk_mode", false),
        settingsPin = prefs.getString("settings_pin", "") ?: "",
        eventName = prefs.getString("event_name", "") ?: "",
        showLiveFilter = prefs.getBoolean("show_live_filter", true),
        autoPrint = prefs.getBoolean("auto_print", false),
        printCopies = prefs.getInt("print_copies", 1),
        autoReturnDelay = prefs.getInt("auto_return_delay", 0),
        idleSlideshowDelay = prefs.getInt("idle_slideshow_delay", 0),
        localServerPort = prefs.getInt("server_port", 8888),
        aiApiKey = prefs.getString("ai_api_key", "") ?: "",
        cloudApiUrl = prefs.getString("cloud_api_url", "https://fotobienchen.de/api") ?: "https://fotobienchen.de/api",
        cloudToken = prefs.getString("cloud_token", "") ?: "",
        cloudBoxId = prefs.getString("cloud_box_id", "") ?: "",
        cloudCustomerName = prefs.getString("cloud_customer_name", "") ?: "",
        autoUploadCloud = prefs.getBoolean("auto_upload_cloud", true),
    )

    fun saveSettings(settings: FotoboxSettings) {
        prefs.edit().apply {
            putString("countdown", settings.countdownDuration.name)
            putString("strip_layout", settings.stripLayout.name)
            putString("default_filter", settings.defaultFilter.name)
            putString("strip_background", settings.stripBackground.name)
            putBoolean("use_flash", settings.useFlash)
            putBoolean("kiosk_mode", settings.kioskMode)
            putString("settings_pin", settings.settingsPin)
            putString("event_name", settings.eventName)
            putBoolean("show_live_filter", settings.showLiveFilter)
            putBoolean("auto_print", settings.autoPrint)
            putInt("print_copies", settings.printCopies)
            putInt("auto_return_delay", settings.autoReturnDelay)
            putInt("idle_slideshow_delay", settings.idleSlideshowDelay)
            putInt("server_port", settings.localServerPort)
            putString("ai_api_key", settings.aiApiKey)
            putString("cloud_api_url", settings.cloudApiUrl)
            putString("cloud_token", settings.cloudToken)
            putString("cloud_box_id", settings.cloudBoxId)
            putString("cloud_customer_name", settings.cloudCustomerName)
            putBoolean("auto_upload_cloud", settings.autoUploadCloud)
            apply()
        }
    }
}
