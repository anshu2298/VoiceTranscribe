package com.example.voicerecorder.ui.screens.recording

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicerecorder.data.audio.AudioRecordManager
import com.example.voicerecorder.data.repository.RecordingRepository
import com.example.voicerecorder.service.RecordingForegroundService
import com.example.voicerecorder.service.RecordingServiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecordingUiState {
    object Idle : RecordingUiState
    data class Recording(
        val recordingId: String,
        val elapsedMs: Long,
        val amplitudes: List<Float>,
        val liveTranscript: String,
        val isFinalizing: Boolean
    ) : RecordingUiState
    data class Done(val recordingId: String) : RecordingUiState
    data class Error(val message: String) : RecordingUiState
}

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRecordManager: AudioRecordManager,
    private val repository: RecordingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecordingUiState>(RecordingUiState.Idle)
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    private var boundService: RecordingForegroundService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as RecordingForegroundService.RecordingBinder).getService()
            boundService = service

            viewModelScope.launch {
                service.recordingState.collect { state ->
                    handleServiceState(state)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
        }
    }

    fun startRecording() {
        val intent = Intent(context, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_START
        }
        context.startForegroundService(intent)

        val bindIntent = Intent(context, RecordingForegroundService::class.java)
        context.bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        viewModelScope.launch {
            audioRecordManager.amplitudeFlow.collect { amp ->
                val current = _uiState.value
                if (current is RecordingUiState.Recording) {
                    val newAmplitudes = (current.amplitudes + amp).takeLast(60)
                    _uiState.value = current.copy(amplitudes = newAmplitudes)
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                val current = _uiState.value
                if (current is RecordingUiState.Recording && !current.isFinalizing) {
                    _uiState.value = current.copy(
                        elapsedMs = current.elapsedMs + 100
                    )
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }

    fun stopRecording() {
        val current = _uiState.value as? RecordingUiState.Recording ?: return
        _uiState.value = current.copy(isFinalizing = true)

        val intent = Intent(context, RecordingForegroundService::class.java).apply {
            action = RecordingForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }

    private fun handleServiceState(state: RecordingServiceState) {
        when (state) {
            is RecordingServiceState.Idle -> {}
            is RecordingServiceState.Recording -> {
                val current = _uiState.value
                if (current is RecordingUiState.Idle) {
                    _uiState.value = RecordingUiState.Recording(
                        recordingId = state.recordingId,
                        elapsedMs = 0,
                        amplitudes = emptyList(),
                        liveTranscript = "",
                        isFinalizing = false
                    )
                    // Observe live transcript from DB
                    viewModelScope.launch {
                        repository.observeRecording(state.recordingId).collect { entity ->
                            val uiStateCurrent = _uiState.value
                            if (uiStateCurrent is RecordingUiState.Recording) {
                                _uiState.value = uiStateCurrent.copy(
                                    liveTranscript = entity?.liveTranscript ?: ""
                                )
                            }
                        }
                    }
                }
            }
            is RecordingServiceState.Finalizing -> {
                val current = _uiState.value
                if (current is RecordingUiState.Recording) {
                    _uiState.value = current.copy(isFinalizing = true)
                }
            }
            is RecordingServiceState.Done -> {
                _uiState.value = RecordingUiState.Done(state.recordingId)
            }
        }
    }

    override fun onCleared() {
        try {
            context.unbindService(serviceConnection)
        } catch (_: Exception) {}
        super.onCleared()
    }
}
