package com.example.voicerecorder.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BatchSubmitResponse(
    val id: String,
    @Json(name = "result_url") val resultUrl: String?
)
