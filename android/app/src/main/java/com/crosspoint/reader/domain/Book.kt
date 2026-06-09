package com.crosspoint.reader.domain

data class Book(
    val id: Long = 0,
    val path: String,
    val title: String,
    val author: String,
    val coverPath: String? = null,
    val progress: Float = 0f,         // 0.0 – 1.0
    val currentLocator: String? = null, // Readium Locator JSON
    val lastOpenedAt: Long = 0L,
    val addedAt: Long = System.currentTimeMillis()
)
