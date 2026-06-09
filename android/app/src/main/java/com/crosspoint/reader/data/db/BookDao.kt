package com.crosspoint.reader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY lastOpenedAt DESC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE lastOpenedAt > 0 ORDER BY lastOpenedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE path = :path")
    suspend fun getByPath(path: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Query("UPDATE books SET progress = :progress, currentLocator = :locator, lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Float, locator: String?, timestamp: Long)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun delete(id: Long)
}
