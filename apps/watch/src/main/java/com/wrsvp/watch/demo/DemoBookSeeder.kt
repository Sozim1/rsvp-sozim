package com.wrsvp.watch.demo

import android.content.Context
import android.util.Log
import com.wrsvp.data.BookRepository
import com.wrsvp.domain.model.Book
import com.wrsvp.domain.model.Chapter
import com.wrsvp.parser.BookImportService
import com.wrsvp.reader.TextTokenizer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoBookSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val tokenizer: TextTokenizer,
    private val importService: BookImportService,
) {
    suspend fun ensureSeeded() {
        seedBundledEpubIfNeeded()
        if (bookRepository.countBooks() > 0) return

        val now = System.currentTimeMillis()
        val tokens = tokenizer.tokenize(DEMO_TEXT)
        val book = Book(
            id = DEMO_BOOK_ID,
            title = "Demo Wrist RSVP",
            author = "Sozim RSVP",
            language = "pt-BR",
            originalFileName = "demo.txt",
            originalFileType = "txt",
            createdAt = now,
            updatedAt = now,
            totalWords = tokens.size,
            totalChapters = 1,
            checksum = sha256(DEMO_TEXT),
        )
        val chapter = Chapter(
            id = "$DEMO_BOOK_ID-0",
            bookId = DEMO_BOOK_ID,
            index = 0,
            title = "Inicio",
            startTokenIndex = 0,
            endTokenIndex = tokens.lastIndex.coerceAtLeast(0),
        )

        bookRepository.saveBookWithTokens(book, listOf(chapter), tokens)
    }

    private suspend fun seedBundledEpubIfNeeded() {
        if (bookRepository.getBook(BUNDLED_EPUB_BOOK_ID) != null) return
        val imported = runCatching {
            context.assets.open(BUNDLED_EPUB_ASSET).use { input ->
                importService.importEpub(input, "Alem da Muralha - James Lowder.epub")
            }
        }.onFailure { throwable ->
            Log.e("DemoBookSeeder", "Failed to seed bundled EPUB", throwable)
        }.getOrNull() ?: return

        val book = imported.book.copy(
            id = BUNDLED_EPUB_BOOK_ID,
            title = imported.book.title.ifBlank { "Alem da Muralha" },
            author = imported.book.author ?: "James Lowder",
            originalFileName = "Alem da Muralha - James Lowder.epub",
        )
        val chapters = imported.chapters.map { chapter ->
            chapter.copy(
                id = "$BUNDLED_EPUB_BOOK_ID-${chapter.index}",
                bookId = BUNDLED_EPUB_BOOK_ID,
            )
        }
        bookRepository.saveBookWithTokens(book, chapters, imported.tokens)
        bookRepository.deleteBook(DEMO_BOOK_ID)
    }

    private fun sha256(text: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val DEMO_BOOK_ID = "demo-wrist-rsvp-reader"
        const val BUNDLED_EPUB_BOOK_ID = "alem-da-muralha-james-lowder"
        const val BUNDLED_EPUB_ASSET = "books/alem-da-muralha-james-lowder.epub"
        val DEMO_TEXT = """
            Este e um livro de demonstracao do Sozim RSVP.
            O objetivo e testar a leitura palavra por palavra no relogio.

            Voce pode pausar, continuar, avancar e voltar durante a leitura.
            O progresso sera salvo automaticamente.

            Depois, voce pode enviar livros do computador direto para o relogio.
        """.trimIndent()
    }
}
