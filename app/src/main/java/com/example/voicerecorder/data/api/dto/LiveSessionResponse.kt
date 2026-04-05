package com.example.voicerecorder.data.api.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LiveSessionResponse(
    val id: String,
    val url: String
)
