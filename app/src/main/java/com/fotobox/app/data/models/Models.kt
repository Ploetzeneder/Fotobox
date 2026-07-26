package com.fotobox.app.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class StripLayout(val photoCount: Int, val label: String) {
    SINGLE(1, "1 Foto"),
    DOUBLE(2, "2 Fotos"),
    STRIP_3(3, "3er Streifen"),
    STRIP_4(4, "4er Streifen"),
    GRID_4(4, "2x2 Raster")
}

enum class PhotoFilter(val label: String) {
    NONE("Original"),
    BLACK_WHITE("Schwarz/Weiß"),
    SEPIA("Sepia"),
    VIVID("Lebendig"),
    COOL("Kalt"),
    WARM("Warm"),
    VINTAGE("Vintage")
}

enum class CountdownDuration(val seconds: Int, val label: String) {
    SHORT(3, "3 Sekunden"),
    MEDIUM(5, "5 Sekunden"),
    LONG(10, "10 Sekunden")
}

@Entity(tableName = "photo_sessions")
data class PhotoSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val stripLayout: StripLayout = StripLayout.STRIP_4,
    val stripFilePath: String? = null,
    val printCount: Int = 0
)

@Entity(
    tableName = "photos",
    foreignKeys = [ForeignKey(
        entity = PhotoSession::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val filePath: String,
    val takenAt: Long = System.currentTimeMillis(),
    val filter: PhotoFilter = PhotoFilter.NONE,
    val orderIndex: Int = 0
)

data class SessionWithPhotos(
    val session: PhotoSession,
    val photos: List<Photo>
)

data class FotoboxSettings(
    val countdownDuration: CountdownDuration = CountdownDuration.SHORT,
    val stripLayout: StripLayout = StripLayout.STRIP_4,
    val defaultFilter: PhotoFilter = PhotoFilter.NONE,
    val useFlash: Boolean = true,
    val kioskMode: Boolean = false,
    val eventName: String = "",
    val showLiveFilter: Boolean = true,
    val autoPrint: Boolean = false,
    val printCopies: Int = 1
)
