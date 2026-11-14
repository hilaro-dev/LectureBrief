package com.lecturebrief.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranscriptionResponseDto(
    @SerialName("text")
    val text: String,
    @SerialName("language")
    val language: String? = null,
    @SerialName("duration")
    val durationSeconds: Double? = null,
    @SerialName("segments")
    val segments: List<TranscriptionSegmentDto> = emptyList()
)

@Serializable
data class TranscriptionSegmentDto(
    @SerialName("id")
    val id: Int = 0,
    @SerialName("start")
    val startSeconds: Double = 0.0,
    @SerialName("end")
    val endSeconds: Double = 0.0,
    @SerialName("text")
    val text: String = ""
)
