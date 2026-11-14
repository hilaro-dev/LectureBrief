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
 * Configures OkHttp + Retrofit for the OpenAI-compatible OmniRoute BYOK gateway.
 */
object OmniRouteClient {

    private const val PLACEHOLDER_RETROFIT_BASE_URL = "http://localhost/"

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
 * OkHttp interceptor that dynamically reads `baseUrl` from [SecureSettingsStorage]
 * and injects `Authorization: Bearer <apiKey>` only when `apiKey` is non-blank.
 */
class OmniRouteGatewayInterceptor(
    private val secureSettingsStorage: SecureSettingsStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val config = secureSettingsStorage.getCurrentConfig()

        val configuredBaseUrl = config.baseUrl.toHttpUrlOrNull()
            ?: throw IOException("Invalid OmniRoute Base URL: \"${config.baseUrl}\"")

        val baseSegments = configuredBaseUrl.encodedPathSegments.filter { it.isNotEmpty() }
        val requestSegments = originalRequest.url.encodedPathSegments.filter { it.isNotEmpty() }

        // Prevent duplicate /v1 when configured baseUrl already ends with /v1
        // and the Retrofit endpoint starts with v1/...
        val effectiveBaseSegments = if (
            baseSegments.lastOrNull() == "v1" && requestSegments.firstOrNull() == "v1"
        ) {
            baseSegments.dropLast(1)
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
            .header("Accept", "application/json")

        if (config.apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer ${config.apiKey}")
        }

        return chain.proceed(requestBuilder.build())
    }
}
