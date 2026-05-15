package com.wrsvp.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlParserTest {
    @Test
    fun `imports html with tags`() {
        val parsed = HtmlParser().parse(
            "book.html",
            "<html><head><title>Titulo HTML</title></head><body><h2>Capitulo</h2><p>Leia &amp; teste.</p></body></html>",
        )

        assertEquals("Titulo HTML", parsed.title)
        assertEquals("Capitulo", parsed.chapters.single().title)
        assertTrue(parsed.plainText.contains("Leia & teste."))
    }
}
