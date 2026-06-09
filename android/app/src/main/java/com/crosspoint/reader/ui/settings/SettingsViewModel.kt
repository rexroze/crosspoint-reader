package com.crosspoint.reader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crosspoint.reader.data.settings.AppSettingsRepository
import com.crosspoint.reader.data.settings.ReaderSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val readerSettings: StateFlow<ReaderSettings> = settingsRepository.readerSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderSettings())

    fun setFontFamily(family: String) = update { it.copy(fontFamily = family) }
    fun setFontSize(size: Int) = update { it.copy(fontSize = size) }
    fun setLineSpacing(spacing: Float) = update { it.copy(lineSpacing = spacing) }
    fun setMargin(margin: Int) = update { it.copy(marginHorizontal = margin) }
    fun setTheme(theme: String) = update { it.copy(theme = theme) }
    fun setTextAlign(align: String) = update { it.copy(textAlign = align) }
    fun setHyphenation(enabled: Boolean) = update { it.copy(hyphenation = enabled) }

    private fun update(transform: (ReaderSettings) -> ReaderSettings) {
        viewModelScope.launch {
            settingsRepository.updateReaderSettings(transform(readerSettings.value))
        }
    }
}
