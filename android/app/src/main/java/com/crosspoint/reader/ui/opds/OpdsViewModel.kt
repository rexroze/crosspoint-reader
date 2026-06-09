package com.crosspoint.reader.ui.opds

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crosspoint.reader.CrossPointApp
import com.crosspoint.reader.data.opds.OpdsFeed
import com.crosspoint.reader.data.opds.OpdsRepository
import com.crosspoint.reader.data.opds.OpdsServer
import com.crosspoint.reader.data.settings.AppSettingsRepository
import com.crosspoint.reader.domain.Book
import com.crosspoint.reader.domain.BookRepository
import com.crosspoint.reader.ui.reader.ReaderActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.readium.r2.streamer.parser.asset.FileAsset
import java.io.File
import javax.inject.Inject

sealed interface OpdsUiState {
    object Loading : OpdsUiState
    data class ServerList(val servers: List<OpdsServer>) : OpdsUiState
    data class Feed(val feed: OpdsFeed, val serverUsername: String, val serverPassword: String) : OpdsUiState
    data class Error(val message: String) : OpdsUiState
    data class Downloading(val title: String) : OpdsUiState
}

@HiltViewModel
class OpdsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val opdsRepository: OpdsRepository,
    private val bookRepository: BookRepository,
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    private val gson = Gson()

    private val _uiState = MutableStateFlow<OpdsUiState>(OpdsUiState.Loading)
    val uiState: StateFlow<OpdsUiState> = _uiState.asStateFlow()

    private var currentUsername = ""
    private var currentPassword = ""

    init {
        viewModelScope.launch { loadServerList() }
    }

    private suspend fun loadServerList() {
        val json = settingsRepository.opdsServersJson.first()
        val type = object : TypeToken<List<OpdsServer>>() {}.type
        val servers: List<OpdsServer> = runCatching { gson.fromJson(json, type) }.getOrDefault(emptyList())
        _uiState.value = OpdsUiState.ServerList(servers)
    }

    fun loadFeed(url: String, username: String = "", password: String = "") {
        currentUsername = username
        currentPassword = password
        _uiState.value = OpdsUiState.Loading
        viewModelScope.launch {
            opdsRepository.fetchFeed(url, username, password)
                .onSuccess { feed -> _uiState.value = OpdsUiState.Feed(feed, username, password) }
                .onFailure { e -> _uiState.value = OpdsUiState.Error(e.message ?: "Failed to load") }
        }
    }

    fun loadServer(server: OpdsServer) = loadFeed(server.url, server.username, server.password)

    fun downloadAndOpen(context: Context, acquisitionUrl: String, title: String) {
        _uiState.update { OpdsUiState.Downloading(title) }
        viewModelScope.launch {
            opdsRepository.downloadBook(acquisitionUrl, currentUsername, currentPassword)
                .onSuccess { bytes ->
                    val app = context.applicationContext as CrossPointApp
                    val file = File(context.filesDir, "${sanitize(title)}.epub")
                    file.writeBytes(bytes)

                    val publication = app.streamer
                        .open(FileAsset(file), allowUserInteraction = false)
                        .getOrNull()

                    val bookTitle = publication?.metadata?.title ?: title
                    val author = publication?.metadata?.authors?.firstOrNull()?.name ?: ""
                    publication?.close()

                    val book = bookRepository.addOrGet(
                        Book(path = file.absolutePath, title = bookTitle, author = author)
                    )
                    val intent = Intent(context, ReaderActivity::class.java).apply {
                        putExtra(ReaderActivity.EXTRA_BOOK_ID, book.id)
                        putExtra(ReaderActivity.EXTRA_BOOK_PATH, book.path)
                    }
                    context.startActivity(intent)
                }
                .onFailure { e ->
                    _uiState.value = OpdsUiState.Error("Download failed: ${e.message}")
                }
        }
    }

    fun addServer(server: OpdsServer) {
        viewModelScope.launch {
            val current = (_uiState.value as? OpdsUiState.ServerList)?.servers ?: emptyList()
            val updated = current + server
            settingsRepository.updateOpdsServersJson(gson.toJson(updated))
            _uiState.value = OpdsUiState.ServerList(updated)
        }
    }

    fun removeServer(server: OpdsServer) {
        viewModelScope.launch {
            val current = (_uiState.value as? OpdsUiState.ServerList)?.servers ?: emptyList()
            val updated = current.filter { it.url != server.url }
            settingsRepository.updateOpdsServersJson(gson.toJson(updated))
            _uiState.value = OpdsUiState.ServerList(updated)
        }
    }

    fun backToServerList() {
        viewModelScope.launch { loadServerList() }
    }

    private fun sanitize(name: String) = name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(64)
}
