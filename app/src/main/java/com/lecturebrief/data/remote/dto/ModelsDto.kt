package com.lecturebrief.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModelsResponseDto(
    @SerialName("object")
    val objectType: String = "list",
    @SerialName("data")
    val data: List<ModelDto> = emptyList()
)

@Serializable
data class ModelDto(
    @SerialName("id")
    val id: String,
    @SerialName("object")
    val objectType: String = "model",
    @SerialName("created")
    val created: Long? = null,
    @SerialName("owned_by")
    val ownedBy: String? = null
)
