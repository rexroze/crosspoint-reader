package com.crosspoint.reader.data.koreader

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class SyncProgress(
    val document: String,
    val progress: String,
    val percentage: Double,
    val device: String,
    @SerializedName("device_id") val deviceId: String
)

data class SyncResult(
    val document: String,
    val progress: String,
    val percentage: Double,
    val device: String,
    @SerializedName("device_id") val deviceId: String,
    val timestamp: Long
)

@Singleton
class KOReaderSyncClient @Inject constructor(private val httpClient: OkHttpClient) {

    private val gson = Gson()

    /** MD5 of password as expected by the KOSync API. */
    fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(password.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    suspend fun authenticate(serverUrl: String, username: String, passwordHash: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url("${serverUrl.trimEnd('/')}/users/auth")
                    .header("x-auth-user", username)
                    .header("x-auth-key", passwordHash)
                    .get()
                    .build()
                httpClient.newCall(request).execute().use { it.isSuccessful }
            }.getOrDefault(false)
        }

    suspend fun pushProgress(
        serverUrl: String,
        username: String,
        passwordHash: String,
        progress: SyncProgress
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = gson.toJson(progress).toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${serverUrl.trimEnd('/')}/syncs/progress")
                .header("x-auth-user", username)
                .header("x-auth-key", passwordHash)
                .put(body)
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
            }
        }
    }

    suspend fun fetchProgress(
        serverUrl: String,
        username: String,
        passwordHash: String,
        document: String
    ): Result<SyncResult?> = withContext(Dispatchers.IO) {
        runCatching {
            val encoded = java.net.URLEncoder.encode(document, "UTF-8")
            val request = Request.Builder()
                .url("${serverUrl.trimEnd('/')}/syncs/progress/$encoded")
                .header("x-auth-user", username)
                .header("x-auth-key", passwordHash)
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.code == 404) return@use null
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val json = response.body?.string() ?: return@use null
                gson.fromJson(json, SyncResult::class.java)
            }
        }
    }
}
