package com.lecturebrief.ui.detail

import android.content.Intent
import com.lecturebrief.ui.util.LatexMathFormatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lecturebrief.data.local.db.entity.FolderEntity
import com.lecturebrief.data.local.db.entity.ProcessingStatus
import com.lecturebrief.ui.components.DeleteAudioFileDialog
import com.lecturebrief.ui.components.MoveToFolderDialog
import com.lecturebrief.ui.components.RenameRecordingDialog
import com.lecturebrief.ui.components.hasLocalAudioFile
import com.lecturebrief.ui.components.shareRecordingAudioFile
import com.lecturebrief.ui.state.RecordingDetailUiState
import kotlinx.coroutines.launch

@Composable
fun DetailRoute(
    viewModel: DetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val markdown = uiState.summary?.overviewMarkdown

    DetailScreen(
        uiState = uiState,
        folders = folders,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onRetryClick = viewModel::retryProcessing,
        onRenameRecording = viewModel::renameRecording,
        onMoveToFolder = viewModel::moveToFolder,
        onCreateFolderAndMove = viewModel::createFolderAndMove,
        onDeleteAudioFileOnly = {
            viewModel.deleteAudioFileOnly()
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Audio file deleted; summary & transcript kept")
            }
        },
        onShareAudioClick = {
            val path = uiState.audioFilePath
            if (path == null || !shareRecordingAudioFile(context, uiState.title, path)) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Audio file unavailable")
                }
            }
        },
        onCopyMarkdownClick = {
            if (!markdown.isNullOrBlank()) {
                clipboardManager.setText(AnnotatedString(markdown))
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Markdown summary copied to clipboard")
                }
            }
        },
        onShareMarkdownClick = {
            if (!markdown.isNullOrBlank()) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, uiState.title)
                    putExtra(Intent.EXTRA_TEXT, markdown)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Lecture Summary"))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    uiState: RecordingDetailUiState,
    folders: List<FolderEntity>,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onRetryClick: () -> Unit,
    onRenameRecording: (String) -> Unit,
    onMoveToFolder: (Long?) -> Unit,
    onCreateFolderAndMove: (String) -> Unit,
    onDeleteAudioFileOnly: () -> Unit,
    onShareAudioClick: () -> Unit,
    onCopyMarkdownClick: () -> Unit,
    onShareMarkdownClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteAudioDialog by remember { mutableStateOf(false) }
    val hasMarkdown = !uiState.summary?.overviewMarkdown.isNullOrBlank()
    val audioAvailable = remember(uiState.audioFilePath) { hasLocalAudioFile(uiState.audioFilePath) }

    if (showRenameDialog) {
        RenameRecordingDialog(
            currentTitle = uiState.title,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newTitle ->
                onRenameRecording(newTitle)
                showRenameDialog = false
            }
        )
    }

    if (showMoveDialog) {
        MoveToFolderDialog(
            currentFolderId = uiState.folderId,
            folders = folders,
            onDismiss = { showMoveDialog = false },
            onSelectFolder = { folderId ->
                onMoveToFolder(folderId)
                showMoveDialog = false
            },
            onCreateNewFolderAndMove = { folderName ->
                onCreateFolderAndMove(folderName)
                showMoveDialog = false
            }
        )
    }

    if (showDeleteAudioDialog) {
        DeleteAudioFileDialog(
            lectureTitle = uiState.title,
            onDismiss = { showDeleteAudioDialog = false },
            onConfirm = {
                onDeleteAudioFileOnly()
                showDeleteAudioDialog = false
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.title.ifBlank { "Lecture Detail" },
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (hasMarkdown) {
                        IconButton(onClick = onCopyMarkdownClick) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Markdown"
                            )
                        }
                        IconButton(onClick = onShareMarkdownClick) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Markdown"
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Lecture options"
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Folder") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    showMoveDialog = true
                                }
                            )
                            if (audioAvailable) {
                                DropdownMenuItem(
                                    text = { Text("Share Recording") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.AudioFile, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onShareAudioClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Audio File") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        showDeleteAudioDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ProcessingStatusSection(
                status = uiState.status,
                processingMessage = uiState.processingMessage,
                errorMessage = uiState.errorMessage,
                onRetryClick = onRetryClick
            )

            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Summary") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Transcript") }
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> SummaryTabContent(
                        uiState = uiState,
                        onCopyMarkdownClick = onCopyMarkdownClick,
                        onShareMarkdownClick = onShareMarkdownClick
                    )

                    1 -> TranscriptTabContent(transcriptText = uiState.transcriptText)
                }
            }
        }
    }
}

@Composable
private fun ProcessingStatusSection(
    status: ProcessingStatus,
    processingMessage: String?,
    errorMessage: String?,
    onRetryClick: () -> Unit
) {
    when (status) {
        ProcessingStatus.PENDING,
        ProcessingStatus.TRANSCRIBING,
        ProcessingStatus.SUMMARIZING -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = processingMessage ?: when (status) {
                        ProcessingStatus.PENDING -> "Preparing AI pipeline..."
                        ProcessingStatus.TRANSCRIBING -> "Transcribing audio..."
                        ProcessingStatus.SUMMARIZING -> "Generating summary..."
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        ProcessingStatus.ERROR -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Processing Failed",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Text(
                        text = errorMessage ?: "An unknown error occurred.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(onClick = onRetryClick) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry")
                    }
                }
            }
        }

        ProcessingStatus.RECORDING,
        ProcessingStatus.DONE -> Unit
    }
}

@Composable
private fun SummaryTabContent(
    uiState: RecordingDetailUiState,
    onCopyMarkdownClick: () -> Unit,
    onShareMarkdownClick: () -> Unit
) {
    val summary = uiState.summary
    if (summary == null || summary.overviewMarkdown.isBlank()) {
        val placeholderText = when (uiState.status) {
            ProcessingStatus.PENDING,
            ProcessingStatus.TRANSCRIBING,
            ProcessingStatus.SUMMARIZING -> "Your structured lecture summary will appear here once processing finishes."
            ProcessingStatus.ERROR -> "Summary unavailable. Check your API key in Settings or tap Retry above."
            else -> "No summary generated yet."
        }
        Text(
            text = placeholderText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCopyMarkdownClick,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy Markdown")
        }

        Button(
            onClick = onShareMarkdownClick,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share Markdown")
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    SelectionContainer {
        MarkdownContent(markdown = summary.overviewMarkdown)
    }
}

@Composable
private fun TranscriptTabContent(transcriptText: String?) {
    if (transcriptText.isNullOrBlank()) {
        Text(
            text = "Transcript is not available yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        SelectionContainer {
            Text(
                text = transcriptText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun MarkdownContent(markdown: String) {
    val lines = remember(markdown) {
        LatexMathFormatter.formatMathInMarkdown(markdown).lines()
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { rawLine ->
            val line = rawLine.trimEnd()
            when {
                line.isBlank() -> Spacer(modifier = Modifier.height(4.dp))
                line.startsWith("### ") -> Text(
                    text = parseInlineBold(line.removePrefix("### ").trim()),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp)
                )
                line.startsWith("## ") -> Text(
                    text = parseInlineBold(line.removePrefix("## ").trim()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp)
                )
                line.startsWith("# ") -> Text(
                    text = parseInlineBold(line.removePrefix("# ").trim()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp)
                )
                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                    val content = line.trimStart().drop(2).trim()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = parseInlineBold(content),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                else -> Text(
                    text = parseInlineBold(line),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun parseInlineBold(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val start = text.indexOf("**", cursor)
            if (start == -1) {
                append(text.substring(cursor))
                break
            }
            if (start > cursor) {
                append(text.substring(cursor, start))
            }
            val end = text.indexOf("**", start + 2)
            if (end == -1) {
                append(text.substring(start))
                break
            }
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(text.substring(start + 2, end))
            }
            cursor = end + 2
        }
    }
}
