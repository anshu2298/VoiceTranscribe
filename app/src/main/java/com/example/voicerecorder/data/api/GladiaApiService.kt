package com.example.voicerecorder.data.api

import com.example.voicerecorder.data.api.dto.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface GladiaApiService {

    @POST("v2/live")
    suspend fun createLiveSession(@Body request: LiveSessionRequest): LiveSessionResponse

    @Multipart
    @POST("v2/upload")
    suspend fun uploadAudio(@Part audio: MultipartBody.Part): UploadResponse

    @POST("v2/pre-recorded")
    suspend fun submitBatch(@Body request: BatchRequest): BatchSubmitResponse

    @GET("v2/pre-recorded/{id}")
    suspend fun pollBatch(@Path("id") id: String): BatchStatusResponse
}
