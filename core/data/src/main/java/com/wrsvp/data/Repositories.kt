package com.wrsvp.data

import androidx.room.withTransaction
import com.wrsvp.domain.model.Book
import com.wrsvp.domain.model.Chapter
import com.wrsvp.domain.model.ReaderSettings
import com.wrsvp.domain.model.ReadingProgress
import com.wrsvp.domain.model.ReadingToken
import com.wrsvp.reader.TextTokenizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

class BookRepository(
    private val database: WristRsvpDatabase,
) {
    private val bookDao = database.bookDao()
    private val chapterDao = database.chapterDao()
    private val tokenDao = database.tokenDao()
    private val progressDao = database.progressDao()

    fun observeBooks(): Flow<List<Book>> = bookDao.observeBooks().map { books -> books.map { it.toDomain() } }

    suspend fun getBook(bookId: String): Book? = bookDao.getBookById(bookId)?.toDomain()

    suspend fun getChapters(bookId: String): List<Chapter> = chapterDao.getChaptersByBookId(bookId).map { it.toDomain() }

    suspend fun getTokens(bookId: String): List<ReadingToken> = tokenDao.getAllTokensByBookId(bookId).map { it.toDomain() }

    suspend fun saveBookWithTokens(book: Book, chapters: List<Chapter>, tokens: List<ReadingToken>) {
        database.withTransaction {
            bookDao.insertBook(book.toEntity())
            chapterDao.deleteChaptersByBookId(book.id)
            tokenDao.deleteTokensByBookId(book.id)
            chapterDao.insertChapters(chapters.map { it.toEntity() })
            tokens.chunked(500).forEach { chunk ->
                tokenDao.insertTokens(chunk.map { it.toEntity(book.id) })
            }
        }
    }

    suspend fun deleteBook(bookId: String) {
        database.withTransaction {
            progressDao.deleteProgressByBookId(bookId)
            chapterDao.deleteChaptersByBookId(bookId)
            tokenDao.deleteTokensByBookId(bookId)
            bookDao.delete(bookId)
        }
    }

    suspend fun renameBook(bookId: String, title: String) {
        bookDao.renameBook(bookId, title, System.currentTimeMillis())
    }

    suspend fun updateAuthor(bookId: String, author: String?) {
        bookDao.updateAuthor(bookId, author, System.currentTimeMillis())
    }

    suspend fun ensureDemoBookExists() {
        if (countBooks() > 0) return
        val text = """
            Este e um livro de demonstracao do Wrist RSVP Reader.
            O objetivo e testar a leitura palavra por palavra no relogio.

            Voce pode pausar, continuar, avancar e voltar durante a leitura.
            O progresso sera salvo automaticamente.

            Depois, voce pode enviar livros do computador direto para o relogio.
        """.trimIndent()
        val tokens = TextTokenizer().tokenize(text)
        val now = System.currentTimeMillis()
        val book = Book(
            id = "demo-wrist-rsvp-reader",
            title = "Demo Wrist RSVP",
            author = "Wrist RSVP Reader",
            language = "pt-BR",
            originalFileName = "demo.txt",
            originalFileType = "txt",
            createdAt = now,
            updatedAt = now,
            totalWords = tokens.size,
            totalChapters = 1,
            checksum = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") {
                "%02x".format(it)
            },
        )
        val chapter = Chapter(
            id = "${book.id}-0",
            bookId = book.id,
            index = 0,
            title = "Inicio",
            startTokenIndex = 0,
            endTokenIndex = tokens.lastIndex.coerceAtLeast(0),
        )
        saveBookWithTokens(book, listOf(chapter), tokens)
    }

    suspend fun countBooks(): Int = bookDao.countBooks()
}

class ReaderRepository(
    private val tokenDao: TokenDao,
    private val chapterDao: ChapterDao? = null,
) {
    suspend fun getToken(bookId: String, tokenIndex: Int): ReadingToken? {
        return tokenDao.getTokenByIndex(bookId, tokenIndex)?.toDomain()
    }

    suspend fun getTokensWindow(bookId: String, centerIndex: Int, before: Int, after: Int): List<ReadingToken> {
        val start = (centerIndex - before).coerceAtLeast(0)
        val end = (centerIndex + after).coerceAtLeast(start)
        return tokenDao.getTokensWindow(bookId, start, end).map { it.toDomain() }
    }

    suspend fun countTokens(bookId: String): Int = tokenDao.countTokensByBookId(bookId)

    suspend fun getChapterTokenWindow(bookId: String, tokenIndex: Int, maxTokens: Int = 220): List<ReadingToken> {
        val chapter = chapterDao?.getChapterForToken(bookId, tokenIndex)
        val start = chapter?.startTokenIndex ?: (tokenIndex - maxTokens / 2).coerceAtLeast(0)
        val end = chapter?.endTokenIndex?.coerceAtMost(start + maxTokens - 1) ?: (start + maxTokens - 1)
        return tokenDao.getTokensWindow(bookId, start, end).map { it.toDomain() }
    }

    suspend fun getCurrentChapterTokens(bookId: String, tokenIndex: Int, maxTokens: Int = 900): List<ReadingToken> {
        val chapter = chapterDao?.getChapterForToken(bookId, tokenIndex)
        val start = chapter?.startTokenIndex ?: (tokenIndex - maxTokens / 2).coerceAtLeast(0)
        val end = chapter?.endTokenIndex?.coerceAtMost(start + maxTokens - 1) ?: (start + maxTokens - 1)
        return tokenDao.getTokensWindow(bookId, start, end).map { it.toDomain() }
    }
}

class ProgressRepository(
    private val progressDao: ProgressDao,
) {
    suspend fun saveProgress(progress: ReadingProgress) = progressDao.upsertProgress(progress.toEntity())

    suspend fun getProgress(bookId: String): ReadingProgress? = progressDao.getProgressByBookId(bookId)?.toDomain()

    fun observeProgress(bookId: String): Flow<ReadingProgress?> {
        return progressDao.observeProgressByBookId(bookId).map { it?.toDomain() }
    }
}

class SettingsRepository(
    private val settingsDao: SettingsDao,
) {
    fun observeGlobalReaderSettings(): Flow<ReaderSettings> {
        return settingsDao.observeGlobalSettings().map { entity ->
            entity?.toDomain() ?: ReaderSettings()
        }
    }

    suspend fun getReaderSettings(bookId: String?): ReaderSettings {
        val globalSettings = settingsDao.getGlobalSettings()?.toDomain()
        val bookSettings = bookId?.let { settingsDao.getBookSettings(it)?.toDomain() }

        return when {
            bookSettings != null && globalSettings != null -> bookSettings.copy(
                theme = globalSettings.theme,
                fontScale = globalSettings.fontScale,
            )
            bookSettings != null -> bookSettings
            globalSettings != null -> globalSettings.copy(bookId = bookId)
            else -> ReaderSettings(bookId = bookId)
        }
    }

    suspend fun saveReaderSettings(settings: ReaderSettings) {
        settingsDao.upsertSettings(settings.toEntity())
    }

    suspend fun resetToDefaults(bookId: String?) {
        settingsDao.upsertSettings(ReaderSettings(bookId = bookId).toEntity())
    }
}
