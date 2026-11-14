package com.lecturebrief.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lecturebrief.data.local.prefs.ModelTier
import com.lecturebrief.data.local.prefs.ProviderPreset
import com.lecturebrief.data.local.prefs.ThemeMode
import com.lecturebrief.ui.state.GatewayConnectionStatus
import com.lecturebrief.ui.state.SettingsUiState
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    SettingsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onUseSameProviderChange = viewModel::onUseSameProviderChange,
        onSttPresetSelected = viewModel::onSttPresetSelected,
        onChatPresetSelected = viewModel::onChatPresetSelected,
        onSttTierSelected = viewModel::onSttTierSelected,
        onChatTierSelected = viewModel::onChatTierSelected,
        onBaseUrlChange = viewModel::onBaseUrlChange,
        onApiKeyChange = viewModel::onApiKeyChange,
        onChatBaseUrlChange = viewModel::onChatBaseUrlChange,
        onChatApiKeyChange = viewModel::onChatApiKeyChange,
        onSttModelChange = viewModel::onSttModelChange,
        onChatModelChange = viewModel::onChatModelChange,
        onThemeModeChange = viewModel::onThemeModeChange,
        onSaveClick = viewModel::saveSettings,
        onTestConnectionClick = viewModel::testConnection,
        onLoadSampleLectureClick = {
            viewModel.loadSampleLecture {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Sample lecture added to library")
                }
            }
        },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onUseSameProviderChange: (Boolean) -> Unit,
    onSttPresetSelected: (ProviderPreset) -> Unit,
    onChatPresetSelected: (ProviderPreset) -> Unit,
    onSttTierSelected: (ModelTier) -> Unit,
    onChatTierSelected: (ModelTier) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onChatBaseUrlChange: (String) -> Unit,
    onChatApiKeyChange: (String) -> Unit,
    onSttModelChange: (String) -> Unit,
    onChatModelChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onSaveClick: () -> Unit,
    onTestConnectionClick: () -> Unit,
    onLoadSampleLectureClick: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var sttApiKeyVisible by rememberSaveable { mutableStateOf(false) }
    var chatApiKeyVisible by rememberSaveable { mutableStateOf(false) }
    val isCustomSelected = uiState.sttPreset == ProviderPreset.CUSTOM ||
        (!uiState.useSameProvider && uiState.chatPreset == ProviderPreset.CUSTOM)
    var advancedExpanded by rememberSaveable { mutableStateOf(isCustomSelected) }

    LaunchedEffect(isCustomSelected) {
        if (isCustomSelected) {
            advancedExpanded = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AI Provider Settings") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Choose your cloud or local AI providers and quality tiers. Base URLs and raw model IDs are tucked into Advanced Settings below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Use same provider & API key for both",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = if (uiState.useSameProvider) {
                            "Shared provider for Speech-to-Text and Summarization"
                        } else {
                            "Independent Speech-to-Text and Summarization providers"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.useSameProvider,
                    onCheckedChange = onUseSameProviderChange
                )
            }

            HorizontalDivider()

            Text(
                text = if (uiState.useSameProvider) {
                    "Provider Configuration (STT & Summary)"
                } else {
                    "Speech-to-Text (STT)"
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            ProviderPresetDropdown(
                label = "Provider",
                selectedPreset = uiState.sttPreset,
                onPresetSelected = onSttPresetSelected
            )

            if (uiState.sttPreset == ProviderPreset.GEMINI) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Gemini's OpenAI endpoint is for summarization only. Pair Groq Cloud for Speech-to-Text with Google Gemini for Summarization.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.apiKey,
                onValueChange = onApiKeyChange,
                label = {
                    Text(
                        if (uiState.useSameProvider) {
                            "API Key (optional for OmniRoute)"
                        } else {
                            "STT API Key"
                        }
                    )
                },
                placeholder = { Text("gsk_... or sk-...") },
                singleLine = true,
                visualTransformation = if (sttApiKeyVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { sttApiKeyVisible = !sttApiKeyVisible }) {
                        Icon(
                            imageVector = if (sttApiKeyVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (sttApiKeyVisible) "Hide API key" else "Show API key"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            val sttTiers = uiState.sttPreset.sttTiers()
            if (sttTiers.isNotEmpty()) {
                TierSelector(
                    label = "STT Quality Tier",
                    tiers = sttTiers.map { it.first },
                    selectedTier = uiState.sttTier,
                    onTierSelected = onSttTierSelected
                )
            }

            if (!uiState.useSameProvider) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Summarization (LLM)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                ProviderPresetDropdown(
                    label = "Summary Provider",
                    selectedPreset = uiState.chatPreset,
                    onPresetSelected = onChatPresetSelected
                )

                OutlinedTextField(
                    value = uiState.chatApiKey,
                    onValueChange = onChatApiKeyChange,
                    label = { Text("Summary API Key") },
                    placeholder = { Text("AIza... or sk-...") },
                    singleLine = true,
                    visualTransformation = if (chatApiKeyVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { chatApiKeyVisible = !chatApiKeyVisible }) {
                            Icon(
                                imageVector = if (chatApiKeyVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (chatApiKeyVisible) "Hide API key" else "Show API key"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val activeChatPreset = if (uiState.useSameProvider) uiState.sttPreset else uiState.chatPreset
            val chatTiers = activeChatPreset.chatTiers()
            if (chatTiers.isNotEmpty()) {
                TierSelector(
                    label = "Summary Quality Tier",
                    tiers = chatTiers.map { it.first },
                    selectedTier = uiState.chatTier,
                    onTierSelected = onChatTierSelected
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSaveClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (uiState.isSaved) "Saved" else "Save")
                }

                Button(
                    onClick = onTestConnectionClick,
                    enabled = uiState.connectionStatus != GatewayConnectionStatus.VERIFYING,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.connectionStatus == GatewayConnectionStatus.VERIFYING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Testing...")
                    } else {
                        Text("Test Connection")
                    }
                }
            }

            ConnectionStatusCard(uiState = uiState)

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "Dark Mode",
                style = MaterialTheme.typography.titleSmall
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val themeModes = ThemeMode.entries
                themeModes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = uiState.themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = themeModes.size
                        ),
                        label = { Text(mode.label, maxLines = 1) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "Offline Demo & Debug Tools",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Insert a realistic mock lecture with transcript and structured Markdown summary into Room for offline demonstrations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(
                onClick = onLoadSampleLectureClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Load Sample Lecture")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            TextButton(
                onClick = { advancedExpanded = !advancedExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Advanced Settings",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Icon(
                        imageVector = if (advancedExpanded) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                        contentDescription = if (advancedExpanded) "Collapse Advanced Settings" else "Expand Advanced Settings"
                    )
                }
            }

            AnimatedVisibility(visible = advancedExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.baseUrl,
                        onValueChange = onBaseUrlChange,
                        label = { Text(if (uiState.useSameProvider) "Base URL" else "STT Base URL") },
                        placeholder = { Text("https://api.groq.com/openai/v1") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uiState.sttModel,
                        onValueChange = onSttModelChange,
                        label = { Text("Custom STT Model ID") },
                        placeholder = { Text("whisper-large-v3") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!uiState.useSameProvider) {
                        OutlinedTextField(
                            value = uiState.chatBaseUrl,
                            onValueChange = onChatBaseUrlChange,
                            label = { Text("Summary Base URL") },
                            placeholder = { Text("https://generativelanguage.googleapis.com/v1beta/openai") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = uiState.chatModel,
                        onValueChange = onChatModelChange,
                        label = { Text("Custom Summary Model ID") },
                        placeholder = { Text("gemini-3.8-flash") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun TierSelector(
    label: String,
    tiers: List<ModelTier>,
    selectedTier: ModelTier?,
    onTierSelected: (ModelTier) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            tiers.forEachIndexed { index, tier ->
                SegmentedButton(
                    selected = selectedTier == tier,
                    onClick = { onTierSelected(tier) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = tiers.size
                    ),
                    label = { Text(tier.label, maxLines = 1) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderPresetDropdown(
    label: String,
    selectedPreset: ProviderPreset,
    onPresetSelected: (ProviderPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedPreset.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ProviderPreset.entries.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.displayName) },
                    onClick = {
                        expanded = false
                        onPresetSelected(preset)
                    }
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(uiState: SettingsUiState) {
    when (uiState.connectionStatus) {
        GatewayConnectionStatus.UNTESTED,
        GatewayConnectionStatus.VERIFYING -> Unit

        GatewayConnectionStatus.CONNECTED -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column {
                        Text(
                            text = "Connection Successful",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = uiState.connectionSummary ?: "${uiState.availableModels.size} available models",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        GatewayConnectionStatus.ERROR -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Column {
                        Text(
                            text = "Connection Failed",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = uiState.connectionError ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}
