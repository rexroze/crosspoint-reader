package com.crosspoint.reader.domain

import com.crosspoint.reader.data.db.BookDao
import com.crosspoint.reader.data.db.BookEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(private val dao: BookDao) {

    fun observeAll(): Flow<List<Book>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeRecent(limit: Int = 20): Flow<List<Book>> =
        dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    suspend fun getByPath(path: String): Book? = dao.getByPath(path)?.toDomain()

    suspend fun addOrGet(book: Book): Book {
        val existing = dao.getByPath(book.path)
        if (existing != null) return existing.toDomain()
        val id = dao.insert(BookEntity.fromDomain(book))
        return book.copy(id = id)
    }

    suspend fun updateProgress(id: Long, progress: Float, locator: String?) {
        dao.updateProgress(id, progress, locator, System.currentTimeMillis())
    }

    suspend fun delete(id: Long) = dao.delete(id)
}
