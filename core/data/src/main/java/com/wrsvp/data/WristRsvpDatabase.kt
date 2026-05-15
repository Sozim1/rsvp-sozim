package com.wrsvp.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        TokenEntity::class,
        ProgressEntity::class,
        ReaderSettingsEntity::class,
        SyncQueueEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class WristRsvpDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun tokenDao(): TokenDao
    abstract fun progressDao(): ProgressDao
    abstract fun settingsDao(): SettingsDao
    abstract fun syncQueueDao(): SyncQueueDao
}
