package com.lecturebrief.di

import android.content.Context
import com.lecturebrief.data.local.db.AppDatabase
import com.lecturebrief.data.local.db.dao.RecordingDao
import com.lecturebrief.data.local.prefs.SecureSettingsStorage
import com.lecturebrief.data.local.prefs.ThemePreferences
import com.lecturebrief.data.media.AudioChunker
import com.lecturebrief.data.media.AudioCompressor
import com.lecturebrief.data.media.AudioExtractor
import com.lecturebrief.data.remote.OmniRouteApi
import com.lecturebrief.data.remote.OmniRouteClient
import com.lecturebrief.data.repository.ProcessingRepository
import com.lecturebrief.data.repository.RecordingRepository
import com.lecturebrief.data.repository.SettingsRepository
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

/**
 * Manual Dependency Injection Container initialized in [com.lecturebrief.LectureBriefApplication].
 * Strictly avoids Hilt/Dagger per architectural constraints.
 */
interface AppContainer {
    val json: Json
    val secureSettingsStorage: SecureSettingsStorage
    val themePreferences: ThemePreferences
    val database: AppDatabase
    val recordingDao: RecordingDao
    val okHttpClient: OkHttpClient
    val omniRouteApi: OmniRouteApi
    val settingsRepository: SettingsRepository
    val recordingRepository: RecordingRepository
    val processingRepository: ProcessingRepository
    val audioCompressor: AudioCompressor
    val audioChunker: AudioChunker
    val audioExtractor: AudioExtractor
}

class DefaultAppContainer(
    private val applicationContext: Context
) : AppContainer {

    override val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
        }
    }

    override val secureSettingsStorage: SecureSettingsStorage by lazy {
        SecureSettingsStorage(applicationContext)
    }

    override val themePreferences: ThemePreferences by lazy {
        ThemePreferences(applicationContext)
    }

    override val database: AppDatabase by lazy {
        AppDatabase.create(applicationContext)
    }

    override val recordingDao: RecordingDao by lazy {
        database.recordingDao()
    }

    override val okHttpClient: OkHttpClient by lazy {
        OmniRouteClient.createOkHttpClient(secureSettingsStorage)
    }

    override val omniRouteApi: OmniRouteApi by lazy {
        OmniRouteClient.createApi(okHttpClient = okHttpClient, json = json)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(
            secureSettingsStorage = secureSettingsStorage,
            omniRouteApi = omniRouteApi
        )
    }

    override val recordingRepository: RecordingRepository by lazy {
        RecordingRepository(
            recordingDao = recordingDao,
            omniRouteApi = omniRouteApi,
            secureSettingsStorage = secureSettingsStorage,
            json = json
        )
    }

    override val audioCompressor: AudioCompressor by lazy {
        AudioCompressor(applicationContext)
    }

    override val audioChunker: AudioChunker by lazy {
        AudioChunker(applicationContext)
    }

    override val processingRepository: ProcessingRepository by lazy {
        ProcessingRepository(
            recordingDao = recordingDao,
            omniRouteApi = omniRouteApi,
            secureSettingsStorage = secureSettingsStorage,
            audioCompressor = audioCompressor,
            audioChunker = audioChunker
        )
    }

    override val audioExtractor: AudioExtractor by lazy {
        AudioExtractor(
            context = applicationContext,
            audioCompressor = audioCompressor
        )
    }
}
