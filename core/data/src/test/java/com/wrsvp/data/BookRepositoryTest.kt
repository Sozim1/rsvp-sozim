package com.wrsvp.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookRepositoryTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val database = inMemoryDatabase()
    private val repository = BookRepository(database)

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `saves book with tokens`() = runTest {
        val (book, chapters, tokens) = sampleBook()

        repository.saveBookWithTokens(book, chapters, tokens)

        assertNotNull(repository.getBook(book.id))
        assertEquals(tokens.size, database.tokenDao().countTokensByBookId(book.id))
        assertEquals(1, database.chapterDao().getChaptersByBookId(book.id).size)
    }

    @Test
    fun `lists books`() = runTest {
        val (book, chapters, tokens) = sampleBook()
        repository.saveBookWithTokens(book, chapters, tokens)

        repository.observeBooks().test {
            assertEquals(listOf(book.id), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deletes book and related rows`() = runTest {
        val (book, chapters, tokens) = sampleBook()
        repository.saveBookWithTokens(book, chapters, tokens)

        repository.deleteBook(book.id)

        assertNull(repository.getBook(book.id))
        assertEquals(0, database.tokenDao().countTokensByBookId(book.id))
    }

    @Test
    fun `ensures demo book only once`() = runTest {
        repository.ensureDemoBookExists()
        repository.ensureDemoBookExists()

        assertEquals(1, repository.countBooks())
    }
}
