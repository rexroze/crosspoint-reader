package com.crosspoint.reader.ui.reader

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.crosspoint.reader.CrossPointApp
import com.crosspoint.reader.R
import com.crosspoint.reader.domain.BookRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.streamer.parser.asset.FileAsset
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ReaderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOOK_ID = "extra_book_id"
        const val EXTRA_BOOK_PATH = "extra_book_path"
        const val EXTRA_LOCATOR_JSON = "extra_locator_json"
        private const val TAG_NAVIGATOR = "epub_navigator"
    }

    @Inject
    lateinit var bookRepository: BookRepository

    private var publication: Publication? = null
    private var bookId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        bookId = intent.getLongExtra(EXTRA_BOOK_ID, -1L)
        val bookPath = intent.getStringExtra(EXTRA_BOOK_PATH) ?: run { finish(); return }
        val locatorJson = intent.getStringExtra(EXTRA_LOCATOR_JSON)

        lifecycleScope.launch {
            val file = File(bookPath)
            if (!file.exists()) { finish(); return@launch }

            val app = applicationContext as CrossPointApp
            val pub = app.streamer
                .open(FileAsset(file), allowUserInteraction = false)
                .getOrNull() ?: run { finish(); return@launch }

            publication = pub

            val initialLocator = locatorJson?.let {
                runCatching { Locator.fromJSON(org.json.JSONObject(it)) }.getOrNull()
            }

            if (savedInstanceState == null) {
                val factory = EpubNavigatorFragment.createFactory(
                    publication = pub,
                    baseUrl = null,
                    initialLocator = initialLocator,
                    listener = NavigatorListener()
                )
                supportFragmentManager.fragmentFactory = factory
                supportFragmentManager.commit {
                    add(R.id.fragment_container, EpubNavigatorFragment::class.java, null, TAG_NAVIGATOR)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        saveProgress()
    }

    override fun onDestroy() {
        super.onDestroy()
        publication?.close()
        publication = null
    }

    private fun saveProgress() {
        val nav = supportFragmentManager.findFragmentByTag(TAG_NAVIGATOR)
            as? EpubNavigatorFragment ?: return
        if (bookId < 0) return

        lifecycleScope.launch {
            val locator = nav.currentLocator.value
            val progress = locator.locations.totalProgression?.toFloat() ?: return@launch
            bookRepository.updateProgress(
                id = bookId,
                progress = progress,
                locator = locator.toJSON().toString()
            )
        }
    }

    inner class NavigatorListener : EpubNavigatorFragment.Listener {
        override fun onTap(point: android.graphics.PointF): Boolean = false
    }
}
