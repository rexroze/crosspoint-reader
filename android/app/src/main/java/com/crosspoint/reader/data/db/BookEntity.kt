package com.crosspoint.reader.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.crosspoint.reader.domain.Book

@Entity(tableName = "books", indices = [Index("path", unique = true)])
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val title: String,
    val author: String,
    val coverPath: String?,
    val progress: Float,
    val currentLocator: String?,
    val lastOpenedAt: Long,
    val addedAt: Long
) {
    fun toDomain() = Book(
        id = id,
        path = path,
        title = title,
        author = author,
        coverPath = coverPath,
        progress = progress,
        currentLocator = currentLocator,
        lastOpenedAt = lastOpenedAt,
        addedAt = addedAt
    )

    companion object {
        fun fromDomain(book: Book) = BookEntity(
            id = book.id,
            path = book.path,
            title = book.title,
            author = book.author,
            coverPath = book.coverPath,
            progress = book.progress,
            currentLocator = book.currentLocator,
            lastOpenedAt = book.lastOpenedAt,
            addedAt = book.addedAt
        )
    }
}
