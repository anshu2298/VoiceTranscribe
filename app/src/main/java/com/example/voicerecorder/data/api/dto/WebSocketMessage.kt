package com.example.voicerecorder.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WebSocketMessage(
    val type: String?,
    val data: TranscriptData?
)

@JsonClass(generateAdapter = true)
data class TranscriptData(
    @Json(name = "is_final") val isFinal: Boolean,
    val utterance: WsUtterance?
)

@JsonClass(generateAdapter = true)
data class WsUtterance(
    val text: String?,
    val confidence: Float?
)
