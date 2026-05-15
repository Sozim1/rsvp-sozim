package com.wrsvp.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.wrsvp.domain.model.FontScale
import com.wrsvp.domain.model.ReaderSettings
import com.wrsvp.domain.model.ReaderTheme
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val database = inMemoryDatabase()
    private val repository = SettingsRepository(database.settingsDao())

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `saves global settings`() = runTest {
        repository.saveReaderSettings(ReaderSettings(wpm = 350))

        assertEquals(350, repository.getReaderSettings(null).wpm)
    }

    @Test
    fun `saves book settings`() = runTest {
        repository.saveReaderSettings(ReaderSettings(bookId = "book-1", wpm = 425, fontScale = FontScale.Large))

        val settings = repository.getReaderSettings("book-1")

        assertEquals(425, settings.wpm)
        assertEquals(FontScale.Large, settings.fontScale)
    }

    @Test
    fun `falls back to global settings`() = runTest {
        repository.saveReaderSettings(ReaderSettings(wpm = 375))

        assertEquals(375, repository.getReaderSettings("missing-book").wpm)
    }

    @Test
    fun `global theme and font apply over existing book settings`() = runTest {
        repository.saveReaderSettings(ReaderSettings(theme = ReaderTheme.Light, fontScale = FontScale.ExtraLarge))
        repository.saveReaderSettings(ReaderSettings(bookId = "book-1", wpm = 425, theme = ReaderTheme.Dark, fontScale = FontScale.Small))

        val settings = repository.getReaderSettings("book-1")

        assertEquals(425, settings.wpm)
        assertEquals(ReaderTheme.Light, settings.theme)
        assertEquals(FontScale.ExtraLarge, settings.fontScale)
    }

    @Test
    fun `reset returns defaults`() = runTest {
        repository.saveReaderSettings(ReaderSettings(bookId = "book-1", wpm = 500))

        repository.resetToDefaults("book-1")

        assertEquals(300, repository.getReaderSettings("book-1").wpm)
    }
}
