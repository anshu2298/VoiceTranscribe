package com.example.voicerecorder.service

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.voicerecorder.MainActivity
import com.example.voicerecorder.R
import com.example.voicerecorder.data.api.GladiaLiveStreamManager
import com.example.voicerecorder.data.api.TranscriptToken
import com.example.voicerecorder.data.repository.RecordingRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@AndroidEntryPoint
class RecordingForegroundService : Service() {

    @Inject lateinit var repository: RecordingRepository
    @Inject lateinit var liveStreamManager: GladiaLiveStreamManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _recordingState = MutableStateFlow<RecordingServiceState>(RecordingServiceState.Idle)
    val recordingState: StateFlow<RecordingServiceState> = _recordingState

    inner class RecordingBinder : Binder() {
        fun getService(): RecordingForegroundService = this@RecordingForegroundService
    }

    private val binder = RecordingBinder()

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        startForeground(NOTIFICATION_ID, buildNotification())

        serviceScope.launch {
            val id = repository.startRecording(serviceScope)
            val startTime = System.currentTimeMillis()

            _recordingState.value = RecordingServiceState.Recording(
                recordingId = id,
                startTimeMs = startTime
            )

            // Collect live transcript tokens
            liveStreamManager.transcriptFlow
                .onEach { token -> repository.appendLiveTranscript(id, token) }
                .launchIn(this)
        }
    }

    fun stopRecording() {
        val state = _recordingState.value as? RecordingServiceState.Recording ?: return

        serviceScope.launch {
            _recordingState.value = RecordingServiceState.Finalizing(state.recordingId)
            repository.stopRecording(state.recordingId, state.startTimeMs)
            _recordingState.value = RecordingServiceState.Done(state.recordingId)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_recording_title))
            .setContentText(getString(R.string.notification_recording_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_START = "com.example.voicerecorder.ACTION_START"
        const val ACTION_STOP = "com.example.voicerecorder.ACTION_STOP"
        const val CHANNEL_ID = "recording_channel"
        const val NOTIFICATION_ID = 1001
    }
}

sealed interface RecordingServiceState {
    object Idle : RecordingServiceState
    data class Recording(val recordingId: String, val startTimeMs: Long) : RecordingServiceState
    data class Finalizing(val recordingId: String) : RecordingServiceState
    data class Done(val recordingId: String) : RecordingServiceState
}
