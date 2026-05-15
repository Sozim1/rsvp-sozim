package com.wrsvp.watch.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wrsvp.data.ProgressRepository
import com.wrsvp.data.ReaderRepository
import com.wrsvp.data.SettingsRepository
import com.wrsvp.domain.model.FontScale
import com.wrsvp.domain.model.ReaderConfig
import com.wrsvp.domain.model.ReaderSettings
import com.wrsvp.domain.model.ReaderTheme
import com.wrsvp.domain.model.ReadingMode
import com.wrsvp.domain.model.ReadingProgress
import com.wrsvp.domain.model.ReadingToken
import com.wrsvp.reader.AnchorCalculator
import com.wrsvp.reader.AnchorParts
import com.wrsvp.reader.ReaderPacingEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderUiState(
    val bookId: String,
    val isLoading: Boolean = true,
    val currentToken: ReadingToken? = null,
    val previousToken: ReadingToken? = null,
    val nextToken: ReadingToken? = null,
    val anchorParts: AnchorParts = AnchorParts("", "", "", 0),
    val currentTokenIndex: Int = 0,
    val totalTokens: Int = 0,
    val currentChapterIndex: Int = 0,
    val isPlaying: Boolean = false,
    val wpm: Int = ReaderConfig().defaultWpm,
    val progressPercent: Float = 0f,
    val readingMode: ReadingMode = ReadingMode.Rsvp,
    val theme: ReaderTheme = ReaderTheme.Dark,
    val fontScale: FontScale = FontScale.Medium,
    val contextOverlayEnabled: Boolean = true,
    val pageText: String = "",
    val pageStartTokenIndex: Int = 0,
    val pageTokenCount: Int = 0,
    val errorMessage: String? = null,
) {
    val progressLabel: String
        get() = "${(progressPercent * 100).toInt()}%"
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val readerRepository: ReaderRepository,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val pacingEngine: ReaderPacingEngine,
    private val anchorCalculator: AnchorCalculator,
) : ViewModel() {
    private val bookId: String = checkNotNull(savedStateHandle["bookId"])
    private val config = ReaderConfig()
    private var settings = ReaderSettings(bookId = bookId)
    private var autoplayJob: Job? = null
    private var wordsSinceLastSave = 0
    private var readingStartedAt = System.currentTimeMillis()

    private val _uiState = MutableStateFlow(ReaderUiState(bookId = bookId))
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadInitialState()
        }
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) pause() else play()
    }

    fun play() {
        if (_uiState.value.isPlaying || _uiState.value.totalTokens <= 0) return
        readingStartedAt = System.currentTimeMillis()
        _uiState.update { it.copy(isPlaying = true) }
        autoplayJob?.cancel()
        autoplayJob = viewModelScope.launch {
            while (_uiState.value.isPlaying) {
                val token = _uiState.value.currentToken ?: break
                val duration = pacingEngine.durationMs(token, settings.copy(wpm = _uiState.value.wpm), config)
                delay(duration)
                if (_uiState.value.isPlaying) {
                    nextWord(fromAutoplay = true)
                }
            }
        }
    }

    fun pause() {
        autoplayJob?.cancel()
        autoplayJob = null
        _uiState.update { it.copy(isPlaying = false) }
        viewModelScope.launch { saveProgress() }
    }

    fun nextWord(fromAutoplay: Boolean = false) {
        val state = _uiState.value
        if (state.totalTokens <= 0) return
        val nextIndex = state.currentTokenIndex + 1
        if (nextIndex > state.totalTokens - 1) {
            pause()
            return
        }
        viewModelScope.launch {
            loadToken(nextIndex)
            wordsSinceLastSave += 1
            if (!fromAutoplay || wordsSinceLastSave >= config.autoSaveProgressEveryWords) {
                saveProgress()
                wordsSinceLastSave = 0
            }
        }
    }

    fun previousWord() {
        val previous = (_uiState.value.currentTokenIndex - 1).coerceAtLeast(0)
        viewModelScope.launch {
            loadToken(previous)
            saveProgress()
        }
    }

    fun skipWords(delta: Int) {
        val state = _uiState.value
        if (state.totalTokens <= 0 || delta == 0) return
        val target = (state.currentTokenIndex + delta).coerceIn(0, state.totalTokens - 1)
        if (target == state.currentTokenIndex) return
        viewModelScope.launch {
            loadToken(target)
            saveProgress()
        }
    }

    fun increaseWpm() {
        val next = (_uiState.value.wpm + config.wpmStep).coerceAtMost(MAX_WPM)
        _uiState.update { it.copy(wpm = next) }
        viewModelScope.launch {
            settings = settings.copy(wpm = next)
            settingsRepository.saveReaderSettings(settings)
            saveProgress()
        }
    }

    fun decreaseWpm() {
        val next = (_uiState.value.wpm - config.wpmStep).coerceAtLeast(MIN_WPM)
        _uiState.update { it.copy(wpm = next) }
        viewModelScope.launch {
            settings = settings.copy(wpm = next)
            settingsRepository.saveReaderSettings(settings)
            saveProgress()
        }
    }

    override fun onCleared() {
        autoplayJob?.cancel()
        viewModelScope.launch { saveProgress() }
        super.onCleared()
    }

    private suspend fun loadInitialState() {
        val total = readerRepository.countTokens(bookId)
        settings = settingsRepository.getReaderSettings(bookId)
        val progress = progressRepository.getProgress(bookId)
        val startIndex = progress?.currentTokenIndex?.coerceIn(0, (total - 1).coerceAtLeast(0)) ?: 0
        _uiState.update {
            it.copy(
                isLoading = false,
                totalTokens = total,
                currentTokenIndex = startIndex,
                currentChapterIndex = progress?.currentChapterIndex ?: 0,
                wpm = settings.wpm.coerceIn(MIN_WPM, MAX_WPM),
                readingMode = settings.readingMode,
                theme = settings.theme,
                fontScale = settings.fontScale,
                contextOverlayEnabled = settings.phantomWordsEnabled,
            )
        }
        loadToken(startIndex)
    }

    private suspend fun loadToken(tokenIndex: Int) {
        val total = _uiState.value.totalTokens
        val boundedIndex = tokenIndex.coerceIn(0, (total - 1).coerceAtLeast(0))
        val token = readerRepository.getToken(bookId, boundedIndex)
        if (token == null) {
            _uiState.update { it.copy(errorMessage = "Palavra nao encontrada", isPlaying = false) }
            return
        }
        val progress = if (total <= 0) 0f else boundedIndex.toFloat() / total.toFloat()
        val previous = if (boundedIndex > 0) readerRepository.getToken(bookId, boundedIndex - 1) else null
        val next = if (boundedIndex < total - 1) readerRepository.getToken(bookId, boundedIndex + 1) else null
        val pageTokens = readerRepository.getCurrentChapterTokens(bookId, boundedIndex)
        _uiState.update {
            it.copy(
                currentToken = token,
                previousToken = previous,
                nextToken = next,
                anchorParts = anchorCalculator.calculate(token.text, settings.anchorDefaultRatio),
                currentTokenIndex = boundedIndex,
                currentChapterIndex = token.chapterIndex,
                progressPercent = progress,
                pageText = pageTokens.joinToString(" ") { tokenInWindow -> tokenInWindow.text },
                pageStartTokenIndex = pageTokens.firstOrNull()?.id?.toInt() ?: boundedIndex,
                pageTokenCount = pageTokens.size,
                errorMessage = null,
            )
        }
    }

    fun jumpWithinPage(ratio: Float) {
        val state = _uiState.value
        val offset = (state.pageTokenCount * ratio.coerceIn(0f, 1f)).toInt()
        jumpToToken(state.pageStartTokenIndex + offset)
    }

    private fun jumpToToken(index: Int) {
        viewModelScope.launch {
            loadToken(index)
            saveProgress()
        }
    }

    fun switchReadingMode() {
        val next = if (_uiState.value.readingMode == ReadingMode.Rsvp) ReadingMode.PageScroll else ReadingMode.Rsvp
        _uiState.update { it.copy(readingMode = next) }
        viewModelScope.launch {
            settings = settings.copy(readingMode = next)
            settingsRepository.saveReaderSettings(settings)
            saveProgress()
        }
    }

    fun toggleContextOverlay() {
        val next = !_uiState.value.contextOverlayEnabled
        _uiState.update { it.copy(contextOverlayEnabled = next) }
        viewModelScope.launch {
            settings = settings.copy(phantomWordsEnabled = next)
            settingsRepository.saveReaderSettings(settings)
        }
    }

    fun toggleTheme() {
        val next = if (_uiState.value.theme == ReaderTheme.Light) ReaderTheme.Dark else ReaderTheme.Light
        _uiState.update { it.copy(theme = next) }
        viewModelScope.launch {
            settings = settings.copy(theme = next)
            val globalSettings = settingsRepository.getReaderSettings(null)
            settingsRepository.saveReaderSettings(globalSettings.copy(theme = next))
        }
    }

    fun increaseFontScale() {
        updateFontScale(
            when (_uiState.value.fontScale) {
                FontScale.Small -> FontScale.Medium
                FontScale.Medium -> FontScale.Large
                FontScale.Large -> FontScale.ExtraLarge
                FontScale.ExtraLarge -> FontScale.ExtraLarge
            },
        )
    }

    fun decreaseFontScale() {
        updateFontScale(
            when (_uiState.value.fontScale) {
                FontScale.Small -> FontScale.Small
                FontScale.Medium -> FontScale.Small
                FontScale.Large -> FontScale.Medium
                FontScale.ExtraLarge -> FontScale.Large
            },
        )
    }

    private fun updateFontScale(next: FontScale) {
        _uiState.update { it.copy(fontScale = next) }
        viewModelScope.launch {
            settings = settings.copy(fontScale = next)
            settingsRepository.saveReaderSettings(settings)
        }
    }

    private suspend fun saveProgress() {
        val state = _uiState.value
        if (state.totalTokens <= 0) return
        val elapsed = (System.currentTimeMillis() - readingStartedAt).coerceAtLeast(0)
        val now = System.currentTimeMillis()
        val progress = ReadingProgress(
            bookId = bookId,
            currentTokenIndex = state.currentTokenIndex,
            currentChapterIndex = state.currentChapterIndex,
            progressPercent = state.progressPercent,
            lastReadAt = now,
            totalReadingTimeMs = elapsed,
        )
        progressRepository.saveProgress(progress)
    }

    private companion object {
        const val MIN_WPM = 100
        const val MAX_WPM = 400
    }
}
