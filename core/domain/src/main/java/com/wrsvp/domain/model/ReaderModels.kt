package com.wrsvp.domain.model

enum class PunctuationType {
    None,
    Comma,
    Colon,
    SentenceEnd,
}

enum class ReadingMode {
    Rsvp,
    PageScroll,
}

enum class ReaderTheme {
    Dark,
    Light,
    Night,
}

enum class FontScale {
    Small,
    Medium,
    Large,
    ExtraLarge,
}

data class ReadingToken(
    val id: Long,
    val text: String,
    val normalizedText: String,
    val chapterIndex: Int,
    val paragraphIndex: Int,
    val sentenceIndex: Int,
    val wordIndex: Int,
    val startCharOffset: Int,
    val endCharOffset: Int,
    val punctuationType: PunctuationType,
    val isSentenceEnd: Boolean,
    val complexityScore: Int,
    val estimatedDurationMs: Long,
)

data class ReaderSettings(
    val bookId: String? = null,
    val wpm: Int = ReaderConfig().defaultWpm,
    val minWpm: Int = ReaderConfig().minWpm,
    val maxWpm: Int = ReaderConfig().maxWpm,
    val wpmStep: Int = ReaderConfig().wpmStep,
    val fontScale: FontScale = FontScale.Medium,
    val typeface: String = "system",
    val theme: ReaderTheme = ReaderTheme.Light,
    val readingMode: ReadingMode = ReadingMode.Rsvp,
    val phantomWordsEnabled: Boolean = true,
    val anchorHighlightEnabled: Boolean = true,
    val sentencePauseEnabled: Boolean = true,
    val punctuationPauseEnabled: Boolean = true,
    val longWordPauseEnabled: Boolean = true,
    val complexWordPauseEnabled: Boolean = true,
    val footerEnabled: Boolean = true,
    val progressEnabled: Boolean = true,
    val estimatedTimeEnabled: Boolean = true,
    val letterSpacingEm: Float = 0f,
    val anchorColorName: String = "orange",
    val anchorDefaultRatio: Double = ReaderConfig().anchorDefaultRatio,
    val sentencePauseMs: Long = ReaderConfig().sentencePauseMs,
    val commaPauseMs: Long = ReaderConfig().commaPauseMs,
    val colonPauseMs: Long = ReaderConfig().colonPauseMs,
    val longWordThreshold: Int = ReaderConfig().longWordThreshold,
    val longWordExtraMsPerChar: Long = ReaderConfig().longWordExtraMsPerChar,
    val complexWordExtraMs: Long = ReaderConfig().complexWordExtraMs,
)

data class ReaderState(
    val bookId: String,
    val currentTokenIndex: Int = 0,
    val isPlaying: Boolean = false,
    val isLockedAutoplay: Boolean = false,
    val currentWpm: Int = ReaderConfig().defaultWpm,
    val readingMode: ReadingMode = ReadingMode.Rsvp,
    val progressPercent: Float = 0f,
    val currentChapter: String? = null,
    val estimatedBookTimeLeft: Long = 0L,
    val estimatedChapterTimeLeft: Long = 0L,
    val errorMessage: String? = null,
)
