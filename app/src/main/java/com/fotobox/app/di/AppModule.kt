package com.fotobox.app.di

import android.content.Context
import androidx.room.Room
import com.fotobox.app.data.db.FotoboxDatabase
import com.fotobox.app.data.db.AudioRecordingDao
import com.fotobox.app.data.db.PhotoDao
import com.fotobox.app.data.db.PhotoSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FotoboxDatabase =
        Room.databaseBuilder(context, FotoboxDatabase::class.java, "fotobox.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSessionDao(db: FotoboxDatabase): PhotoSessionDao = db.photoSessionDao()

    @Provides
    fun providePhotoDao(db: FotoboxDatabase): PhotoDao = db.photoDao()

    @Provides
    fun provideAudioRecordingDao(db: FotoboxDatabase): AudioRecordingDao = db.audioRecordingDao()
}
