package com.example.voicerecorder.data.repository

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.work.*
import com.example.voicerecorder.data.api.GladiaApiService
import com.example.voicerecorder.data.api.GladiaLiveStreamManager
import com.example.voicerecorder.data.api.TranscriptToken
import com.example.voicerecorder.data.api.dto.BatchRequest
import com.example.voicerecorder.data.audio.AudioRecordManager
import com.example.voicerecorder.data.audio.WavFileWriter
import com.example.voicerecorder.data.db.RecordingDao
import com.example.voicerecorder.data.db.RecordingEntity
import com.example.voicerecorder.data.db.TranscriptionStatus
import com.example.voicerecorder.data.work.BatchTranscriptionWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepository @Inject constructor(
    private val dao: RecordingDao,
    private val liveStreamManager: GladiaLiveStreamManager,
    private val audioRecordManager: AudioRecordManager,
    private val wavFileWriter: WavFileWriter,
    private val apiService: GladiaApiService,
    private val workManager: WorkManager,
    @ApplicationContext private val context: Context
) {
    val allRecordings: Flow<List<RecordingEntity>> = dao.observeAll()

    fun searchRecordings(query: String): Flow<List<RecordingEntity>> =
        if (query.isBlank()) allRecordings else dao.search(query.toFtsQuery())

    fun observeRecording(id: String): Flow<RecordingEntity?> = dao.observeById(id)

    suspend fun startRecording(serviceScope: CoroutineScope): String {
        val id = UUID.randomUUID().toString()
        val wavFile = File(context.filesDir, "$id.wav")
        val title = buildTitle()

        dao.insert(
            RecordingEntity(
                id = id,
                title = title,
                createdAt = System.currentTimeMillis(),
                durationMs = 0L,
                filePath = wavFile.absolutePath,
                fileSizeBytes = 0L,
                liveTranscript = null,
                batchTranscript = null,
                transcriptionStatus = TranscriptionStatus.LIVE_ACTIVE,
                gladiaLiveId = null,
                gladiaBatchId = null,
                language = null
            )
        )

        wavFileWriter.open(wavFile)

        val gladiaLiveId = try {
            liveStreamManager.startSession()
        } catch (e: Exception) {
            null
        }

        gladiaLiveId?.let {
            dao.getById(id)?.let { entity ->
                dao.update(entity.copy(gladiaLiveId = it))
            }
        }

        audioRecordManager.start(serviceScope)

        serviceScope.launch(Dispatchers.IO) {
            audioRecordManager.pcmChunkFlow.collect { chunk ->
                liveStreamManager.sendPcmChunk(chunk)
                wavFileWriter.write(chunk)
            }
        }

        return id
    }

    suspend fun stopRecording(recordingId: String, startTimeMs: Long) {
        audioRecordManager.stop()
        liveStreamManager.stopSession()
        wavFileWriter.close()

        val durationMs = System.currentTimeMillis() - startTimeMs
        val wavFile = dao.getById(recordingId)?.filePath?.let { File(it) } ?: return
        val sizeBytes = wavFile.length()

        dao.updateStatus(recordingId, TranscriptionStatus.UPLOADING.name)

        val batchId = try {
            val requestBody = wavFile.asRequestBody("audio/wav".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("audio", wavFile.name, requestBody)
            val uploadResponse = apiService.uploadAudio(part)

            dao.updateStatus(recordingId, TranscriptionStatus.PROCESSING.name)

            val batchResponse = apiService.submitBatch(BatchRequest(audioUrl = uploadResponse.audioUrl))
            batchResponse.id
        } catch (e: Exception) {
            dao.updateStatus(recordingId, TranscriptionStatus.ERROR.name)
            return
        }

        dao.updateAfterStop(
            id = recordingId,
            batchId = batchId,
            sizeBytes = sizeBytes,
            durationMs = durationMs,
            status = TranscriptionStatus.PROCESSING.name
        )

        enqueueBatchWorker(recordingId, batchId)
    }

    suspend fun appendLiveTranscript(recordingId: String, token: TranscriptToken) {
        val current = dao.getById(recordingId)?.liveTranscript ?: ""
        val updated = if (token.isFinal) {
            if (current.isBlank()) token.text else "$current ${token.text}"
        } else {
            current
        }
        dao.updateLiveTranscript(
            id = recordingId,
            transcript = updated,
            status = TranscriptionStatus.LIVE_ACTIVE.name
        )
        dao.rebuildFts(SimpleSQLiteQuery("INSERT INTO recordings_fts(recordings_fts) VALUES('rebuild')"))
    }

    suspend fun deleteRecording(id: String) {
        val entity = dao.getById(id)
        entity?.filePath?.let { File(it).delete() }
        dao.deleteById(id)
    }

    private fun enqueueBatchWorker(recordingId: String, batchId: String) {
        val data = workDataOf(
            BatchTranscriptionWorker.KEY_RECORDING_ID to recordingId,
            BatchTranscriptionWorker.KEY_BATCH_ID to batchId
        )
        val request = OneTimeWorkRequestBuilder<BatchTranscriptionWorker>()
            .setInputData(data)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniqueWork(
            "batch_$recordingId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun buildTitle(): String {
        val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        return "Recording – ${sdf.format(Date())}"
    }

    private fun String.toFtsQuery(): String =
        trim().split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .joinToString(" ") { "\"${it.replace("\"", "")}*\"" }
}
