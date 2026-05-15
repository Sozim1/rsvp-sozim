package com.wrsvp.watch.chapters

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wrsvp.data.BookRepository
import com.wrsvp.data.ProgressRepository
import com.wrsvp.domain.model.Chapter
import com.wrsvp.domain.model.ReadingProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChapterUi(
    val chapter: Chapter,
    val isCurrent: Boolean,
    val progressPercent: Float,
)

data class ChaptersUiState(
    val isLoading: Boolean = true,
    val bookId: String = "",
    val chapters: List<ChapterUi> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class ChaptersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {
    private val bookId: String = checkNotNull(savedStateHandle["bookId"])
    private val _uiState = MutableStateFlow(ChaptersUiState(bookId = bookId))
    val uiState: StateFlow<ChaptersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    fun selectChapter(chapter: Chapter, onSelected: () -> Unit) {
        viewModelScope.launch {
            val totalWords = bookRepository.getBook(bookId)?.totalWords?.coerceAtLeast(1) ?: 1
            progressRepository.saveProgress(
                ReadingProgress(
                    bookId = bookId,
                    currentTokenIndex = chapter.startTokenIndex,
                    currentChapterIndex = chapter.index,
                    progressPercent = chapter.startTokenIndex.toFloat() / totalWords.toFloat(),
                    lastReadAt = System.currentTimeMillis(),
                    totalReadingTimeMs = progressRepository.getProgress(bookId)?.totalReadingTimeMs ?: 0L,
                ),
            )
            onSelected()
        }
    }

    private suspend fun load() {
        runCatching {
            val progress = progressRepository.getProgress(bookId)
            val chapters = bookRepository.getChapters(bookId).ifEmpty {
                listOf(Chapter("${bookId}-0", bookId, 0, "Inicio", 0, 0))
            }
            _uiState.value = ChaptersUiState(
                isLoading = false,
                bookId = bookId,
                chapters = chapters.map { chapter ->
                    val size = (chapter.endTokenIndex - chapter.startTokenIndex + 1).coerceAtLeast(1)
                    val read = ((progress?.currentTokenIndex ?: 0) - chapter.startTokenIndex).coerceIn(0, size)
                    ChapterUi(
                        chapter = chapter.copy(title = chapter.title.ifBlank { "Capitulo ${chapter.index + 1}" }),
                        isCurrent = progress?.currentChapterIndex == chapter.index,
                        progressPercent = read.toFloat() / size.toFloat(),
                    )
                },
            )
        }.onFailure { throwable ->
            _uiState.update {
                it.copy(isLoading = false, errorMessage = throwable.message ?: "Nao foi possivel carregar capitulos")
            }
        }
    }
}
