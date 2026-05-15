package com.wrsvp.reader

import com.wrsvp.domain.model.ReaderConfig
import com.wrsvp.domain.model.ReadingMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderControllerTest {
    @Test
    fun `play and pause update state`() {
        val controller = controller()

        controller.play()
        assertTrue(controller.state.value.isPlaying)

        controller.pause()
        assertFalse(controller.state.value.isPlaying)
    }

    @Test
    fun `next and previous word move cursor`() {
        val controller = controller()

        controller.nextWord()
        controller.nextWord()
        controller.previousWord()

        assertEquals(1, controller.state.value.currentTokenIndex)
    }

    @Test
    fun `wpm respects min and max`() {
        val controller = controller(config = ReaderConfig(wpmStep = 1000))

        controller.increaseWpm()
        assertEquals(900, controller.state.value.currentWpm)

        controller.decreaseWpm()
        assertEquals(100, controller.state.value.currentWpm)
    }

    @Test
    fun `jump to chapter changes token`() {
        val controller = controller(chapters = mapOf("capitulo-2" to 30))

        controller.jumpToChapter("capitulo-2")

        assertEquals(30, controller.state.value.currentTokenIndex)
        assertEquals("capitulo-2", controller.state.value.currentChapter)
    }

    @Test
    fun `switch reading mode toggles mode`() {
        val controller = controller()

        controller.switchReadingMode()

        assertEquals(ReadingMode.PageScroll, controller.state.value.readingMode)
    }

    @Test
    fun `progress is saved and restored`() = runTest {
        val store = InMemoryProgressStore()
        val first = controller(progressStore = store)
        first.jumpToToken(42)
        first.saveProgress()

        val second = controller(progressStore = store)
        second.restoreProgress()

        assertEquals(42, second.state.value.currentTokenIndex)
    }

    private fun controller(
        config: ReaderConfig = ReaderConfig(),
        chapters: Map<String, Int> = emptyMap(),
        progressStore: ProgressStore = InMemoryProgressStore(),
    ) = ReaderController(
        bookId = "book-1",
        totalTokens = 100,
        chapterStartTokens = chapters,
        progressStore = progressStore,
        config = config,
    )
}
