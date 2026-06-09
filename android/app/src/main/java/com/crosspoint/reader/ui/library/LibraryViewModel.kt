package com.crosspoint.reader.ui.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crosspoint.reader.CrossPointApp
import com.crosspoint.reader.domain.Book
import com.crosspoint.reader.domain.BookRepository
import com.crosspoint.reader.ui.reader.ReaderActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.readium.r2.streamer.parser.asset.FileAsset
import java.io.File
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository
) : ViewModel() {

    val books: StateFlow<List<Book>> = bookRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun openBook(context: Context, book: Book) {
        val intent = Intent(context, ReaderActivity::class.java).apply {
            putExtra(ReaderActivity.EXTRA_BOOK_ID, book.id)
            putExtra(ReaderActivity.EXTRA_BOOK_PATH, book.path)
            book.currentLocator?.let { putExtra(ReaderActivity.EXTRA_LOCATOR_JSON, it) }
        }
        context.startActivity(intent)
    }

    fun importEpub(context: Context, uri: Uri) {
        viewModelScope.launch {
            val file = copyUriToFiles(context, uri) ?: return@launch
            val app = context.applicationContext as CrossPointApp

            val publication = app.streamer
                .open(FileAsset(file), allowUserInteraction = false)
                .getOrNull() ?: return@launch

            val title = publication.metadata.title ?: file.nameWithoutExtension
            val author = publication.metadata.authors.firstOrNull()?.name ?: ""
            publication.close()

            bookRepository.addOrGet(
                Book(path = file.absolutePath, title = title, author = author)
            )
        }
    }

    fun removeBook(book: Book) {
        viewModelScope.launch { bookRepository.delete(book.id) }
    }

    private fun copyUriToFiles(context: Context, uri: Uri): File? = runCatching {
        val dest = File(context.filesDir, "import_${System.currentTimeMillis()}.epub")
        context.contentResolver.openInputStream(uri)?.use { it.copyTo(dest.outputStream()) }
        dest
    }.getOrNull()
}
