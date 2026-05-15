package com.wrsvp.watch.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wrsvp.data.BookRepository
import com.wrsvp.data.ProgressRepository
import com.wrsvp.domain.model.Book
import com.wrsvp.domain.model.ReadingProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookDetailsUiState(
    val isLoading: Boolean = true,
    val book: Book? = null,
    val progress: ReadingProgress? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class BookDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {
    private val bookId: String = checkNotNull(savedStateHandle["bookId"])
    private val _uiState = MutableStateFlow(BookDetailsUiState())
    val uiState: StateFlow<BookDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    fun deleteBook(onDeleted: () -> Unit) {
        viewModelScope.launch {
            bookRepository.deleteBook(bookId)
            onDeleted()
        }
    }

    private suspend fun load() {
        runCatching {
            _uiState.value = BookDetailsUiState(
                isLoading = false,
                book = bookRepository.getBook(bookId),
                progress = progressRepository.getProgress(bookId),
            )
        }.onFailure { throwable ->
            _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message ?: "Erro ao abrir detalhes") }
        }
    }
}
