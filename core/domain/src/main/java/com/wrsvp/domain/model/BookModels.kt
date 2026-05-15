package com.wrsvp.domain.model

data class Book(
    val id: String,
    val title: String,
    val author: String? = null,
    val language: String? = null,
    val originalFileName: String,
    val originalFileType: String,
    val createdAt: Long,
    val updatedAt: Long,
    val totalWords: Int,
    val totalChapters: Int,
    val checksum: String,
    val coverImagePath: String? = null,
)

data class Chapter(
    val id: String,
    val bookId: String,
    val index: Int,
    val title: String,
    val startTokenIndex: Int,
    val endTokenIndex: Int,
)

data class ReadingProgress(
    val bookId: String,
    val currentTokenIndex: Int,
    val currentChapterIndex: Int,
    val progressPercent: Float,
    val lastReadAt: Long,
    val totalReadingTimeMs: Long,
)
