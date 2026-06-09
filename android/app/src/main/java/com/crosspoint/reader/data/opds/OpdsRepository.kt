package com.crosspoint.reader.data.opds

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpdsRepository @Inject constructor(private val httpClient: OkHttpClient) {

    private val parser = OpdsParser()

    suspend fun fetchFeed(
        url: String,
        username: String = "",
        password: String = ""
    ): Result<OpdsFeed> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).apply {
                if (username.isNotBlank()) {
                    header("Authorization", Credentials.basic(username, password))
                }
            }.build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}: ${response.message}")
                val body = response.body ?: error("Empty response body")
                parser.parse(body.byteStream())
            }
        }
    }

    suspend fun downloadBook(
        url: String,
        username: String = "",
        password: String = ""
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).apply {
                if (username.isNotBlank()) {
                    header("Authorization", Credentials.basic(username, password))
                }
            }.build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}: ${response.message}")
                response.body?.bytes() ?: error("Empty response body")
            }
        }
    }
}
