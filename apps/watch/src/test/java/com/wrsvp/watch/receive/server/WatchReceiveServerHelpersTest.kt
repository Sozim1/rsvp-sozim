package com.wrsvp.watch.receive.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchReceiveServerHelpersTest {
    @Test
    fun `pairing code has six digits`() {
        val code = PairingCodeGenerator().generate()

        assertEquals(6, code.length)
        assertTrue(code.all(Char::isDigit))
    }

    @Test
    fun `pairing code changes across generations`() {
        val generator = PairingCodeGenerator()

        val first = generator.generate()
        val second = generator.generate()

        assertNotEquals(first, second)
    }

    @Test
    fun `multipart parser reads code and uploaded file`() {
        val boundary = "abc123"
        val body = """
            --$boundary
            Content-Disposition: form-data; name="code"

            482913
            --$boundary
            Content-Disposition: form-data; name="file"; filename="livro.txt"
            Content-Type: text/plain

            Ola mundo
            --$boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray(Charsets.ISO_8859_1)

        val parsed = MultipartRequestParser.parse(body, boundary)

        assertEquals("482913", parsed.fields["code"])
        assertEquals("livro.txt", parsed.files["file"]?.fileName)
        assertEquals("Ola mundo", parsed.files["file"]?.bytes?.toString(Charsets.UTF_8)?.trim())
    }
}
