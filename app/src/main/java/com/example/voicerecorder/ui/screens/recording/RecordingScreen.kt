package com.example.voicerecorder.ui.screens.recording

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voicerecorder.util.formatDuration
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    onRecordingFinished: (String) -> Unit,
    onCancel: () -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(uiState) {
        if (uiState is RecordingUiState.Done) {
            onRecordingFinished((uiState as RecordingUiState.Done).recordingId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Recording") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(32.dp))

            when (val state = uiState) {
                is RecordingUiState.Idle -> {
                    if (!micPermission.status.isGranted) {
                        PermissionSection(
                            shouldShowRationale = micPermission.status.shouldShowRationale,
                            onRequest = { micPermission.launchPermissionRequest() }
                        )
                    } else {
                        IdleContent(onStart = { viewModel.startRecording() })
                    }
                }
                is RecordingUiState.Recording -> {
                    RecordingContent(state = state, onStop = { viewModel.stopRecording() })
                }
                is RecordingUiState.Done -> {
                    // navigation handled by LaunchedEffect
                }
                is RecordingUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun IdleContent(onStart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Tap to start recording",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(48.dp))
        RecordButton(isRecording = false, onClick = onStart)
    }
}

@Composable
private fun RecordingContent(state: RecordingUiState.Recording, onStop: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Timer
        Text(
            text = formatDuration(state.elapsedMs),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Thin,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        // Waveform
        WaveformVisualizer(
            amplitudes = state.amplitudes,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        )

        Spacer(Modifier.height(24.dp))

        // Live transcript area
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 240.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.large
        ) {
            Box(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (state.liveTranscript.isBlank()) {
                    Text(
                        "Transcript will appear here...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = state.liveTranscript,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        if (state.isFinalizing) {
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text(
                "Finalizing...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            RecordButton(isRecording = true, onClick = onStop)
        }
    }
}

@Composable
private fun RecordButton(isRecording: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isRecording) 1f else 0.9f,
        animationSpec = if (isRecording) {
            infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            spring()
        },
        label = "pulse"
    )

    FilledIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(80.dp)
            .scale(scale),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isRecording) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = if (isRecording) "Stop" else "Record",
            modifier = Modifier.size(36.dp),
            tint = if (isRecording) MaterialTheme.colorScheme.onError
            else MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun PermissionSection(shouldShowRationale: Boolean, onRequest: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (shouldShowRationale)
                "Microphone access is required to record audio."
            else
                "Please grant microphone permission to use this app.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) {
            Text("Grant Permission")
        }
    }
}
