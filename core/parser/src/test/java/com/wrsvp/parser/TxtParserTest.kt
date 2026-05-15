package com.wrsvp.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class TxtParserTest {
    @Test
    fun `imports simple txt`() {
        val parsed = TxtParser().parse("livro.txt", "Titulo\n\nCorpo do texto.")

        assertEquals("Titulo", parsed.title)
        assertEquals("txt", parsed.originalFileType)
        assertEquals("Inicio", parsed.chapters.single().title)
    }
}
