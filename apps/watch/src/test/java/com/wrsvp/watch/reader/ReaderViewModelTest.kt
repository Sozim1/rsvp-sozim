package com.wrsvp.watch.reader

import androidx.lifecycle.SavedStateHandle
import com.wrsvp.data.ProgressRepository
import com.wrsvp.data.ReaderRepository
import com.wrsvp.data.SettingsRepository
import com.wrsvp.domain.model.PunctuationType
import com.wrsvp.domain.model.ReaderSettings
import com.wrsvp.domain.model.ReadingProgress
import com.wrsvp.domain.model.ReadingToken
import com.wrsvp.reader.AnchorCalculator
import com.wrsvp.reader.ReaderPacingEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val readerRepository = mockk<ReaderRepository>()
    private val progressRepository = mockk<ProgressRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    @Test
    fun `loads initial progress`() = runTest {
        stubReader(progressIndex = 2)

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.currentTokenIndex)
        assertEquals("palavra2", viewModel.uiState.value.currentToken?.text)
    }

    @Test
    fun `play starts autoplay`() = runTest {
        stubReader(progressIndex = 0)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.play()
        advanceTimeBy(250)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.currentTokenIndex >= 1)
    }

    @Test
    fun `pause stops autoplay and saves progress`() = runTest {
        stubReader(progressIndex = 0)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.play()
        viewModel.pause()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPlaying)
        coVerify { progressRepository.saveProgress(any()) }
    }

    @Test
    fun `next and previous word move cursor`() = runTest {
        stubReader(progressIndex = 0)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.nextWord()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.currentTokenIndex)

        viewModel.previousWord()
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.currentTokenIndex)
    }

    @Test
    fun `wpm respects max and min`() = runTest {
        stubReader(progressIndex = 0, settings = ReaderSettings(bookId = BOOK_ID, wpm = 400))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.increaseWpm()
        advanceUntilIdle()
        assertEquals(400, viewModel.uiState.value.wpm)

        repeat(40) { viewModel.decreaseWpm() }
        advanceUntilIdle()
        assertEquals(100, viewModel.uiState.value.wpm)
    }

    @Test
    fun `progress is saved`() = runTest {
        stubReader(progressIndex = 0)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.nextWord()
        advanceUntilIdle()

        coVerify { progressRepository.saveProgress(match { it.currentTokenIndex == 1 }) }
    }

    private fun viewModel(): ReaderViewModel = ReaderViewModel(
        savedStateHandle = SavedStateHandle(mapOf("bookId" to BOOK_ID)),
        readerRepository = readerRepository,
        progressRepository = progressRepository,
        settingsRepository = settingsRepository,
        pacingEngine = ReaderPacingEngine(),
        anchorCalculator = AnchorCalculator(),
    )

    private fun stubReader(
        progressIndex: Int,
        settings: ReaderSettings = ReaderSettings(bookId = BOOK_ID, wpm = 300),
    ) {
        coEvery { readerRepository.countTokens(BOOK_ID) } returns 5
        coEvery { readerRepository.getToken(BOOK_ID, any()) } answers {
            token(secondArg())
        }
        coEvery { readerRepository.getChapterTokenWindow(BOOK_ID, any(), any()) } answers {
            (0 until 5).map { token(it) }
        }
        coEvery { readerRepository.getCurrentChapterTokens(BOOK_ID, any(), any()) } answers {
            (0 until 5).map { token(it) }
        }
        coEvery { progressRepository.getProgress(BOOK_ID) } returns ReadingProgress(
            bookId = BOOK_ID,
            currentTokenIndex = progressIndex,
            currentChapterIndex = 0,
            progressPercent = progressIndex / 5f,
            lastReadAt = 1L,
            totalReadingTimeMs = 0L,
        )
        coEvery { settingsRepository.getReaderSettings(BOOK_ID) } returns settings
        coEvery { settingsRepository.saveReaderSettings(any()) } returns Unit
        coEvery { progressRepository.saveProgress(any()) } returns Unit
    }

    private fun token(index: Int): ReadingToken = ReadingToken(
        id = index.toLong(),
        text = "palavra$index",
        normalizedText = "palavra$index",
        chapterIndex = 0,
        paragraphIndex = 0,
        sentenceIndex = 0,
        wordIndex = index,
        startCharOffset = 0,
        endCharOffset = 8,
        punctuationType = PunctuationType.None,
        isSentenceEnd = false,
        complexityScore = 0,
        estimatedDurationMs = 200,
    )

    private companion object {
        const val BOOK_ID = "book-1"
    }
}
