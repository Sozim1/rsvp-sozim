package com.wrsvp.watch.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wrsvp.data.SettingsRepository
import com.wrsvp.domain.model.FontScale
import com.wrsvp.domain.model.ReaderSettings
import com.wrsvp.domain.model.ReaderTheme
import com.wrsvp.domain.model.ReadingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: ReaderSettings = ReaderSettings(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val bookId: String? = savedStateHandle["bookId"]
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings(bookId)
    }

    fun observeSettings(bookId: String?) {
        viewModelScope.launch {
            _uiState.value = SettingsUiState(settingsRepository.getReaderSettings(bookId), isLoading = false)
        }
    }

    fun updateWpm(value: Int) = update { it.copy(wpm = value.coerceIn(MIN_WPM, MAX_WPM)) }
    fun increaseWpm() = updateWpm(_uiState.value.settings.wpm + WPM_STEP)
    fun decreaseWpm() = updateWpm(_uiState.value.settings.wpm - WPM_STEP)
    fun updateFontSize(value: FontScale) = update { it.copy(fontScale = value) }
    fun updateTheme(value: ReaderTheme) = update { it.copy(theme = value) }
    fun updateReadingMode(value: ReadingMode) = update { it.copy(readingMode = value) }
    fun updatePhantomWords(enabled: Boolean) = update { it.copy(phantomWordsEnabled = enabled) }
    fun updateAnchorHighlight(enabled: Boolean) = update { it.copy(anchorHighlightEnabled = enabled) }
    fun updateSentencePause(enabled: Boolean) = update { it.copy(sentencePauseEnabled = enabled) }
    fun updatePunctuationPause(enabled: Boolean) = update { it.copy(punctuationPauseEnabled = enabled) }
    fun updateLongWordPause(enabled: Boolean) = update { it.copy(longWordPauseEnabled = enabled) }
    fun updateComplexWordPause(enabled: Boolean) = update { it.copy(complexWordPauseEnabled = enabled) }
    fun updateFooter(enabled: Boolean) = update { it.copy(footerEnabled = enabled) }
    fun updateProgress(enabled: Boolean) = update { it.copy(progressEnabled = enabled) }
    fun updateEstimatedTime(enabled: Boolean) = update { it.copy(estimatedTimeEnabled = enabled) }
    fun updateLetterSpacing(value: Float) = update { it.copy(letterSpacingEm = value.coerceIn(0f, 0.2f)) }
    fun updateAnchorRatio(value: Double) = update { it.copy(anchorDefaultRatio = value.coerceIn(0.2, 0.7)) }
    fun updateAnchorColor(value: String) = update { it.copy(anchorColorName = value) }
    fun updateCommaPause(value: Long) = update { it.copy(commaPauseMs = value.coerceIn(0, 800)) }
    fun updateColonPause(value: Long) = update { it.copy(colonPauseMs = value.coerceIn(0, 800)) }
    fun updateSentencePauseMs(value: Long) = update { it.copy(sentencePauseMs = value.coerceIn(0, 1500)) }
    fun updateLongWordDelay(value: Long) = update { it.copy(longWordExtraMsPerChar = value.coerceIn(0, 80)) }
    fun updateComplexWordDelay(value: Long) = update { it.copy(complexWordExtraMs = value.coerceIn(0, 500)) }

    fun resetToDefaults(bookId: String? = this.bookId) {
        viewModelScope.launch {
            settingsRepository.resetToDefaults(bookId)
            _uiState.value = SettingsUiState(settingsRepository.getReaderSettings(bookId), isLoading = false)
        }
    }

    private fun update(block: (ReaderSettings) -> ReaderSettings) {
        viewModelScope.launch {
            val next = block(_uiState.value.settings)
            settingsRepository.saveReaderSettings(next)
            _uiState.value = SettingsUiState(next, isLoading = false)
        }
    }

    private companion object {
        const val MIN_WPM = 100
        const val MAX_WPM = 400
        const val WPM_STEP = 25
    }
}
