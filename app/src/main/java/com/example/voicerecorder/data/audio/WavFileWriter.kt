package com.example.voicerecorder.data.audio

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

class WavFileWriter @Inject constructor() {

    private var outputStream: FileOutputStream? = null
    private var dataByteCount = 0L

    fun open(file: File) {
        outputStream = FileOutputStream(file)
        writeWavHeader(dataSize = 0)
    }

    fun write(pcm: ByteArray) {
        outputStream?.write(pcm)
        dataByteCount += pcm.size
    }

    fun close() {
        outputStream?.let { stream ->
            stream.channel.position(0)
            writeWavHeader(dataSize = dataByteCount)
            stream.flush()
            stream.close()
        }
        outputStream = null
        dataByteCount = 0
    }

    val totalBytes: Long get() = dataByteCount

    private fun writeWavHeader(dataSize: Long) {
        val sampleRate = AudioRecordManager.SAMPLE_RATE
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt((dataSize + 36).toInt())
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize.toInt())
        }.array()

        outputStream?.write(header)
    }
}
