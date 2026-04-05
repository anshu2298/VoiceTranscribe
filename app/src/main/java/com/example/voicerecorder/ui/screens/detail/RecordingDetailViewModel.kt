package com.example.voicerecorder.ui.screens.detail

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.voicerecorder.data.db.RecordingEntity
import com.example.voicerecorder.data.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject

enum class PlaybackState { STOPPED, PLAYING, PAUSED }

@HiltViewModel
class RecordingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RecordingRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val recordingId: String = checkNotNull(savedStateHandle["recordingId"])

    val recording: StateFlow<RecordingEntity?> =
        repository.observeRecording(recordingId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _playbackState = MutableStateFlow(PlaybackState.STOPPED)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private var exoPlayer: ExoPlayer? = null

    fun initPlayer() {
        val filePath = recording.value?.filePath ?: return
        val file = File(filePath)
        if (!file.exists()) return

        val player = ExoPlayer.Builder(context).build().also {
            it.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            it.prepare()
        }
        exoPlayer = player

        viewModelScope.launch {
            while (true) {
                _playbackPositionMs.value = player.currentPosition
                if (!player.isPlaying && _playbackState.value == PlaybackState.PLAYING) {
                    _playbackState.value = PlaybackState.STOPPED
                    _playbackPositionMs.value = 0L
                }
                delay(200)
            }
        }
    }

    fun playPause() {
        val player = exoPlayer ?: return
        when (_playbackState.value) {
            PlaybackState.STOPPED, PlaybackState.PAUSED -> {
                player.play()
                _playbackState.value = PlaybackState.PLAYING
            }
            PlaybackState.PLAYING -> {
                player.pause()
                _playbackState.value = PlaybackState.PAUSED
            }
        }
    }

    fun seekTo(fraction: Float) {
        val player = exoPlayer ?: return
        val duration = player.duration.takeIf { it > 0 } ?: return
        player.seekTo((fraction * duration).toLong())
    }

    fun shareTranscript() {
        val text = recording.value?.batchTranscript ?: recording.value?.liveTranscript ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share transcript").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun exportTranscript(uri: Uri, contentResolver: ContentResolver) {
        val text = recording.value?.batchTranscript ?: recording.value?.liveTranscript ?: return
        viewModelScope.launch {
            contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(text.toByteArray(Charsets.UTF_8))
            }
        }
    }

    override fun onCleared() {
        exoPlayer?.release()
        exoPlayer = null
        super.onCleared()
    }
}
