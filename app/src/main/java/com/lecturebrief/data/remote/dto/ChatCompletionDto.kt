package com.lecturebrief.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequestDto(
    @SerialName("model")
    val model: String,
    @SerialName("messages")
    val messages: List<ChatMessageDto>,
    @SerialName("temperature")
    val temperature: Double = 0.2,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    @SerialName("stream")
    val stream: Boolean = false,
    @SerialName("reasoning_effort")
    val reasoningEffort: String? = null
)

@Serializable
data class ChatMessageDto(
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: String
)

@Serializable
data class ChatCompletionResponseDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("model")
    val model: String? = null,
    @SerialName("choices")
    val choices: List<ChatChoiceDto> = emptyList(),
    @SerialName("usage")
    val usage: TokenUsageDto? = null
)

@Serializable
data class ChatChoiceDto(
    @SerialName("index")
    val index: Int = 0,
    @SerialName("message")
    val message: ChatMessageDto,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class TokenUsageDto(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0
)

@Serializable
data class StructuredLectureSummaryPayload(
    @SerialName("overview_markdown")
    val overviewMarkdown: String = "",
    @SerialName("key_takeaways")
    val keyTakeaways: List<String> = emptyList(),
    @SerialName("action_items")
    val actionItems: List<String> = emptyList()
)
