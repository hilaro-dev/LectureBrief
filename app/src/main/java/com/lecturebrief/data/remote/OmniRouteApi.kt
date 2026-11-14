package com.lecturebrief.data.remote

import com.lecturebrief.data.remote.dto.ChatCompletionRequestDto
import com.lecturebrief.data.remote.dto.ChatCompletionResponseDto
import com.lecturebrief.data.remote.dto.ModelsResponseDto
import com.lecturebrief.data.remote.dto.TranscriptionResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface OmniRouteApi {

    @GET("v1/models")
    suspend fun getModels(): ModelsResponseDto

    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun createTranscription(
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("language") language: RequestBody? = null,
        @Part("prompt") prompt: RequestBody? = null,
        @Part("response_format") responseFormat: RequestBody? = null,
        @Part("temperature") temperature: RequestBody? = null
    ): TranscriptionResponseDto

    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Body request: ChatCompletionRequestDto
    ): ChatCompletionResponseDto
}
