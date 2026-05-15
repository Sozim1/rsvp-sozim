package com.wrsvp.reader

import com.wrsvp.domain.model.ReaderConfig
import com.wrsvp.domain.model.ReaderState
import com.wrsvp.domain.model.ReaderSettings
import com.wrsvp.domain.model.ReadingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface ProgressStore {
    suspend fun save(state: ReaderState)
    suspend fun restore(bookId: String): ReaderState?
}

class InMemoryProgressStore : ProgressStore {
    private val states = mutableMapOf<String, ReaderState>()

    override suspend fun save(state: ReaderState) {
        states[state.bookId] = state
    }

    override suspend fun restore(bookId: String): ReaderState? = states[bookId]
}

class ReaderController(
    bookId: String,
    private val totalTokens: Int,
    private val chapterStartTokens: Map<String, Int> = emptyMap(),
    private val settings: ReaderSettings = ReaderSettings(),
    private val progressStore: ProgressStore = InMemoryProgressStore(),
    private val config: ReaderConfig = ReaderConfig(),
) {
    private val _state = MutableStateFlow(
        ReaderState(
            bookId = bookId,
            currentWpm = settings.wpm.coerceIn(settings.minWpm, settings.maxWpm),
            readingMode = settings.readingMode,
        ).withProgress(),
    )
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    fun play() = _state.update { it.copy(isPlaying = true, errorMessage = null) }

    fun pause() = _state.update { it.copy(isPlaying = false, isLockedAutoplay = false) }

    fun pauseAtSentenceEnd() = _state.update { it.copy(isPlaying = false) }

    fun toggleLockedAutoplay() = _state.update {
        it.copy(isLockedAutoplay = !it.isLockedAutoplay, isPlaying = !it.isLockedAutoplay)
    }

    fun nextWord() = _state.update {
        it.copy(currentTokenIndex = (it.currentTokenIndex + 1).coerceAtMost(lastTokenIndex())).withProgress()
    }

    fun previousWord() = _state.update {
        it.copy(currentTokenIndex = (it.currentTokenIndex - 1).coerceAtLeast(0)).withProgress()
    }

    fun jumpToToken(index: Int) = _state.update {
        it.copy(currentTokenIndex = index.coerceIn(0, lastTokenIndex())).withProgress()
    }

    fun jumpToChapter(chapterId: String) = _state.update {
        val start = chapterStartTokens[chapterId]
        if (start == null) {
            it.copy(errorMessage = "Chapter not found: $chapterId")
        } else {
            it.copy(currentTokenIndex = start.coerceIn(0, lastTokenIndex()), currentChapter = chapterId).withProgress()
        }
    }

    fun increaseWpm() = _state.update {
        it.copy(currentWpm = (it.currentWpm + config.wpmStep).coerceAtMost(settings.maxWpm))
    }

    fun decreaseWpm() = _state.update {
        it.copy(currentWpm = (it.currentWpm - config.wpmStep).coerceAtLeast(settings.minWpm))
    }

    fun switchReadingMode() = _state.update {
        val next = if (it.readingMode == ReadingMode.Rsvp) ReadingMode.PageScroll else ReadingMode.Rsvp
        it.copy(readingMode = next)
    }

    suspend fun saveProgress() {
        progressStore.save(_state.value)
    }

    suspend fun restoreProgress() {
        val restored = progressStore.restore(_state.value.bookId) ?: return
        _state.value = restored.withProgress()
    }

    private fun ReaderState.withProgress(): ReaderState {
        val progress = if (totalTokens <= 0) 0f else currentTokenIndex.toFloat() / totalTokens.toFloat()
        val remainingWords = (totalTokens - currentTokenIndex).coerceAtLeast(0)
        val estimatedMs = remainingWords * (60_000L / currentWpm.coerceAtLeast(1))
        return copy(progressPercent = progress, estimatedBookTimeLeft = estimatedMs)
    }

    private fun lastTokenIndex(): Int = (totalTokens - 1).coerceAtLeast(0)
}
