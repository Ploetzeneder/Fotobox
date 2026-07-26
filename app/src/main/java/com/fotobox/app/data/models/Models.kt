package com.fotobox.app.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// 12 Layouts wie KRUU Premium
enum class StripLayout(val photoCount: Int, val label: String, val icon: String) {
    // Klassische Streifen
    SINGLE(1, "Einzelfoto", "▪"),
    STRIP_2(2, "2er Streifen", "▪▪"),
    STRIP_3(3, "3er Streifen", "▪▪▪"),
    STRIP_4(4, "4er Streifen", "▪▪▪▪"),
    // Raster
    GRID_4(4, "2×2 Raster", "⊞"),
    GRID_6(6, "2×3 Raster", "⊟"),
    GRID_9(9, "3×3 Mosaik", "⊠"),
    // Quer / Reihen
    ROW_2(2, "2er Reihe", "◫◫"),
    ROW_3(3, "3er Reihe", "◫◫◫"),
    ROW_4(4, "4er Reihe", "◫◫◫◫"),
    // Highlight-Layouts
    HERO_PLUS_2(3, "Highlight + 2", "◨◧"),
    HERO_PLUS_3(4, "Highlight + 3", "◨◧◧"),
}

enum class PhotoFilter(val label: String) {
    NONE("Original"),
    BLACK_WHITE("S/W"),
    SEPIA("Sepia"),
    VIVID("Lebendig"),
    COOL("Kalt"),
    WARM("Warm"),
    VINTAGE("Vintage"),
    HIGH_CONTRAST("Kontrast"),
    FADED("Verblasst"),
}

enum class CountdownDuration(val seconds: Int, val label: String) {
    SHORT(3, "3 Sek"),
    MEDIUM(5, "5 Sek"),
    LONG(10, "10 Sek"),
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

@Entity(tableName = "audio_recordings")
data class AudioRecording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val guestName: String = ""
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
    val settingsPin: String = "",
    val eventName: String = "",
    val showLiveFilter: Boolean = true,
    val autoPrint: Boolean = false,
    val printCopies: Int = 1,
    val autoReturnDelay: Int = 0,
    val localServerPort: Int = 8888,
    val aiApiKey: String = "",
    // Fotobienchen Cloud
    val cloudApiUrl: String = "https://fotobienchen.de/api",
    val cloudToken: String = "",
    val cloudBoxId: String = "",
    val cloudCustomerName: String = "",
    val autoUploadCloud: Boolean = true,
)
