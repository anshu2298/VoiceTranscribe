package com.example.voicerecorder.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LiveSessionRequest(
    val encoding: String = "wav/pcm",
    @Json(name = "bit_depth") val bitDepth: Int = 16,
    @Json(name = "sample_rate") val sampleRate: Int = 16000,
    val channels: Int = 1
)
