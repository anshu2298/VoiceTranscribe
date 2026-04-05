package com.example.voicerecorder.data.db

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter

@ProvidedTypeConverter
class Converters {
    @TypeConverter
    fun fromStatus(value: String): TranscriptionStatus = TranscriptionStatus.valueOf(value)

    @TypeConverter
    fun toStatus(status: TranscriptionStatus): String = status.name
}
