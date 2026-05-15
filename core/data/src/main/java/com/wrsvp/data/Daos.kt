package com.wrsvp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("SELECT * FROM books ORDER BY updatedAt DESC, title ASC")
    fun observeBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: String): BookEntity?

    @Query("SELECT COUNT(*) FROM books")
    suspend fun countBooks(): Int

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun delete(bookId: String)

    @Query("UPDATE books SET title = :title, updatedAt = :updatedAt WHERE id = :bookId")
    suspend fun renameBook(bookId: String, title: String, updatedAt: Long)

    @Query("UPDATE books SET author = :author, updatedAt = :updatedAt WHERE id = :bookId")
    suspend fun updateAuthor(bookId: String, author: String?, updatedAt: Long)
}

@Dao
interface ChapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY `index` ASC")
    suspend fun getChaptersByBookId(bookId: String): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND `index` = :index")
    suspend fun getChapterByIndex(bookId: String, index: Int): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND startTokenIndex <= :tokenIndex AND endTokenIndex >= :tokenIndex LIMIT 1")
    suspend fun getChapterForToken(bookId: String, tokenIndex: Int): ChapterEntity?

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersByBookId(bookId: String)
}

@Dao
interface TokenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTokens(tokens: List<TokenEntity>)

    @Query("SELECT * FROM tokens WHERE bookId = :bookId AND tokenIndex = :tokenIndex")
    suspend fun getTokenByIndex(bookId: String, tokenIndex: Int): TokenEntity?

    @Query(
        """
        SELECT * FROM tokens
        WHERE bookId = :bookId AND tokenIndex BETWEEN :startInclusive AND :endInclusive
        ORDER BY tokenIndex ASC
        """,
    )
    suspend fun getTokensWindow(bookId: String, startInclusive: Int, endInclusive: Int): List<TokenEntity>

    @Query("SELECT COUNT(*) FROM tokens WHERE bookId = :bookId")
    suspend fun countTokensByBookId(bookId: String): Int

    @Query("SELECT * FROM tokens WHERE bookId = :bookId ORDER BY tokenIndex ASC")
    suspend fun getAllTokensByBookId(bookId: String): List<TokenEntity>

    @Query("DELETE FROM tokens WHERE bookId = :bookId")
    suspend fun deleteTokensByBookId(bookId: String)
}

@Dao
interface ProgressDao {
    @Upsert
    suspend fun upsertProgress(progress: ProgressEntity)

    @Query("SELECT * FROM progress WHERE bookId = :bookId")
    suspend fun getProgressByBookId(bookId: String): ProgressEntity?

    @Query("SELECT * FROM progress WHERE bookId = :bookId")
    fun observeProgressByBookId(bookId: String): Flow<ProgressEntity?>

    @Query("DELETE FROM progress WHERE bookId = :bookId")
    suspend fun deleteProgressByBookId(bookId: String)
}

@Dao
interface SettingsDao {
    @Upsert
    suspend fun upsertSettings(settings: ReaderSettingsEntity)

    @Query("SELECT * FROM reader_settings WHERE bookId IS NULL LIMIT 1")
    suspend fun getGlobalSettings(): ReaderSettingsEntity?

    @Query("SELECT * FROM reader_settings WHERE bookId IS NULL LIMIT 1")
    fun observeGlobalSettings(): Flow<ReaderSettingsEntity?>

    @Query("SELECT * FROM reader_settings WHERE bookId = :bookId LIMIT 1")
    suspend fun getBookSettings(bookId: String): ReaderSettingsEntity?
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY nextAttemptAt ASC")
    suspend fun getByStatus(status: String): List<SyncQueueEntity>

    @Upsert
    suspend fun upsert(item: SyncQueueEntity)
}
