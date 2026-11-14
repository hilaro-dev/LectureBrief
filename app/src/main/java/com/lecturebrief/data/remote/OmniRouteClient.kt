package com.lecturebrief.data.remote

import com.lecturebrief.BuildConfig
import com.lecturebrief.data.local.prefs.SecureSettingsStorage
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Configures OkHttp + Retrofit for OpenAI-compatible gateways and direct cloud providers.
 */
object OmniRouteClient {

    private const val PLACEHOLDER_RETROFIT_BASE_URL = "http://localhost/"
    const val TARGET_HEADER = "X-LB-Target"
    const val TARGET_STT = "stt"
    const val TARGET_CHAT = "chat"

    fun createOkHttpClient(secureSettingsStorage: SecureSettingsStorage): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(OmniRouteGatewayInterceptor(secureSettingsStorage))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    fun createApi(okHttpClient: OkHttpClient, json: Json): OmniRouteApi {
        val contentType = "application/json; charset=UTF-8".toMediaType()
        return Retrofit.Builder()
            .baseUrl(PLACEHOLDER_RETROFIT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(OmniRouteApi::class.java)
    }
}

/**
 * OkHttp interceptor that dynamically routes each request to either the active STT or
 * Summary (LLM) Base URL + API Key from [SecureSettingsStorage], normalizing path segments
 * so trailing slashes or endpoints like `.../v1` and `.../openai` never produce double slashes.
 */
class OmniRouteGatewayInterceptor(
    private val secureSettingsStorage: SecureSettingsStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val config = secureSettingsStorage.getCurrentConfig()

        val requestSegments = originalRequest.url.encodedPathSegments.filter { it.isNotEmpty() }
        val target = originalRequest.header(OmniRouteClient.TARGET_HEADER)
            ?: if (requestSegments.lastOrNull() == "completions") {
                OmniRouteClient.TARGET_CHAT
            } else {
                OmniRouteClient.TARGET_STT
            }

        val (rawBaseUrl, rawApiKey) = if (target == OmniRouteClient.TARGET_CHAT) {
            config.effectiveChatBaseUrl to config.effectiveChatApiKey
        } else {
            config.baseUrl to config.apiKey
        }

        val normalizedRawUrl = rawBaseUrl.trim().trimEnd('/')
        val configuredBaseUrl = normalizedRawUrl.toHttpUrlOrNull()
            ?: throw IOException("Invalid ${if (target == OmniRouteClient.TARGET_CHAT) "Summary" else "STT"} Base URL: \"$rawBaseUrl\"")

        val baseSegments = configuredBaseUrl.encodedPathSegments.filter { it.isNotEmpty() }
        val effectiveBaseSegments = if (baseSegments.isEmpty()) {
            listOf("v1")
        } else {
            baseSegments
        }

        val urlBuilder = configuredBaseUrl.newBuilder()
        for (i in 0 until configuredBaseUrl.pathSize) {
            urlBuilder.removePathSegment(0)
        }
        (effectiveBaseSegments + requestSegments).forEach { segment ->
            urlBuilder.addEncodedPathSegment(segment)
        }
        urlBuilder.encodedQuery(originalRequest.url.encodedQuery)

        val requestBuilder = originalRequest.newBuilder()
            .url(urlBuilder.build())
            .removeHeader(OmniRouteClient.TARGET_HEADER)
            .header("Accept", "application/json")

        if (rawApiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $rawApiKey")
        }

        return chain.proceed(requestBuilder.build())
    }
}
