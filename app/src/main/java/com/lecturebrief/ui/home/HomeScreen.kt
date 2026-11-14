package com.lecturebrief.ui.home

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lecturebrief.data.local.db.entity.AudioSourceMode
import com.lecturebrief.data.local.db.entity.FolderEntity
import com.lecturebrief.service.AudioRecorderService
import com.lecturebrief.service.RecordingState
import com.lecturebrief.service.RecordingStatus
import com.lecturebrief.ui.components.CreateFolderDialog
import com.lecturebrief.ui.components.DeleteAudioFileDialog
import com.lecturebrief.ui.components.DeleteFolderDialog
import com.lecturebrief.ui.components.MoveToFolderDialog
import com.lecturebrief.ui.components.RenameRecordingDialog
import com.lecturebrief.ui.components.hasLocalAudioFile
import com.lecturebrief.ui.components.shareRecordingAudioFile
import com.lecturebrief.ui.state.LibraryUiState
import com.lecturebrief.ui.state.RecordingListItemUiModel
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    onRecordingClick: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val selectedSourceMode by viewModel.selectedSourceMode.collectAsStateWithLifecycle()
    val waveformSamples by viewModel.waveformSamples.collectAsStateWithLifecycle()
    val libraryUiState by viewModel.libraryUiState.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val importMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importMediaUri(
                uri = uri,
                onSuccess = { recordingId -> onRecordingClick(recordingId) },
                onError = { message ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
            )
        }
    }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedExplicitly by remember { mutableStateOf(false) }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            AudioRecorderService.startRecording(
                context = context,
                sourceMode = selectedSourceMode,
                projectionResultCode = result.resultCode,
                projectionData = data,
                folderId = libraryUiState.selectedFolderId
            )
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Internal audio capture permission was cancelled.")
            }
        }
    }

    val launchMediaProjectionPrompt: () -> Unit = {
        val projectionManager = context.getSystemService(MediaProjectionManager::class.java)
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    val requiredPermissions = remember(selectedSourceMode) {
        buildList {
            if (selectedSourceMode != AudioSourceMode.INTERNAL_ONLY) {
                add(Manifest.permission.RECORD_AUDIO)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val needsMic = selectedSourceMode != AudioSourceMode.INTERNAL_ONLY
        val micGranted = if (!needsMic) {
            true
        } else {
            results[Manifest.permission.RECORD_AUDIO]
                ?: (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED)
        }

        hasMicPermission = micGranted
        permissionDeniedExplicitly = needsMic && !micGranted

        if (micGranted) {
            when (selectedSourceMode) {
                AudioSourceMode.MICROPHONE_ONLY -> {
                    AudioRecorderService.startRecording(
                        context = context,
                        sourceMode = AudioSourceMode.MICROPHONE_ONLY,
                        folderId = libraryUiState.selectedFolderId
                    )
                }

                AudioSourceMode.INTERNAL_ONLY,
                AudioSourceMode.MIC_AND_INTERNAL -> {
                    launchMediaProjectionPrompt()
                }
            }
        }
    }

    HomeScreen(
        recordingState = recordingState,
        selectedSourceMode = selectedSourceMode,
        waveformSamples = waveformSamples,
        libraryUiState = libraryUiState,
        isImporting = isImporting,
        snackbarHostState = snackbarHostState,
        hasMicPermission = hasMicPermission,
        permissionDenied = permissionDeniedExplicitly,
        onSourceModeSelected = viewModel::onSourceModeSelected,
        onStartRecordingClick = {
            val needsMic = selectedSourceMode != AudioSourceMode.INTERNAL_ONLY
            val micGranted = !needsMic || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

            if (micGranted && notificationsGranted) {
                hasMicPermission = micGranted
                when (selectedSourceMode) {
                    AudioSourceMode.MICROPHONE_ONLY -> {
                        AudioRecorderService.startRecording(
                            context = context,
                            sourceMode = AudioSourceMode.MICROPHONE_ONLY,
                            folderId = libraryUiState.selectedFolderId
                        )
                    }

                    AudioSourceMode.INTERNAL_ONLY,
                    AudioSourceMode.MIC_AND_INTERNAL -> {
                        launchMediaProjectionPrompt()
                    }
                }
            } else {
                permissionLauncher.launch(requiredPermissions)
            }
        },
        onPauseClick = { AudioRecorderService.pauseRecording(context) },
        onResumeClick = { AudioRecorderService.resumeRecording(context) },
        onStopClick = { AudioRecorderService.stopRecording(context) },
        onRequestPermissionClick = { permissionLauncher.launch(requiredPermissions) },
        onFolderSelected = viewModel::onFolderSelected,
        onCreateFolder = viewModel::createFolder,
        onDeleteFolder = viewModel::deleteFolder,
        onRenameRecording = viewModel::renameRecording,
        onMoveRecordingToFolder = viewModel::moveRecordingToFolder,
        onCreateFolderAndMoveRecording = viewModel::createFolderAndMoveRecording,
        onDeleteAudioFileOnly = viewModel::deleteAudioFileOnly,
        onDeleteRecording = viewModel::deleteRecording,
        onImportMediaClick = { importMediaLauncher.launch(arrayOf("audio/*", "video/*")) },
        onRecordingClick = onRecordingClick,
        onNavigateToSettings = onNavigateToSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    recordingState: RecordingState,
    selectedSourceMode: AudioSourceMode,
    waveformSamples: List<Float>,
    libraryUiState: LibraryUiState,
    isImporting: Boolean,
    snackbarHostState: SnackbarHostState,
    hasMicPermission: Boolean,
    permissionDenied: Boolean,
    onSourceModeSelected: (AudioSourceMode) -> Unit,
    onStartRecordingClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit,
    onRequestPermissionClick: () -> Unit,
    onFolderSelected: (Long?) -> Unit,
    onCreateFolder: (String) -> Unit,
    onDeleteFolder: (Long) -> Unit,
    onRenameRecording: (Long, String) -> Unit,
    onMoveRecordingToFolder: (Long, Long?) -> Unit,
    onCreateFolderAndMoveRecording: (Long, String) -> Unit,
    onDeleteAudioFileOnly: (Long) -> Unit,
    onDeleteRecording: (Long) -> Unit,
    onImportMediaClick: () -> Unit,
    onRecordingClick: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<FolderEntity?>(null) }
    var recordingToRename by remember { mutableStateOf<RecordingListItemUiModel?>(null) }
    var recordingToMove by remember { mutableStateOf<RecordingListItemUiModel?>(null) }
    var recordingToDeleteAudio by remember { mutableStateOf<RecordingListItemUiModel?>(null) }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                onCreateFolder(name)
                showCreateFolderDialog = false
            }
        )
    }

    folderToDelete?.let { folder ->
        DeleteFolderDialog(
            folderName = folder.name,
            onDismiss = { folderToDelete = null },
            onConfirm = {
                onDeleteFolder(folder.id)
                folderToDelete = null
            }
        )
    }

    recordingToRename?.let { item ->
        RenameRecordingDialog(
            currentTitle = item.title,
            onDismiss = { recordingToRename = null },
            onConfirm = { newTitle ->
                onRenameRecording(item.id, newTitle)
                recordingToRename = null
            }
        )
    }

    recordingToMove?.let { item ->
        MoveToFolderDialog(
            currentFolderId = item.folderId,
            folders = libraryUiState.folders,
            onDismiss = { recordingToMove = null },
            onSelectFolder = { targetFolderId ->
                onMoveRecordingToFolder(item.id, targetFolderId)
                recordingToMove = null
            },
            onCreateNewFolderAndMove = { newFolderName ->
                onCreateFolderAndMoveRecording(item.id, newFolderName)
                recordingToMove = null
            }
        )
    }

    recordingToDeleteAudio?.let { item ->
        DeleteAudioFileDialog(
            lectureTitle = item.title,
            onDismiss = { recordingToDeleteAudio = null },
            onConfirm = {
                onDeleteAudioFileOnly(item.id)
                recordingToDeleteAudio = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Audio file deleted; summary & transcript kept")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("LectureBrief") },
                actions = {
                    IconButton(
                        onClick = onImportMediaClick,
                        enabled = !isImporting
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Import Audio / Video"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasMicPermission && permissionDenied) {
                item(key = "permission_banner") {
                    PermissionRationaleCard(onGrantClick = onRequestPermissionClick)
                }
            }

            if (isImporting) {
                item(key = "importing_banner") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Extracting audio track...",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            item(key = "recorder_card") {
                RecorderControlCard(
                    recordingState = recordingState,
                    selectedSourceMode = selectedSourceMode,
                    waveformSamples = waveformSamples,
                    isImporting = isImporting,
                    onSourceModeSelected = onSourceModeSelected,
                    onStartClick = onStartRecordingClick,
                    onImportClick = onImportMediaClick,
                    onPauseClick = onPauseClick,
                    onResumeClick = onResumeClick,
                    onStopClick = onStopClick
                )
            }

            item(key = "folder_bar") {
                FolderFilterRow(
                    folders = libraryUiState.folders,
                    selectedFolderId = libraryUiState.selectedFolderId,
                    onFolderSelected = onFolderSelected,
                    onFolderLongClick = { folder -> folderToDelete = folder },
                    onAddFolderClick = { showCreateFolderDialog = true }
                )
            }

            item(key = "recordings_header") {
                Text(
                    text = "Saved Recordings (${libraryUiState.recordings.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            val inlineError = recordingState.errorMessage ?: libraryUiState.errorMessage
            if (!inlineError.isNullOrBlank()) {
                item(key = "inline_error_banner") {
                    InlineErrorBanner(message = inlineError)
                }
            }

            if (libraryUiState.recordings.isEmpty() && !libraryUiState.isLoading) {
                item(key = "empty_state") {
                    EmptyRecordingsCard(
                        isFilteredByFolder = libraryUiState.selectedFolderId != null,
                        onOpenSettings = onNavigateToSettings
                    )
                }
            } else {
                items(
                    items = libraryUiState.recordings,
                    key = { it.id }
                ) { item ->
                    val folderName = remember(item.folderId, libraryUiState.folders) {
                        libraryUiState.folders.firstOrNull { it.id == item.folderId }?.name
                    }
                    SavedRecordingRow(
                        item = item,
                        folderName = folderName,
                        onClick = { onRecordingClick(item.id) },
                        onRenameClick = { recordingToRename = item },
                        onMoveToFolderClick = { recordingToMove = item },
                        onShareAudioClick = {
                            val path = item.audioFilePath
                            if (path == null || !shareRecordingAudioFile(context, item.title, path)) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Audio file unavailable")
                                }
                            }
                        },
                        onDeleteAudioFileClick = { recordingToDeleteAudio = item },
                        onDeleteClick = { onDeleteRecording(item.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderFilterRow(
    folders: List<FolderEntity>,
    selectedFolderId: Long?,
    onFolderSelected: (Long?) -> Unit,
    onFolderLongClick: (FolderEntity) -> Unit,
    onAddFolderClick: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item(key = "folder_all") {
            FilterChip(
                selected = selectedFolderId == null,
                onClick = { onFolderSelected(null) },
                label = { Text("All") }
            )
        }

        items(items = folders, key = { it.id }) { folder ->
            Box {
                FilterChip(
                    selected = selectedFolderId == folder.id,
                    onClick = { onFolderSelected(folder.id) },
                    label = { Text(folder.name) }
                )
                // Transparent overlay supporting long-press to delete while delegating tap to FilterChip
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .combinedClickable(
                            onClick = { onFolderSelected(folder.id) },
                            onLongClick = { onFolderLongClick(folder) }
                        )
                )
            }
        }

        item(key = "folder_add") {
            AssistChip(
                onClick = onAddFolderClick,
                label = { Text("+") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Folder",
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun PermissionRationaleCard(
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Microphone Permission Required",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "LectureBrief needs microphone access to record lectures in the background.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            OutlinedButton(onClick = onGrantClick) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun RecorderControlCard(
    recordingState: RecordingState,
    selectedSourceMode: AudioSourceMode,
    waveformSamples: List<Float>,
    isImporting: Boolean,
    onSourceModeSelected: (AudioSourceMode) -> Unit,
    onStartClick: () -> Unit,
    onImportClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val activeMode = if (recordingState.status == RecordingStatus.IDLE) {
        selectedSourceMode
    } else {
        recordingState.sourceMode
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = listOf(
                    AudioSourceMode.MICROPHONE_ONLY to "Mic",
                    AudioSourceMode.INTERNAL_ONLY to "Internal",
                    AudioSourceMode.MIC_AND_INTERNAL to "Mic + Int"
                )
                modes.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = activeMode == mode,
                        onClick = { onSourceModeSelected(mode) },
                        enabled = recordingState.status == RecordingStatus.IDLE,
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                        label = { Text(label, maxLines = 1) }
                    )
                }
            }

            Text(
                text = when (recordingState.status) {
                    RecordingStatus.IDLE -> "Ready to Record"
                    RecordingStatus.RECORDING -> when (recordingState.sourceMode) {
                        AudioSourceMode.MICROPHONE_ONLY -> "Recording Mic (AAC 16kHz)"
                        AudioSourceMode.INTERNAL_ONLY -> "Recording Internal Audio (PCM 16kHz)"
                        AudioSourceMode.MIC_AND_INTERNAL -> "Recording Mic + Internal (PCM 16kHz)"
                    }
                    RecordingStatus.PAUSED -> "Paused"
                },
                style = MaterialTheme.typography.labelLarge,
                color = when (recordingState.status) {
                    RecordingStatus.RECORDING -> MaterialTheme.colorScheme.error
                    RecordingStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
                    RecordingStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Text(
                text = AudioRecorderService.formatElapsed(recordingState.elapsedSeconds),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold)
            )

            WaveformBar(
                samples = waveformSamples,
                isActive = recordingState.status == RecordingStatus.RECORDING,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            recordingState.errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            when (recordingState.status) {
                RecordingStatus.IDLE -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onStartClick,
                            enabled = !isImporting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Recording")
                        }
                        OutlinedButton(
                            onClick = onImportClick,
                            enabled = !isImporting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import Audio / Video")
                        }
                    }
                }

                RecordingStatus.RECORDING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onPauseClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause")
                        }
                        Button(
                            onClick = onStopClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop")
                        }
                    }
                }

                RecordingStatus.PAUSED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onResumeClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume")
                        }
                        Button(
                            onClick = onStopClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveformBar(
    samples: List<Float>,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = samples.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.6f).coerceAtLeast(2f)
            val centerY = size.height / 2f

            samples.forEachIndexed { index, amplitude ->
                val clamped = amplitude.coerceIn(0f, 1f)
                val barHeight = (size.height * (0.12f + 0.88f * clamped)).coerceAtLeast(4f)
                val x = index * slotWidth + (slotWidth - barWidth) / 2f
                val y = centerY - barHeight / 2f

                drawRoundRect(
                    color = if (isActive && clamped > 0.02f) activeColor else inactiveColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}

@Composable
private fun SavedRecordingRow(
    item: RecordingListItemUiModel,
    folderName: String?,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onMoveToFolderClick: () -> Unit,
    onShareAudioClick: () -> Unit,
    onDeleteAudioFileClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val audioAvailable = remember(item.audioFilePath) { hasLocalAudioFile(item.audioFilePath) }

    val formattedDate = remember(item.createdAtEpochMs) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(item.createdAtEpochMs))
    }
    val formattedDuration = remember(item.durationMs) {
        AudioRecorderService.formatElapsed(item.durationMs / 1000L)
    }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "$formattedDate • $formattedDuration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = onClick,
                        label = { Text(item.status.name) }
                    )
                    if (!folderName.isNullOrBlank()) {
                        AssistChip(
                            onClick = onMoveToFolderClick,
                            label = { Text(folderName) }
                        )
                    }
                }
                val activeStepMessage = item.processingMessage
                if (!activeStepMessage.isNullOrBlank()) {
                    Text(
                        text = activeStepMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Recording options"
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
                            onRenameClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Move to Folder") },
                        leadingIcon = {
                            Icon(imageVector = Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onMoveToFolderClick()
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
                                onDeleteAudioFileClick()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineErrorBanner(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun EmptyRecordingsCard(
    isFilteredByFolder: Boolean,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isFilteredByFolder) {
                    "No Lectures in This Folder"
                } else {
                    "No Lectures Recorded Yet"
                },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (isFilteredByFolder) {
                    "Start a recording while this folder is selected, or move an existing lecture here."
                } else {
                    "Tap Start Recording above to capture microphone or internal device audio, or load a sample lecture in Settings for offline demos."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isFilteredByFolder) {
                OutlinedButton(onClick = onOpenSettings) {
                    Text("Open Settings")
                }
            }
        }
    }
}
