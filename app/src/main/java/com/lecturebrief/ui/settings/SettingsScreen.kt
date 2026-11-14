package com.lecturebrief.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lecturebrief.data.local.prefs.AppColorPalette
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
        onColorPaletteChange = viewModel::onColorPaletteChange,
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
    onColorPaletteChange: (AppColorPalette) -> Unit,
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
                text = "Paste your free API keys below to start transcribing and summarizing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (uiState.useSameProvider) {
                            "1. Audio Transcription & Summary"
                        } else {
                            "1. Audio Transcription"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    ProviderPresetDropdown(
                        label = "Provider",
                        selectedPreset = uiState.sttPreset,
                        onPresetSelected = onSttPresetSelected
                    )

                    ProviderKeyHelper(preset = uiState.sttPreset)

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
                        label = { Text(apiKeyLabelForPreset(uiState.sttPreset)) },
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
                            label = "Accuracy",
                            tiers = sttTiers.map { it.first },
                            selectedTier = uiState.sttTier,
                            onTierSelected = onSttTierSelected
                        )
                    }

                    if (uiState.useSameProvider) {
                        val sharedChatTiers = uiState.sttPreset.chatTiers()
                        if (sharedChatTiers.isNotEmpty()) {
                            TierSelector(
                                label = "Summary Quality",
                                tiers = sharedChatTiers.map { it.first },
                                selectedTier = uiState.chatTier,
                                onTierSelected = onChatTierSelected
                            )
                        }
                    }
                }
            }

            if (!uiState.useSameProvider) {
                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "2. Summary Generation",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        ProviderPresetDropdown(
                            label = "Provider",
                            selectedPreset = uiState.chatPreset,
                            onPresetSelected = onChatPresetSelected
                        )

                        ProviderKeyHelper(preset = uiState.chatPreset)

                        OutlinedTextField(
                            value = uiState.chatApiKey,
                            onValueChange = onChatApiKeyChange,
                            label = { Text(apiKeyLabelForPreset(uiState.chatPreset)) },
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

                        val chatTiers = uiState.chatPreset.chatTiers()
                        if (chatTiers.isNotEmpty()) {
                            TierSelector(
                                label = "Summary Quality",
                                tiers = chatTiers.map { it.first },
                                selectedTier = uiState.chatTier,
                                onTierSelected = onChatTierSelected
                            )
                        }
                    }
                }
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
                text = "Appearance",
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
                        icon = {},
                        label = { Text(mode.label, maxLines = 1) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Accent Color",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = uiState.colorPalette.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppColorPalette.entries.forEach { palette ->
                    val isSelected = uiState.colorPalette == palette
                    val swatchColor = Color(palette.swatchArgb)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 2.5.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                }
                            )
                            .clickable { onColorPaletteChange(palette) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = palette.label,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (palette == AppColorPalette.WALLPAPER) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = palette.label,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
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
                                    "Shared provider for transcription and summary"
                                } else {
                                    "Independent providers for transcription and summary"
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
private fun ProviderKeyHelper(
    preset: ProviderPreset,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    var guideExpanded by rememberSaveable(preset) { mutableStateOf(false) }

    when (preset) {
        ProviderPreset.GROQ,
        ProviderPreset.GEMINI -> {
            val (linkLabel, url, steps) = if (preset == ProviderPreset.GROQ) {
                Triple(
                    "Get Free Groq API Key ↗",
                    "https://console.groq.com/keys",
                    listOf(
                        "1. Sign in with Google/email at console.groq.com (no credit card required).",
                        "2. Tap Create API Key, name it (e.g., LectureBrief), and copy the gsk_... key.",
                        "3. Paste it into the API Key box below."
                    )
                )
            } else {
                Triple(
                    "Get Free Gemini API Key ↗",
                    "https://aistudio.google.com/api-keys",
                    listOf(
                        "1. Sign in with your Google account at aistudio.google.com (free tier, no credit card needed).",
                        "2. Tap Create API key (create or select a free project if prompted).",
                        "3. Copy the AIza... key and paste it into the API Key box below."
                    )
                )
            }

            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { uriHandler.openUri(url) },
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = linkLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    TextButton(
                        onClick = { guideExpanded = !guideExpanded },
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (guideExpanded) "Hide guide" else "How to get a free key?",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                AnimatedVisibility(visible = guideExpanded) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "How to get a free API key",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            steps.forEach { step ->
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        ProviderPreset.OMNIROUTE -> {
            Text(
                text = "Requires OmniRoute running on your PC (http://<PC-IP>:20128/v1). Copy the API key from your local OmniRoute dashboard.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier.fillMaxWidth()
            )
        }

        ProviderPreset.CUSTOM -> {
            Text(
                text = "Enter your custom provider's Base URL, API key, and Model ID in Advanced Settings below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier.fillMaxWidth()
            )
        }
    }
}

private fun apiKeyLabelForPreset(preset: ProviderPreset): String = when (preset) {
    ProviderPreset.GROQ -> "Groq API Key"
    ProviderPreset.GEMINI -> "Gemini API Key"
    ProviderPreset.OMNIROUTE -> "OmniRoute API Key (optional)"
    ProviderPreset.CUSTOM -> "API Key"
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
