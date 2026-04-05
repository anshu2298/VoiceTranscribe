package com.example.voicerecorder.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BatchStatusResponse(
    val id: String,
    val status: String,
    val result: BatchResult?
)

@JsonClass(generateAdapter = true)
data class BatchResult(
    val transcription: BatchTranscription?,
    val metadata: BatchMetadata?
)

@JsonClass(generateAdapter = true)
data class BatchTranscription(
    @Json(name = "full_transcript") val fullTranscript: String?
)

@JsonClass(generateAdapter = true)
data class BatchMetadata(
    @Json(name = "audio_detected_language") val audioDetectedLanguage: String?
)
