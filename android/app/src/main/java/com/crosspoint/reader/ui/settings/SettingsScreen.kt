package com.crosspoint.reader.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crosspoint.reader.data.settings.KOReaderSettings
import com.crosspoint.reader.data.settings.ReaderSettings
import com.crosspoint.reader.data.settings.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToKOReaderSync: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.readerSettings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSection("Reading")

            // Theme
            SegmentedSetting(
                label = "Theme",
                options = listOf("Light" to "light", "Sepia" to "sepia", "Dark" to "dark"),
                selected = settings.theme,
                onSelect = viewModel::setTheme
            )

            // Font family
            SegmentedSetting(
                label = "Font",
                options = listOf("Serif" to "serif", "Sans-serif" to "sans"),
                selected = settings.fontFamily,
                onSelect = viewModel::setFontFamily
            )

            // Font size
            StepperSetting(
                label = "Font size",
                value = settings.fontSize,
                unit = "sp",
                min = 12,
                max = 28,
                step = 1,
                onDecrement = { viewModel.setFontSize((settings.fontSize - 1).coerceAtLeast(12)) },
                onIncrement = { viewModel.setFontSize((settings.fontSize + 1).coerceAtMost(28)) }
            )

            // Line spacing
            StepperSetting(
                label = "Line spacing",
                value = "%.1f".format(settings.lineSpacing).toFloat().let { it },
                displayValue = "%.1f×".format(settings.lineSpacing),
                min = 1.0f,
                max = 2.5f,
                onDecrement = { viewModel.setLineSpacing((settings.lineSpacing - 0.1f).coerceAtLeast(1.0f)) },
                onIncrement = { viewModel.setLineSpacing((settings.lineSpacing + 0.1f).coerceAtMost(2.5f)) }
            )

            // Margin
            StepperSetting(
                label = "Margin",
                value = settings.marginHorizontal,
                unit = "dp",
                min = 8,
                max = 64,
                step = 4,
                onDecrement = { viewModel.setMargin((settings.marginHorizontal - 4).coerceAtLeast(8)) },
                onIncrement = { viewModel.setMargin((settings.marginHorizontal + 4).coerceAtMost(64)) }
            )

            // Text alignment
            SegmentedSetting(
                label = "Alignment",
                options = listOf("Justify" to "justify", "Left" to "left"),
                selected = settings.textAlign,
                onSelect = viewModel::setTextAlign
            )

            // Hyphenation
            SwitchSetting(
                label = "Hyphenation",
                checked = settings.hyphenation,
                onCheckedChange = viewModel::setHyphenation
            )

            SettingsSection("Sync")

            NavSetting(
                label = "KOReader sync",
                sublabel = "Sync reading progress with KOReader",
                icon = Icons.Outlined.SyncAlt,
                onClick = onNavigateToKOReaderSync
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KOReaderSyncScreen(
    onBack: () -> Unit,
    viewModel: KOReaderViewModel = hiltViewModel()
) {
    val settings by viewModel.koReaderSettings.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }
    var rawPassword by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KOReader Sync") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            SwitchSetting(
                label = "Enable sync",
                checked = settings.enabled,
                onCheckedChange = { viewModel.setEnabled(it) }
            )

            OutlinedTextField(
                value = settings.serverUrl,
                onValueChange = { viewModel.setServerUrl(it) },
                label = { Text("Server URL") },
                placeholder = { Text("https://sync.koreader.rocks") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = settings.enabled
            )

            OutlinedTextField(
                value = settings.username,
                onValueChange = { viewModel.setUsername(it) },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = settings.enabled
            )

            OutlinedTextField(
                value = rawPassword,
                onValueChange = {
                    rawPassword = it
                    viewModel.setPasswordHash(it)
                },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = settings.enabled,
                visualTransformation = if (passwordVisible)
                    androidx.compose.ui.text.input.VisualTransformation.None
                else
                    androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            null
                        )
                    }
                }
            )

            OutlinedTextField(
                value = settings.deviceName,
                onValueChange = { viewModel.setDeviceName(it) },
                label = { Text("Device name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = settings.enabled
            )

            Button(
                onClick = {
                    viewModel.testConnection { result -> testResult = result }
                },
                enabled = settings.enabled && settings.serverUrl.isNotBlank() && settings.username.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test connection")
            }

            testResult?.let {
                Text(
                    it,
                    color = if (it.startsWith("✓")) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Shared setting composables ──────────────────────────────────────────────

@Composable
private fun SettingsSection(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 6.dp)
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SegmentedSetting(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { idx, (label, value) ->
                SegmentedButton(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(idx, options.size),
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun StepperSetting(
    label: String,
    value: Int,
    unit: String,
    min: Int,
    max: Int,
    step: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onDecrement, enabled = value > min) {
            Icon(Icons.Outlined.Remove, "Decrease")
        }
        Text("$value$unit", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.widthIn(min = 48.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = onIncrement, enabled = value < max) {
            Icon(Icons.Outlined.Add, "Increase")
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun StepperSetting(
    label: String,
    value: Float,
    displayValue: String,
    min: Float,
    max: Float,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onDecrement, enabled = value > min) {
            Icon(Icons.Outlined.Remove, "Decrease")
        }
        Text(displayValue, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.widthIn(min = 48.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = onIncrement, enabled = value < max) {
            Icon(Icons.Outlined.Add, "Increase")
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SwitchSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun NavSetting(
    label: String,
    sublabel: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        icon?.let { Icon(it, null, tint = MaterialTheme.colorScheme.onBackground) }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            sublabel?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

// ─── KOReader ViewModel ───────────────────────────────────────────────────────

@HiltViewModel
class KOReaderViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val syncClient: com.crosspoint.reader.data.koreader.KOReaderSyncClient
) : ViewModel() {

    val koReaderSettings: StateFlow<KOReaderSettings> = settingsRepository.koReaderSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KOReaderSettings())

    fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }
    fun setServerUrl(url: String) = update { it.copy(serverUrl = url) }
    fun setUsername(user: String) = update { it.copy(username = user) }
    fun setPasswordHash(raw: String) = update { it.copy(passwordHash = syncClient.hashPassword(raw)) }
    fun setDeviceName(name: String) = update { it.copy(deviceName = name) }

    fun testConnection(onResult: (String) -> Unit) {
        val s = koReaderSettings.value
        viewModelScope.launch {
            val ok = syncClient.authenticate(s.serverUrl, s.username, s.passwordHash)
            onResult(if (ok) "✓ Connected successfully" else "✗ Authentication failed")
        }
    }

    private fun update(transform: (KOReaderSettings) -> KOReaderSettings) {
        viewModelScope.launch {
            settingsRepository.updateKOReaderSettings(transform(koReaderSettings.value))
        }
    }
}
