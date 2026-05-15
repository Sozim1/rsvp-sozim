package com.wrsvp.data

import com.wrsvp.domain.model.Book
import com.wrsvp.domain.model.Chapter
import com.wrsvp.domain.model.FontScale
import com.wrsvp.domain.model.PunctuationType
import com.wrsvp.domain.model.ReaderSettings
import com.wrsvp.domain.model.ReaderTheme
import com.wrsvp.domain.model.ReadingMode
import com.wrsvp.domain.model.ReadingProgress
import com.wrsvp.domain.model.ReadingToken

fun Book.toEntity(): BookEntity = BookEntity(
    id = id,
    title = title,
    author = author,
    language = language,
    originalFileName = originalFileName,
    originalFileType = originalFileType,
    createdAt = createdAt,
    updatedAt = updatedAt,
    totalWords = totalWords,
    totalChapters = totalChapters,
    checksum = checksum,
)

fun BookEntity.toDomain(): Book = Book(
    id = id,
    title = title,
    author = author,
    language = language,
    originalFileName = originalFileName.orEmpty(),
    originalFileType = originalFileType.orEmpty(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    totalWords = totalWords,
    totalChapters = totalChapters,
    checksum = checksum.orEmpty(),
)

fun Chapter.toEntity(): ChapterEntity = ChapterEntity(
    id = id,
    bookId = bookId,
    index = index,
    title = title,
    startTokenIndex = startTokenIndex,
    endTokenIndex = endTokenIndex,
)

fun ChapterEntity.toDomain(): Chapter = Chapter(
    id = id,
    bookId = bookId,
    index = index,
    title = title,
    startTokenIndex = startTokenIndex,
    endTokenIndex = endTokenIndex,
)

fun ReadingToken.toEntity(bookId: String): TokenEntity = TokenEntity(
    id = id,
    bookId = bookId,
    tokenIndex = id.toInt(),
    text = text,
    normalizedText = normalizedText,
    chapterIndex = chapterIndex,
    paragraphIndex = paragraphIndex,
    sentenceIndex = sentenceIndex,
    wordIndex = wordIndex,
    startCharOffset = startCharOffset,
    endCharOffset = endCharOffset,
    punctuationType = punctuationType.name,
    isSentenceEnd = isSentenceEnd,
    complexityScore = complexityScore,
    estimatedDurationMs = estimatedDurationMs,
)

fun TokenEntity.toDomain(): ReadingToken = ReadingToken(
    id = id,
    text = text,
    normalizedText = normalizedText,
    chapterIndex = chapterIndex,
    paragraphIndex = paragraphIndex,
    sentenceIndex = sentenceIndex,
    wordIndex = wordIndex,
    startCharOffset = startCharOffset,
    endCharOffset = endCharOffset,
    punctuationType = runCatching { PunctuationType.valueOf(punctuationType) }.getOrDefault(PunctuationType.None),
    isSentenceEnd = isSentenceEnd,
    complexityScore = complexityScore,
    estimatedDurationMs = estimatedDurationMs,
)

fun ReadingProgress.toEntity(): ProgressEntity = ProgressEntity(
    bookId = bookId,
    currentTokenIndex = currentTokenIndex,
    currentChapterIndex = currentChapterIndex,
    progressPercent = progressPercent,
    lastReadAt = lastReadAt,
    totalReadingTimeMs = totalReadingTimeMs,
)

fun ProgressEntity.toDomain(): ReadingProgress = ReadingProgress(
    bookId = bookId,
    currentTokenIndex = currentTokenIndex,
    currentChapterIndex = currentChapterIndex,
    progressPercent = progressPercent,
    lastReadAt = lastReadAt,
    totalReadingTimeMs = totalReadingTimeMs,
)

fun ReaderSettings.toEntity(id: String = bookId ?: "global"): ReaderSettingsEntity = ReaderSettingsEntity(
    id = id,
    bookId = bookId,
    wpm = wpm,
    fontSize = fontScale.name,
    theme = theme.name,
    readingMode = readingMode.name,
    phantomWordsEnabled = phantomWordsEnabled,
    anchorHighlightEnabled = anchorHighlightEnabled,
    sentencePauseEnabled = sentencePauseEnabled,
    punctuationPauseEnabled = punctuationPauseEnabled,
    longWordPauseEnabled = longWordPauseEnabled,
    complexWordPauseEnabled = complexWordPauseEnabled,
    footerEnabled = footerEnabled,
    progressEnabled = progressEnabled,
    estimatedTimeEnabled = estimatedTimeEnabled,
    letterSpacingEm = letterSpacingEm,
    anchorDefaultRatio = anchorDefaultRatio,
    anchorColorName = anchorColorName,
    commaPauseMs = commaPauseMs,
    colonPauseMs = colonPauseMs,
    sentencePauseMs = sentencePauseMs,
    longWordExtraMsPerChar = longWordExtraMsPerChar,
    complexWordExtraMs = complexWordExtraMs,
)

fun ReaderSettingsEntity.toDomain(): ReaderSettings = ReaderSettings(
    bookId = bookId,
    wpm = wpm,
    fontScale = runCatching { FontScale.valueOf(fontSize) }.getOrDefault(FontScale.Medium),
    theme = runCatching { ReaderTheme.valueOf(theme) }.getOrDefault(ReaderTheme.Dark),
    readingMode = runCatching { ReadingMode.valueOf(readingMode) }.getOrDefault(ReadingMode.Rsvp),
    phantomWordsEnabled = phantomWordsEnabled,
    anchorHighlightEnabled = anchorHighlightEnabled,
    sentencePauseEnabled = sentencePauseEnabled,
    punctuationPauseEnabled = punctuationPauseEnabled,
    longWordPauseEnabled = longWordPauseEnabled,
    complexWordPauseEnabled = complexWordPauseEnabled,
    footerEnabled = footerEnabled,
    progressEnabled = progressEnabled,
    estimatedTimeEnabled = estimatedTimeEnabled,
    letterSpacingEm = letterSpacingEm,
    anchorDefaultRatio = anchorDefaultRatio,
    anchorColorName = anchorColorName,
    commaPauseMs = commaPauseMs,
    colonPauseMs = colonPauseMs,
    sentencePauseMs = sentencePauseMs,
    longWordExtraMsPerChar = longWordExtraMsPerChar,
    complexWordExtraMs = complexWordExtraMs,
)
