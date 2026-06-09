package com.crosspoint.reader.data.opds

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

private const val NS_ATOM = "http://www.w3.org/2005/Atom"
private const val NS_OPDS = "http://opds-spec.org/2010/catalog"
private const val NS_DC = "http://purl.org/dc/terms/"

private val ACQUISITION_RELS = setOf(
    "http://opds-spec.org/acquisition",
    "http://opds-spec.org/acquisition/open-access",
    "http://opds-spec.org/acquisition/buy",
    "http://opds-spec.org/acquisition/borrow",
    "http://opds-spec.org/acquisition/subscribe",
    "http://opds-spec.org/acquisition/sample"
)

private val NAV_RELS = setOf("subsection", "related", "http://opds-spec.org/catalog")

class OpdsParser {

    fun parse(stream: InputStream): OpdsFeed {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(stream, null)
        }

        var feedTitle = ""
        val entries = mutableListOf<OpdsEntry>()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when {
                    parser.name == "title" && parser.depth == 2 -> feedTitle = parser.nextText()
                    parser.name == "entry" -> entries.add(parseEntry(parser))
                }
            }
            event = parser.next()
        }

        return OpdsFeed(title = feedTitle, entries = entries)
    }

    private fun parseEntry(parser: XmlPullParser): OpdsEntry {
        var id = ""
        var title = ""
        var author = ""
        var summary = ""
        var coverUrl: String? = null
        var acquisitionUrl: String? = null
        var acquisitionMime: String? = null
        var navigationUrl: String? = null
        var navigationMime: String? = null

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "entry")) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "id" -> id = parser.nextText()
                    "title" -> title = parser.nextText()
                    "summary", "content" -> summary = parser.nextText()
                    "author" -> author = parseAuthor(parser)
                    "link" -> {
                        val rel = parser.getAttributeValue(null, "rel") ?: ""
                        val href = parser.getAttributeValue(null, "href") ?: ""
                        val mime = parser.getAttributeValue(null, "type") ?: ""
                        when {
                            rel in ACQUISITION_RELS -> {
                                if (acquisitionUrl == null) {
                                    acquisitionUrl = href
                                    acquisitionMime = mime
                                }
                            }
                            rel == "http://opds-spec.org/image" ||
                            rel == "http://opds-spec.org/cover" -> coverUrl = href
                            rel in NAV_RELS || mime.contains("opds-catalog") -> {
                                if (navigationUrl == null) {
                                    navigationUrl = href
                                    navigationMime = mime
                                }
                            }
                        }
                    }
                }
            }
            event = parser.next()
        }

        return OpdsEntry(
            id = id,
            title = title,
            author = author,
            summary = summary,
            coverUrl = coverUrl,
            acquisitionUrl = acquisitionUrl,
            acquisitionMime = acquisitionMime,
            navigationUrl = navigationUrl,
            navigationMime = navigationMime
        )
    }

    private fun parseAuthor(parser: XmlPullParser): String {
        var name = ""
        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "author")) {
            if (event == XmlPullParser.START_TAG && parser.name == "name") {
                name = parser.nextText()
            }
            event = parser.next()
        }
        return name
    }
}
