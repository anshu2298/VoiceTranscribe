package com.example.voicerecorder.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BatchRequest(
    @Json(name = "audio_url") val audioUrl: String,
    val diarization: Boolean = true
)
