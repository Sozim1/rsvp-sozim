package com.wrsvp.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EpubParserTest {
    @Test
    fun `imports minimal valid epub`() {
        val parsed = EpubParser().parse("book.epub", ByteArrayInputStream(epubBytes(author = "Autor")))

        assertEquals("Livro EPUB", parsed.title)
        assertEquals("Autor", parsed.author)
        assertTrue(parsed.plainText.contains("Texto do capitulo"))
    }

    @Test
    fun `imports epub without author`() {
        val parsed = EpubParser().parse("book.epub", ByteArrayInputStream(epubBytes(author = null)))

        assertEquals(null, parsed.author)
    }

    @Test
    fun `uses nav xhtml chapter titles`() {
        val parsed = EpubParser().parse("book.epub", ByteArrayInputStream(epubBytes(author = "Autor", includeNav = true)))

        assertEquals("Titulo do Nav", parsed.chapters.first().title)
    }

    @Test
    fun `uses ncx chapter titles`() {
        val parsed = EpubParser().parse("book.epub", ByteArrayInputStream(epubBytes(author = "Autor", includeNcx = true)))

        assertEquals("Titulo do NCX", parsed.chapters.first().title)
    }

    @Test
    fun `decodes declared latin1 encoding`() {
        val parsed = EpubParser().parse("book.epub", ByteArrayInputStream(epubBytes(author = "Autor", latin1 = true)))

        assertTrue(parsed.plainText.contains("ação"))
    }

    @Test
    fun `fails invalid epub`() {
        val result = runCatching { EpubParser().parse("bad.epub", ByteArrayInputStream("bad".toByteArray())) }

        assertTrue(result.exceptionOrNull() is BookParseException)
    }

    private fun epubBytes(
        author: String?,
        includeNav: Boolean = false,
        includeNcx: Boolean = false,
        latin1: Boolean = false,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.put("META-INF/container.xml", """<container><rootfiles><rootfile full-path="OPS/content.opf"/></rootfiles></container>""")
            val manifestExtras = buildString {
                if (includeNav) append("""<item id="nav" href="nav.xhtml"/>""")
                if (includeNcx) append("""<item id="ncx" href="toc.ncx"/>""")
            }
            zip.put(
                "OPS/content.opf",
                """
                <package>
                  <metadata>
                    <dc:title>Livro EPUB</dc:title>
                    ${author?.let { "<dc:creator>$it</dc:creator>" } ?: ""}
                    <dc:language>pt-BR</dc:language>
                  </metadata>
                  <manifest><item id="c1" href="c1.xhtml"/>$manifestExtras</manifest>
                  <spine><itemref idref="c1"/></spine>
                </package>
                """.trimIndent(),
            )
            if (includeNav) {
                zip.put("OPS/nav.xhtml", """<html><body><nav><ol><li><a href="c1.xhtml">Titulo do Nav</a></li></ol></nav></body></html>""")
            }
            if (includeNcx) {
                zip.put("OPS/toc.ncx", """<ncx><navMap><navPoint><navLabel><text>Titulo do NCX</text></navLabel><content src="c1.xhtml"/></navPoint></navMap></ncx>""")
            }
            val html = if (latin1) {
                """<?xml version="1.0" encoding="ISO-8859-1"?><html><body><h1>Capitulo 1</h1><p>Texto com ação.</p></body></html>"""
            } else {
                "<html><body><h1>Capitulo 1</h1><p>Texto do capitulo.</p></body></html>"
            }
            zip.put("OPS/c1.xhtml", html, if (latin1) Charsets.ISO_8859_1 else Charsets.UTF_8)
        }
        return output.toByteArray()
    }

    private fun ZipOutputStream.put(name: String, content: String, charset: java.nio.charset.Charset = Charsets.UTF_8) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(charset))
        closeEntry()
    }
}
