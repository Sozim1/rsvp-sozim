package com.wrsvp.reader

import com.wrsvp.domain.model.PunctuationType
import com.wrsvp.domain.model.ReaderSettings
import com.wrsvp.domain.model.ReadingToken
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderPacingEngineTest {
    private val engine = ReaderPacingEngine()

    @Test
    fun `wpm 300 returns base duration`() {
        assertEquals(200, engine.durationMs(token("casa"), ReaderSettings(wpm = 300)))
    }

    @Test
    fun `long word keeps constant duration`() {
        val duration = engine.durationMs(token("extraordinariamente"))

        assertEquals(200, duration)
    }

    @Test
    fun `comma keeps constant duration`() {
        val duration = engine.durationMs(token("calma,", punctuationType = PunctuationType.Comma))

        assertEquals(200, duration)
    }

    @Test
    fun `sentence end keeps constant duration`() {
        val duration = engine.durationMs(token("fim.", punctuationType = PunctuationType.SentenceEnd, isSentenceEnd = true))

        assertEquals(200, duration)
    }

    @Test
    fun `disabled adjustments do not add delay`() {
        val settings = ReaderSettings(
            punctuationPauseEnabled = false,
            longWordPauseEnabled = false,
            complexWordPauseEnabled = false,
        )

        assertEquals(200, engine.durationMs(token("extraordinariamente,", PunctuationType.Comma, complexityScore = 3), settings))
    }

    @Test
    fun `wpm is bounded`() {
        assertEquals(100, engine.boundedWpm(50))
        assertEquals(900, engine.boundedWpm(1000))
    }

    private fun token(
        text: String,
        punctuationType: PunctuationType = PunctuationType.None,
        isSentenceEnd: Boolean = false,
        complexityScore: Int = 0,
    ) = ReadingToken(
        id = 0,
        text = text,
        normalizedText = text.trim { it in ".,;:!?" }.lowercase(),
        chapterIndex = 0,
        paragraphIndex = 0,
        sentenceIndex = 0,
        wordIndex = 0,
        startCharOffset = 0,
        endCharOffset = text.length,
        punctuationType = punctuationType,
        isSentenceEnd = isSentenceEnd,
        complexityScore = complexityScore,
        estimatedDurationMs = 0,
    )
}
