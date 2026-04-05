package com.example.voicerecorder.data.db

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "recordings_fts")
@Fts4(contentEntity = RecordingEntity::class)
data class RecordingFtsEntity(
    val title: String,
    val liveTranscript: String,
    val batchTranscript: String
)
