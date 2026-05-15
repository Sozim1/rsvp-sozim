package com.wrsvp.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.wrsvp.domain.model.ReadingProgress
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProgressRepositoryTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val database = inMemoryDatabase()
    private val repository = ProgressRepository(database.progressDao())

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `saves and restores progress`() = runTest {
        val progress = progress(index = 3)

        repository.saveProgress(progress)

        assertEquals(3, repository.getProgress("book-1")?.currentTokenIndex)
    }

    @Test
    fun `overwrites existing progress`() = runTest {
        repository.saveProgress(progress(index = 3))
        repository.saveProgress(progress(index = 8))

        assertEquals(8, repository.getProgress("book-1")?.currentTokenIndex)
    }

    @Test
    fun `observes progress`() = runTest {
        repository.observeProgress("book-1").test {
            assertEquals(null, awaitItem())
            repository.saveProgress(progress(index = 5))
            assertEquals(5, awaitItem()?.currentTokenIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun progress(index: Int): ReadingProgress = ReadingProgress(
        bookId = "book-1",
        currentTokenIndex = index,
        currentChapterIndex = 0,
        progressPercent = index / 10f,
        lastReadAt = index.toLong(),
        totalReadingTimeMs = 0L,
    )
}
