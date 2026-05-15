package com.wrsvp.reader

import com.wrsvp.domain.model.PunctuationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextTokenizerTest {
    private val tokenizer = TextTokenizer()

    @Test
    fun `tokenizes simple portuguese text`() {
        val tokens = tokenizer.tokenize("O relogio mostra uma palavra por vez.")

        assertEquals(listOf("O", "relogio", "mostra", "uma", "palavra", "por", "vez."), tokens.map { it.text })
        assertEquals(0, tokens.first().paragraphIndex)
        assertTrue(tokens.last().isSentenceEnd)
    }

    @Test
    fun `detects punctuation and sentence breaks`() {
        val tokens = tokenizer.tokenize("Calma, leia: agora! Depois?")

        assertEquals(PunctuationType.Comma, tokens[0].punctuationType)
        assertEquals(PunctuationType.Colon, tokens[1].punctuationType)
        assertTrue(tokens[2].isSentenceEnd)
        assertTrue(tokens[3].isSentenceEnd)
    }

    @Test
    fun `detects paragraph breaks`() {
        val tokens = tokenizer.tokenize("Primeiro paragrafo.\n\nSegundo paragrafo.")

        assertEquals(0, tokens[0].paragraphIndex)
        assertEquals(1, tokens[2].paragraphIndex)
    }

    @Test
    fun `preserves accents and hyphenated words`() {
        val tokens = tokenizer.tokenize("Atenção ao bem-estar.")

        assertEquals("Atenção", tokens[0].text)
        assertEquals("bem-estar.", tokens[2].text)
        assertTrue(tokens[0].complexityScore > 0)
    }

    @Test
    fun `strips basic markdown`() {
        val tokens = tokenizer.tokenize("# Titulo\n\nLeia **muito** [agora](https://example.com).")

        assertEquals(listOf("Titulo", "Leia", "muito", "agora."), tokens.map { it.text })
    }

    @Test
    fun `strips html tags`() {
        val tokens = tokenizer.tokenize("<h1>Titulo</h1><p>Leia <strong>agora</strong>.</p>")

        assertEquals(listOf("titulo", "leia", "agora"), tokens.map { it.normalizedText })
    }
}
