package com.example.voicerecorder.ui.navigation

sealed class Screen(val route: String) {
    object RecordingsList : Screen("recordings_list")
    object Recording : Screen("recording")
    object Settings : Screen("settings")
    object RecordingDetail : Screen("recording_detail/{recordingId}") {
        fun createRoute(id: String) = "recording_detail/$id"
    }
}
