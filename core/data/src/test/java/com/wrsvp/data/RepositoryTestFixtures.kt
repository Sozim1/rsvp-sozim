package com.wrsvp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wrsvp.domain.model.Book
import com.wrsvp.domain.model.Chapter
import com.wrsvp.reader.TextTokenizer

fun inMemoryDatabase(): WristRsvpDatabase {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return Room.inMemoryDatabaseBuilder(context, WristRsvpDatabase::class.java)
        .allowMainThreadQueries()
        .build()
}

fun sampleBook(id: String = "book-1", tokenCount: Int = 4): Triple<Book, List<Chapter>, List<com.wrsvp.domain.model.ReadingToken>> {
    val text = (1..tokenCount).joinToString(" ") { "palavra$it" }
    val tokens = TextTokenizer().tokenize(text)
    val book = Book(
        id = id,
        title = "Livro teste",
        author = "Autor",
        language = "pt-BR",
        originalFileName = "livro.txt",
        originalFileType = "txt",
        createdAt = 1L,
        updatedAt = 1L,
        totalWords = tokens.size,
        totalChapters = 1,
        checksum = "checksum",
    )
    val chapters = listOf(
        Chapter(
            id = "$id-0",
            bookId = id,
            index = 0,
            title = "Inicio",
            startTokenIndex = 0,
            endTokenIndex = tokens.lastIndex,
        ),
    )
    return Triple(book, chapters, tokens)
}
