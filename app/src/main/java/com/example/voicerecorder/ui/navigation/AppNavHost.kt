package com.example.voicerecorder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.voicerecorder.ui.screens.detail.RecordingDetailScreen
import com.example.voicerecorder.ui.screens.recording.RecordingScreen
import com.example.voicerecorder.ui.screens.recordingslist.RecordingsListScreen
import com.example.voicerecorder.ui.screens.settings.SettingsScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.RecordingsList.route) {

        composable(Screen.RecordingsList.route) {
            RecordingsListScreen(
                onNavigateToRecording = { navController.navigate(Screen.Recording.route) },
                onNavigateToDetail = { id ->
                    navController.navigate(Screen.RecordingDetail.createRoute(id))
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Recording.route) {
            RecordingScreen(
                onRecordingFinished = { id ->
                    navController.navigate(Screen.RecordingDetail.createRoute(id)) {
                        popUpTo(Screen.Recording.route) { inclusive = true }
                    }
                },
                onCancel = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.RecordingDetail.route,
            arguments = listOf(navArgument("recordingId") { type = NavType.StringType })
        ) {
            RecordingDetailScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.navigateUp() })
        }
    }
}
