package com.wrsvp.watch.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wrsvp.data.BookRepository
import com.wrsvp.data.ProgressRepository
import com.wrsvp.domain.model.Book
import com.wrsvp.watch.demo.DemoBookSeeder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryBookUi(
    val book: Book,
    val progressPercent: Float,
)

data class LibraryUiState(
    val isLoading: Boolean = true,
    val books: List<LibraryBookUi> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val progressRepository: ProgressRepository,
    private val demoBookSeeder: DemoBookSeeder,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            demoBookSeeder.ensureSeeded()
            bookRepository.observeBooks()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = throwable.message ?: "Erro ao carregar biblioteca")
                    }
                }
                .collect { books ->
                    val uiBooks = books.map { book ->
                        val progress = progressRepository.getProgress(book.id)
                        LibraryBookUi(book = book, progressPercent = progress?.progressPercent ?: 0f)
                    }
                    _uiState.value = LibraryUiState(isLoading = false, books = uiBooks)
                }
        }
    }
}
