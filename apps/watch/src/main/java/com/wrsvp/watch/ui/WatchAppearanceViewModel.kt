package com.wrsvp.watch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wrsvp.data.SettingsRepository
import com.wrsvp.domain.model.ReaderTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchAppearanceViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _theme = MutableStateFlow(ReaderTheme.Light)
    val theme: StateFlow<ReaderTheme> = _theme.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeGlobalReaderSettings().collect { settings ->
                _theme.value = settings.theme
            }
        }
    }
}
