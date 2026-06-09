package com.crosspoint.reader.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Library : Screen("library")
    object Opds : Screen("opds")
    object Settings : Screen("settings")
    object KOReaderSync : Screen("koreader_sync")

    // OPDS drill-down; url is base64-encoded to avoid NavGraph delimiter conflicts
    class OpdsCatalog(url: String) : Screen("opds_catalog/$url") {
        companion object {
            const val ROUTE = "opds_catalog/{url}"
            const val ARG_URL = "url"
        }
    }
}
