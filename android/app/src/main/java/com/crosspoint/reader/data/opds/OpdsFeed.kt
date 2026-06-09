package com.crosspoint.reader.data.opds

data class OpdsFeed(
    val title: String,
    val entries: List<OpdsEntry>
)

data class OpdsEntry(
    val id: String,
    val title: String,
    val author: String,
    val summary: String,
    val coverUrl: String?,
    /** Null means this is a navigation entry (link to another feed). */
    val acquisitionUrl: String?,
    val acquisitionMime: String?,
    /** Link to drill into a sub-catalog feed. */
    val navigationUrl: String?,
    val navigationMime: String?
) {
    val isAcquisition: Boolean get() = acquisitionUrl != null
    val isNavigation: Boolean get() = navigationUrl != null
}
