package com.crosspoint.reader.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore by preferencesDataStore(name = "crosspoint_settings")

data class ReaderSettings(
    val fontFamily: String = "serif",       // "serif" | "sans"
    val fontSize: Int = 18,                 // sp
    val lineSpacing: Float = 1.4f,
    val marginHorizontal: Int = 24,         // dp
    val theme: String = "light",            // "light" | "dark" | "sepia"
    val textAlign: String = "justify",      // "justify" | "left"
    val hyphenation: Boolean = true
)

data class OpdsSettings(
    val servers: List<OpdsServer> = emptyList()
)

data class OpdsServer(
    val name: String,
    val url: String,
    val username: String = "",
    val password: String = ""
)

data class KOReaderSettings(
    val enabled: Boolean = false,
    val serverUrl: String = "",
    val username: String = "",
    val passwordHash: String = "",  // MD5 of password
    val deviceName: String = "CrossPoint Android"
)

@Singleton
class AppSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val FONT_FAMILY = stringPreferencesKey("font_family")
        private val FONT_SIZE = intPreferencesKey("font_size")
        private val LINE_SPACING = floatPreferencesKey("line_spacing")
        private val MARGIN_H = intPreferencesKey("margin_horizontal")
        private val READER_THEME = stringPreferencesKey("reader_theme")
        private val TEXT_ALIGN = stringPreferencesKey("text_align")
        private val HYPHENATION = booleanPreferencesKey("hyphenation")

        private val KOREADER_ENABLED = booleanPreferencesKey("koreader_enabled")
        private val KOREADER_URL = stringPreferencesKey("koreader_url")
        private val KOREADER_USER = stringPreferencesKey("koreader_user")
        private val KOREADER_PASS_HASH = stringPreferencesKey("koreader_pass_hash")
        private val KOREADER_DEVICE = stringPreferencesKey("koreader_device")

        private val OPDS_SERVERS_JSON = stringPreferencesKey("opds_servers_json")
    }

    val readerSettings: Flow<ReaderSettings> = context.dataStore.data.map { prefs ->
        ReaderSettings(
            fontFamily = prefs[FONT_FAMILY] ?: "serif",
            fontSize = prefs[FONT_SIZE] ?: 18,
            lineSpacing = prefs[LINE_SPACING] ?: 1.4f,
            marginHorizontal = prefs[MARGIN_H] ?: 24,
            theme = prefs[READER_THEME] ?: "light",
            textAlign = prefs[TEXT_ALIGN] ?: "justify",
            hyphenation = prefs[HYPHENATION] ?: true
        )
    }

    val koReaderSettings: Flow<KOReaderSettings> = context.dataStore.data.map { prefs ->
        KOReaderSettings(
            enabled = prefs[KOREADER_ENABLED] ?: false,
            serverUrl = prefs[KOREADER_URL] ?: "",
            username = prefs[KOREADER_USER] ?: "",
            passwordHash = prefs[KOREADER_PASS_HASH] ?: "",
            deviceName = prefs[KOREADER_DEVICE] ?: "CrossPoint Android"
        )
    }

    val opdsServersJson: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[OPDS_SERVERS_JSON] ?: "[]"
    }

    suspend fun updateReaderSettings(settings: ReaderSettings) {
        context.dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                set(FONT_FAMILY, settings.fontFamily)
                set(FONT_SIZE, settings.fontSize)
                set(LINE_SPACING, settings.lineSpacing)
                set(MARGIN_H, settings.marginHorizontal)
                set(READER_THEME, settings.theme)
                set(TEXT_ALIGN, settings.textAlign)
                set(HYPHENATION, settings.hyphenation)
            }
        }
    }

    suspend fun updateKOReaderSettings(settings: KOReaderSettings) {
        context.dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply {
                set(KOREADER_ENABLED, settings.enabled)
                set(KOREADER_URL, settings.serverUrl)
                set(KOREADER_USER, settings.username)
                set(KOREADER_PASS_HASH, settings.passwordHash)
                set(KOREADER_DEVICE, settings.deviceName)
            }
        }
    }

    suspend fun updateOpdsServersJson(json: String) {
        context.dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply { set(OPDS_SERVERS_JSON, json) }
        }
    }
}
