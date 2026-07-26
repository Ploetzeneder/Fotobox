package com.fotobox.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.fotobox.app.data.models.AudioRecording
import com.fotobox.app.data.models.Photo
import com.fotobox.app.data.models.PhotoFilter
import com.fotobox.app.data.models.PhotoSession
import com.fotobox.app.data.models.StripLayout

class Converters {
    @TypeConverter fun fromStripLayout(v: StripLayout): String = v.name
    @TypeConverter fun toStripLayout(v: String): StripLayout = StripLayout.valueOf(v)
    @TypeConverter fun fromPhotoFilter(v: PhotoFilter): String = v.name
    @TypeConverter fun toPhotoFilter(v: String): PhotoFilter = PhotoFilter.valueOf(v)
}

@Database(
    entities = [PhotoSession::class, Photo::class, AudioRecording::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FotoboxDatabase : RoomDatabase() {
    abstract fun photoSessionDao(): PhotoSessionDao
    abstract fun photoDao(): PhotoDao
    abstract fun audioRecordingDao(): AudioRecordingDao
}
