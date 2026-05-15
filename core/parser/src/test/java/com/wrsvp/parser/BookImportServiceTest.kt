package com.wrsvp.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookImportServiceTest {
    private val service = BookImportService()

    @Test
    fun `rejects invalid file`() {
        val result = runCatching { service.importText("texto", "book.pdf", "pdf") }

        assertTrue(result.exceptionOrNull() is BookParseException)
    }

    @Test
    fun `generates checksum and tokens`() {
        val imported = service.importText("Titulo\n\nUma frase simples.", "book.txt")

        assertEquals("Titulo", imported.book.title)
        assertTrue(imported.book.checksum.isNotBlank())
        assertTrue(imported.tokens.isNotEmpty())
    }
}
