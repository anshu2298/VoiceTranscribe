package com.example.voicerecorder.data.api

import com.example.voicerecorder.data.api.dto.LiveSessionRequest
import com.example.voicerecorder.data.api.dto.WebSocketMessage
import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import javax.inject.Inject
import javax.inject.Singleton

data class TranscriptToken(
    val text: String,
    val isFinal: Boolean,
    val confidence: Float?
)

@Singleton
class GladiaLiveStreamManager @Inject constructor(
    private val apiService: GladiaApiService,
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) {
    private var webSocket: WebSocket? = null
    private val adapter by lazy { moshi.adapter(WebSocketMessage::class.java) }

    private val _transcriptFlow = MutableSharedFlow<TranscriptToken>(
        replay = 0,
        extraBufferCapacity = 128
    )
    val transcriptFlow: SharedFlow<TranscriptToken> = _transcriptFlow

    private val _errorFlow = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val errorFlow: SharedFlow<String> = _errorFlow

    suspend fun startSession(): String {
        val response = apiService.createLiveSession(LiveSessionRequest())
        val request = Request.Builder().url(response.url).build()
        webSocket = okHttpClient.newWebSocket(request, createListener())
        return response.id
    }

    fun sendPcmChunk(pcm: ByteArray) {
        webSocket?.send(pcm.toByteString())
    }

    suspend fun stopSession() {
        webSocket?.send("""{"type":"stop_recording"}""")
        delay(500)
        webSocket?.close(1000, "Recording stopped")
        webSocket = null
    }

    private fun createListener() = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            val message = try {
                adapter.fromJson(text)
            } catch (e: Exception) {
                null
            } ?: return

            val utteranceText = message.data?.utterance?.text ?: return
            if (utteranceText.isBlank()) return

            _transcriptFlow.tryEmit(
                TranscriptToken(
                    text = utteranceText,
                    isFinal = message.data.isFinal,
                    confidence = message.data.utterance?.confidence
                )
            )
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _errorFlow.tryEmit(t.message ?: "WebSocket error")
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            // Gladia sends text frames; binary frames are not expected from server
        }
    }
}
