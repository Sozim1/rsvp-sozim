package com.wrsvp.watch.di

import android.content.Context
import androidx.room.Room
import com.wrsvp.data.BookRepository
import com.wrsvp.data.ProgressRepository
import com.wrsvp.data.ReaderRepository
import com.wrsvp.data.SettingsRepository
import com.wrsvp.data.WristRsvpDatabase
import com.wrsvp.parser.BookImportService
import com.wrsvp.reader.AnchorCalculator
import com.wrsvp.reader.ReaderPacingEngine
import com.wrsvp.reader.TextTokenizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WatchDataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WristRsvpDatabase {
        return Room.databaseBuilder(
            context,
            WristRsvpDatabase::class.java,
            "wrist-rsvp-reader.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideBookRepository(database: WristRsvpDatabase): BookRepository = BookRepository(database)

    @Provides
    fun provideReaderRepository(database: WristRsvpDatabase): ReaderRepository = ReaderRepository(database.tokenDao(), database.chapterDao())

    @Provides
    fun provideProgressRepository(database: WristRsvpDatabase): ProgressRepository = ProgressRepository(database.progressDao())

    @Provides
    fun provideSettingsRepository(database: WristRsvpDatabase): SettingsRepository = SettingsRepository(database.settingsDao())

    @Provides
    fun provideTextTokenizer(): TextTokenizer = TextTokenizer()

    @Provides
    fun provideBookImportService(): BookImportService = BookImportService()

    @Provides
    fun provideReaderPacingEngine(): ReaderPacingEngine = ReaderPacingEngine()

    @Provides
    fun provideAnchorCalculator(): AnchorCalculator = AnchorCalculator()

}
