package com.wrsvp.parser

import com.wrsvp.domain.model.Book
import com.wrsvp.domain.model.Chapter
import com.wrsvp.domain.model.ReadingToken
import com.wrsvp.reader.TextTokenizer
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

data class ImportedBook(
    val book: Book,
    val chapters: List<Chapter>,
    val tokens: List<ReadingToken>,
    val plainText: String,
)

class BookImportService(
    private val tokenizer: TextTokenizer = TextTokenizer(),
    private val parsers: List<ContentParser> = listOf(TxtParser(), MarkdownParser(), HtmlParser()),
    private val epubParser: EpubParser = EpubParser(),
) {
    fun importText(
        content: String,
        fileName: String,
        fileType: String = fileName.extension(),
        nowMillis: Long = System.currentTimeMillis(),
    ): ImportedBook {
        val normalizedName = if (fileName.extension().isBlank()) "$fileName.$fileType" else fileName
        val parser = parsers.firstOrNull { it.canParse(normalizedName) }
            ?: throw BookParseException("Tipo de arquivo nao suportado: $fileName")
        return parsedToImported(parser.parse(normalizedName, content), normalizedName, nowMillis)
    }

    fun importEpub(
        inputStream: InputStream,
        fileName: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): ImportedBook {
        return parsedToImported(epubParser.parse(fileName, inputStream), fileName, nowMillis)
    }

    fun importParsed(parsedBook: ParsedBook, fileName: String, nowMillis: Long = System.currentTimeMillis()): ImportedBook {
        return parsedToImported(parsedBook, fileName, nowMillis)
    }

    private fun parsedToImported(parsed: ParsedBook, fileName: String, nowMillis: Long): ImportedBook {
        val plainText = parsed.plainText.normalizeWhitespace()
        if (plainText.isBlank()) throw BookParseException("Arquivo sem texto legivel")

        val tokens = tokenizer.tokenize(plainText)
        val bookId = UUID.randomUUID().toString()
        val chapters = buildChapters(bookId, parsed.chapters, tokens)

        return ImportedBook(
            book = Book(
                id = bookId,
                title = parsed.title.ifBlank { fileName.nameWithoutExtension() },
                author = parsed.author,
                language = parsed.language,
                originalFileName = fileName,
                originalFileType = parsed.originalFileType,
                createdAt = nowMillis,
                updatedAt = nowMillis,
                totalWords = tokens.size,
                totalChapters = chapters.size,
                checksum = checksum(plainText),
            ),
            chapters = chapters,
            tokens = tokens,
            plainText = plainText,
        )
    }

    private fun buildChapters(bookId: String, parsedChapters: List<ParsedChapter>, tokens: List<ReadingToken>): List<Chapter> {
        val safeChapters = parsedChapters.ifEmpty { listOf(ParsedChapter("Inicio", 0)) }
        return safeChapters.mapIndexed { index, parsed ->
            val start = if (safeChapters.size == 1) 0 else approximateTokenIndex(parsed.plainTextStartOffset, tokens)
            val nextStart = safeChapters.getOrNull(index + 1)?.let { approximateTokenIndex(it.plainTextStartOffset, tokens) }
            Chapter(
                id = "$bookId-$index",
                bookId = bookId,
                index = index,
                title = parsed.title.ifBlank { "Capitulo ${index + 1}" },
                startTokenIndex = start,
                endTokenIndex = ((nextStart ?: tokens.size) - 1).coerceAtLeast(start),
            )
        }
    }

    private fun approximateTokenIndex(charOffset: Int, tokens: List<ReadingToken>): Int {
        return tokens.indexOfFirst { it.startCharOffset >= charOffset }.takeIf { it >= 0 } ?: 0
    }

    private fun checksum(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        val SUPPORTED_TYPES = setOf("txt", "md", "markdown", "html", "htm", "xhtml", "epub")
    }
}
