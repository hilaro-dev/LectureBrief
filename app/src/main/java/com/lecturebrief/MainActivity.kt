package com.lecturebrief

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lecturebrief.di.AppContainer
import com.lecturebrief.ui.detail.DetailRoute
import com.lecturebrief.ui.detail.DetailViewModel
import com.lecturebrief.ui.home.HomeRoute
import com.lecturebrief.ui.home.HomeViewModel
import com.lecturebrief.ui.navigation.AppDestination
import com.lecturebrief.ui.settings.SettingsRoute
import com.lecturebrief.ui.settings.SettingsViewModel
import com.lecturebrief.ui.theme.LectureBriefTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Single-Activity entry point for LectureBrief.
 */
class MainActivity : ComponentActivity() {

    private val pendingNavigateRecordingId = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as LectureBriefApplication).appContainer
        handleIncomingShareIntent(intent, appContainer)

        setContent {
            val themeMode by appContainer.themePreferences.themeMode.collectAsStateWithLifecycle()
            val colorPalette by appContainer.themePreferences.colorPalette.collectAsStateWithLifecycle()
            val sharedRecordingId by pendingNavigateRecordingId.collectAsStateWithLifecycle()
            LectureBriefTheme(
                themeMode = themeMode,
                colorPalette = colorPalette
            ) {
                Surface {
                    val navController = rememberNavController()
                    LaunchedEffect(sharedRecordingId) {
                        val targetId = sharedRecordingId ?: return@LaunchedEffect
                        pendingNavigateRecordingId.value = null
                        navController.navigate(AppDestination.RecordingDetail(targetId)) {
                            popUpTo(AppDestination.Library) { inclusive = false }
                            launchSingleTop = true
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = AppDestination.Library
                    ) {
                        composable<AppDestination.Library> {
                            val homeViewModel: HomeViewModel = viewModel(
                                factory = HomeViewModel.Factory(
                                    recordingRepository = appContainer.recordingRepository,
                                    processingRepository = appContainer.processingRepository,
                                    audioExtractor = appContainer.audioExtractor
                                )
                            )
                            HomeRoute(
                                viewModel = homeViewModel,
                                onNavigateToSettings = {
                                    navController.navigate(AppDestination.Settings)
                                },
                                onRecordingClick = { recordingId ->
                                    navController.navigate(AppDestination.RecordingDetail(recordingId))
                                }
                            )
                        }

                        composable<AppDestination.RecordingDetail> { backStackEntry ->
                            val destination = backStackEntry.toRoute<AppDestination.RecordingDetail>()
                            val detailViewModel: DetailViewModel = viewModel(
                                factory = DetailViewModel.Factory(
                                    recordingId = destination.recordingId,
                                    recordingRepository = appContainer.recordingRepository,
                                    processingRepository = appContainer.processingRepository
                                )
                            )
                            DetailRoute(
                                viewModel = detailViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable<AppDestination.Settings> {
                            val settingsViewModel: SettingsViewModel = viewModel(
                                factory = SettingsViewModel.Factory(
                                    settingsRepository = appContainer.settingsRepository,
                                    recordingRepository = appContainer.recordingRepository,
                                    themePreferences = appContainer.themePreferences
                                )
                            )
                            SettingsRoute(
                                viewModel = settingsViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val appContainer = (application as LectureBriefApplication).appContainer
        handleIncomingShareIntent(intent, appContainer)
    }

    private fun handleIncomingShareIntent(intent: Intent?, appContainer: AppContainer) {
        if (intent?.action != Intent.ACTION_SEND) return

        val sharedUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            ?: return
        val fallbackMime = intent.type

        // Clear handled intent extras so configuration changes do not re-import the same media.
        intent.removeExtra(Intent.EXTRA_STREAM)
        intent.clipData = null
        intent.action = null

        lifecycleScope.launch {
            val stagedFile = appContainer.audioExtractor.stageSharedMedia(sharedUri, fallbackMime)
                .getOrElse { throwable ->
                    Toast.makeText(
                        this@MainActivity,
                        throwable.message ?: "Unable to read shared audio/video file",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

            try {
                appContainer.audioExtractor.extractAudio(Uri.fromFile(stagedFile))
                    .onSuccess { extracted ->
                        val recordingId = appContainer.recordingRepository.createImportedRecording(
                            title = extracted.displayTitle,
                            audioFilePath = extracted.file.absolutePath,
                            durationMs = extracted.durationMs,
                            fileSizeBytes = extracted.file.length(),
                            folderId = null
                        )
                        appContainer.processingRepository.enqueueProcessing(recordingId)
                        pendingNavigateRecordingId.value = recordingId
                    }
                    .onFailure { throwable ->
                        Toast.makeText(
                            this@MainActivity,
                            throwable.message ?: "Failed to import shared media",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            } finally {
                runCatching { stagedFile.delete() }
                runCatching { stagedFile.parentFile?.delete() }
            }
        }
    }
}
