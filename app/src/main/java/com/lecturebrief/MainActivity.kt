package com.lecturebrief

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lecturebrief.ui.detail.DetailRoute
import com.lecturebrief.ui.detail.DetailViewModel
import com.lecturebrief.ui.home.HomeRoute
import com.lecturebrief.ui.home.HomeViewModel
import com.lecturebrief.ui.navigation.AppDestination
import com.lecturebrief.ui.settings.SettingsRoute
import com.lecturebrief.ui.settings.SettingsViewModel
import com.lecturebrief.ui.theme.LectureBriefTheme

/**
 * Single-Activity entry point for LectureBrief.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as LectureBriefApplication).appContainer

        setContent {
            val themeMode by appContainer.themePreferences.themeMode.collectAsStateWithLifecycle()
            LectureBriefTheme(themeMode = themeMode) {
                Surface {
                    val navController = rememberNavController()
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
}
