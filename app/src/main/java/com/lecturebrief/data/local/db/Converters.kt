package com.lecturebrief.data.local.db

import androidx.room.TypeConverter
import com.lecturebrief.data.local.db.entity.AudioSourceMode
import com.lecturebrief.data.local.db.entity.ProcessingStatus

class Converters {

    @TypeConverter
    fun fromAudioSourceMode(mode: AudioSourceMode): String = mode.name

    @TypeConverter
    fun toAudioSourceMode(value: String): AudioSourceMode =
        runCatching { AudioSourceMode.valueOf(value) }.getOrDefault(AudioSourceMode.MIC_AND_INTERNAL)

    @TypeConverter
    fun fromProcessingStatus(status: ProcessingStatus): String = status.name

    @TypeConverter
    fun toProcessingStatus(value: String): ProcessingStatus =
        runCatching { ProcessingStatus.valueOf(value) }.getOrDefault(ProcessingStatus.PENDING)
}
