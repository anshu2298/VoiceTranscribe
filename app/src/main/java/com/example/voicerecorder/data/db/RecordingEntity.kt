package com.example.voicerecorder.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TranscriptionStatus {
    IDLE,
    LIVE_ACTIVE,
    UPLOADING,
    PROCESSING,
    DONE,
    ERROR
}

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val durationMs: Long,
    val filePath: String,
    val fileSizeBytes: Long,
    val liveTranscript: String?,
    val batchTranscript: String?,
    val transcriptionStatus: TranscriptionStatus,
    val gladiaLiveId: String?,
    val gladiaBatchId: String?,
    val language: String?
)
