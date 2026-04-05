package com.example.voicerecorder.data.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class AudioRecordManager @Inject constructor() {

    companion object {
        const val SAMPLE_RATE = 16_000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BYTES_PER_SAMPLE = 2
        const val CHUNK_DURATION_MS = 20
        const val SAMPLES_PER_CHUNK = SAMPLE_RATE * CHUNK_DURATION_MS / 1000  // 320
        const val BYTES_PER_CHUNK = SAMPLES_PER_CHUNK * BYTES_PER_SAMPLE       // 640
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val _pcmChunkFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 256)
    val pcmChunkFlow: SharedFlow<ByteArray> = _pcmChunkFlow

    private val _amplitudeFlow = MutableSharedFlow<Float>(extraBufferCapacity = 128)
    val amplitudeFlow: SharedFlow<Float> = _amplitudeFlow

    fun start(scope: CoroutineScope) {
        if (audioRecord != null) return

        val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = if (minBufSize <= 0) 4096 else maxOf(minBufSize * 2, BYTES_PER_CHUNK * 4)

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )
        record.startRecording()
        audioRecord = record

        recordingJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(BYTES_PER_CHUNK)
            while (isActive) {
                val bytesRead = record.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    val chunk = buffer.copyOf(bytesRead)
                    _pcmChunkFlow.tryEmit(chunk)
                    _amplitudeFlow.tryEmit(computeRms(chunk))
                }
            }
        }
    }

    suspend fun stop() {
        recordingJob?.cancelAndJoin()
        recordingJob = null
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
    }

    private fun computeRms(pcm: ByteArray): Float {
        if (pcm.size < 2) return 0f
        var sum = 0.0
        var i = 0
        while (i < pcm.size - 1) {
            val sample = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xFF)).toShort()
            sum += sample.toDouble() * sample.toDouble()
            i += 2
        }
        val sampleCount = pcm.size / 2
        return if (sampleCount == 0) 0f else (sqrt(sum / sampleCount) / Short.MAX_VALUE).toFloat()
    }
}
