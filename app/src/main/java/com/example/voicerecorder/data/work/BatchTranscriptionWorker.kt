package com.example.voicerecorder.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.voicerecorder.data.api.GladiaApiService
import com.example.voicerecorder.data.db.RecordingDao
import com.example.voicerecorder.data.db.TranscriptionStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay

@HiltWorker
class BatchTranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiService: GladiaApiService,
    private val dao: RecordingDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val recordingId = inputData.getString(KEY_RECORDING_ID) ?: return Result.failure()
        val gladiaBatchId = inputData.getString(KEY_BATCH_ID) ?: return Result.failure()

        repeat(MAX_POLLS) {
            val response = try {
                apiService.pollBatch(gladiaBatchId)
            } catch (e: Exception) {
                delay(POLL_INTERVAL_MS)
                return@repeat
            }

            when (response.status) {
                "done" -> {
                    val transcript = response.result?.transcription?.fullTranscript ?: ""
                    val language = response.result?.metadata?.audioDetectedLanguage
                    dao.updateBatchResult(
                        id = recordingId,
                        transcript = transcript,
                        language = language,
                        status = TranscriptionStatus.DONE.name
                    )
                    dao.rebuildFts(
                        SimpleSQLiteQuery("INSERT INTO recordings_fts(recordings_fts) VALUES('rebuild')")
                    )
                    return Result.success()
                }
                "error" -> {
                    dao.updateStatus(recordingId, TranscriptionStatus.ERROR.name)
                    return Result.failure()
                }
            }
            delay(POLL_INTERVAL_MS)
        }

        return Result.retry()
    }

    companion object {
        const val KEY_RECORDING_ID = "recording_id"
        const val KEY_BATCH_ID = "batch_id"
        const val MAX_POLLS = 36          // 36 × 5s = 3 minutes max wait
        const val POLL_INTERVAL_MS = 5_000L
    }
}
