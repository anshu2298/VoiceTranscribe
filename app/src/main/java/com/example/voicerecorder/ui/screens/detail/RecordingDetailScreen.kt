package com.example.voicerecorder.ui.screens.detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voicerecorder.data.db.RecordingEntity
import com.example.voicerecorder.data.db.TranscriptionStatus
import com.example.voicerecorder.util.formatDuration
import com.example.voicerecorder.util.formatFileSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecordingDetailViewModel = hiltViewModel()
) {
    val recording by viewModel.recording.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val positionMs by viewModel.playbackPositionMs.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let { viewModel.exportTranscript(it, context.contentResolver) }
    }

    LaunchedEffect(recording?.filePath) {
        if (recording != null) viewModel.initPlayer()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = recording?.title ?: "Recording",
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.shareTranscript() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share transcript")
                    }
                    IconButton(onClick = {
                        exportLauncher.launch("transcript_${recording?.id ?: "export"}.txt")
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export transcript")
                    }
                }
            )
        }
    ) { paddingValues ->
        recording?.let { rec ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(16.dp))

                RecordingMetadata(rec)

                Spacer(Modifier.height(16.dp))

                PlayerSection(
                    durationMs = rec.durationMs,
                    positionMs = positionMs,
                    playbackState = playbackState,
                    onPlayPause = { viewModel.playPause() },
                    onSeek = { viewModel.seekTo(it) }
                )

                Spacer(Modifier.height(24.dp))

                TranscriptSection(rec)

                Spacer(Modifier.height(32.dp))
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun RecordingMetadata(rec: RecordingEntity) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rec.durationMs > 0) {
            InfoChip(icon = Icons.Default.Timer, label = formatDuration(rec.durationMs))
        }
        if (rec.fileSizeBytes > 0) {
            InfoChip(icon = Icons.Default.Storage, label = formatFileSize(rec.fileSizeBytes))
        }
        rec.language?.let {
            InfoChip(icon = Icons.Default.Language, label = it.uppercase())
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PlayerSection(
    durationMs: Long,
    positionMs: Long,
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f

            Slider(
                value = progress,
                onValueChange = onSeek,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatDuration(positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatDuration(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (playbackState == PlaybackState.PLAYING)
                        Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playbackState == PlaybackState.PLAYING) "Pause" else "Play",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun TranscriptSection(rec: RecordingEntity) {
    val transcript = rec.batchTranscript ?: rec.liveTranscript

    Text(
        text = "Transcript",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(Modifier.height(8.dp))

    when (rec.transcriptionStatus) {
        TranscriptionStatus.DONE -> {
            if (transcript.isNullOrBlank()) {
                Text(
                    "No transcript available.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = transcript,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        TranscriptionStatus.PROCESSING, TranscriptionStatus.UPLOADING -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                    "Processing transcription...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!transcript.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = transcript,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        TranscriptionStatus.ERROR -> {
            Text(
                "Transcription failed.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!transcript.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = transcript,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        else -> {
            if (!transcript.isNullOrBlank()) {
                Text(text = transcript, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
