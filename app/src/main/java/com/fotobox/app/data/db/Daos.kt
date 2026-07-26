package com.fotobox.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fotobox.app.data.models.Photo
import com.fotobox.app.data.models.PhotoSession
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: PhotoSession): Long

    @Update
    suspend fun update(session: PhotoSession)

    @Delete
    suspend fun delete(session: PhotoSession)

    @Query("SELECT * FROM photo_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<PhotoSession>>

    @Query("SELECT * FROM photo_sessions WHERE id = :id")
    suspend fun getById(id: Long): PhotoSession?

    @Query("SELECT COUNT(*) FROM photo_sessions")
    suspend fun getCount(): Int

    @Query("UPDATE photo_sessions SET stripFilePath = :path WHERE id = :id")
    suspend fun updateStripPath(id: Long, path: String)

    @Query("UPDATE photo_sessions SET printCount = printCount + 1 WHERE id = :id")
    suspend fun incrementPrintCount(id: Long)
}

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: Photo): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<Photo>): List<Long>

    @Delete
    suspend fun delete(photo: Photo)

    @Query("SELECT * FROM photos WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    suspend fun getPhotosForSession(sessionId: Long): List<Photo>

    @Query("SELECT * FROM photos WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    fun getPhotosForSessionFlow(sessionId: Long): Flow<List<Photo>>

    @Query("DELETE FROM photos WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}
