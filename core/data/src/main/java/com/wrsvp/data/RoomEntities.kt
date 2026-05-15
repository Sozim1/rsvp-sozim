package com.wrsvp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    indices = [Index("id", unique = true)],
)
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val language: String?,
    val originalFileName: String?,
    val originalFileType: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val totalWords: Int,
    val totalChapters: Int,
    val checksum: String?,
)

@Entity(
    tableName = "chapters",
    primaryKeys = ["id"],
    indices = [Index("bookId"), Index("bookId", "index")],
)
data class ChapterEntity(
    val id: String,
    val bookId: String,
    val index: Int,
    val title: String,
    val startTokenIndex: Int,
    val endTokenIndex: Int,
)

@Entity(
    tableName = "tokens",
    primaryKeys = ["bookId", "tokenIndex"],
    indices = [Index("bookId"), Index("bookId", "tokenIndex"), Index("bookId", "chapterIndex")],
)
data class TokenEntity(
    val id: Long,
    val bookId: String,
    val tokenIndex: Int,
    val text: String,
    val normalizedText: String,
    val chapterIndex: Int,
    val paragraphIndex: Int,
    val sentenceIndex: Int,
    val wordIndex: Int,
    val startCharOffset: Int,
    val endCharOffset: Int,
    val punctuationType: String,
    val isSentenceEnd: Boolean,
    val complexityScore: Int,
    val estimatedDurationMs: Long,
)

@Entity(
    tableName = "progress",
    primaryKeys = ["bookId"],
    indices = [Index("bookId")],
)
data class ProgressEntity(
    val bookId: String,
    val currentTokenIndex: Int,
    val currentChapterIndex: Int,
    val progressPercent: Float,
    val lastReadAt: Long,
    val totalReadingTimeMs: Long,
)

@Entity(
    tableName = "reader_settings",
    primaryKeys = ["id"],
    indices = [Index("bookId")],
)
data class ReaderSettingsEntity(
    val id: String,
    val bookId: String?,
    val wpm: Int,
    val fontSize: String,
    val theme: String,
    val readingMode: String,
    val phantomWordsEnabled: Boolean,
    val anchorHighlightEnabled: Boolean,
    val sentencePauseEnabled: Boolean,
    val punctuationPauseEnabled: Boolean,
    val longWordPauseEnabled: Boolean,
    val complexWordPauseEnabled: Boolean,
    val footerEnabled: Boolean = true,
    val progressEnabled: Boolean = true,
    val estimatedTimeEnabled: Boolean = true,
    val letterSpacingEm: Float = 0f,
    val anchorDefaultRatio: Double = 0.42,
    val anchorColorName: String = "orange",
    val commaPauseMs: Long = 180,
    val colonPauseMs: Long = 220,
    val sentencePauseMs: Long = 350,
    val longWordExtraMsPerChar: Long = 12,
    val complexWordExtraMs: Long = 80,
)

@Entity(
    tableName = "sync_queue",
    indices = [Index("bookId"), Index("status")],
)
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val bookId: String?,
    val path: String,
    val payloadPath: String?,
    val status: String,
    val retryCount: Int,
    val nextAttemptAt: Long,
    val createdAt: Long,
)
