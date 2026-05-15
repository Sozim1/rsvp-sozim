package com.wrsvp.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {
    @Test
    fun `imports markdown title`() {
        val parsed = MarkdownParser().parse("book.md", "# Meu Livro\n\nTexto **forte** com [link](https://x.test).")

        assertEquals("Meu Livro", parsed.title)
        assertTrue(parsed.plainText.contains("Texto forte com link."))
    }

    @Test
    fun `imports markdown chapters`() {
        val parsed = MarkdownParser().parse("book.md", "# Livro\n\n## Um\nTexto\n\n## Dois\nTexto")

        assertEquals(listOf("Um", "Dois"), parsed.chapters.map { it.title })
    }
}
