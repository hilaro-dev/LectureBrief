# LectureBrief — Architecture & Technical Specification

**LectureBrief** is a native Android application (Kotlin + Jetpack Compose, Min SDK 29) that captures microphone and internal system audio (`AudioPlaybackCapture`), transcribes recordings, and summarizes them via an OpenAI-compatible Bring-Your-Own-Key (BYOK) gateway (**OmniRoute**).

---

## 1. Tech Stack & Strict Constraints

| Layer | Technology | Notes |
|---|---|---|
| **UI** | Jetpack Compose + Material 3 | Single-activity architecture (`MainActivity`) with Compose Navigation |
| **DI** | Manual DI (`AppContainer`) | Instantiated in `LectureBriefApplication`; **NO Hilt/Dagger** |
| **Database** | Room (SQLite) | Async `Flow` queries + `suspend` DAO mutations for recordings & summaries |
| **Secure Preferences** | `EncryptedSharedPreferences` | Stores BYOK API key, base URL, and selected transcription/chat model IDs |
| **Networking** | OkHttp 4 + Retrofit 2 + Kotlinx Serialization | Dynamic auth/base-URL interceptor + multipart audio upload + JSON chat completions |
| **Audio Capture** | Foreground `Service` + `MediaProjection` + `AudioRecord` | Concurrent mic + internal audio (`AudioPlaybackCaptureConfiguration`, API 29+) |
| **Min / Target SDK** | Min SDK 29 / Target SDK 35 | Android 10+ required for `AudioPlaybackCapture` |

---

## 2. Package Structure

All source files live under `app/src/main/java/com/lecturebrief/`:

```text
com.lecturebrief
├── LectureBriefApplication.kt              # Application subclass owning AppContainer
├── MainActivity.kt                         # Single-Activity host for Compose UI
├── di/
│   └── AppContainer.kt                     # Manual Dependency Injection container
├── data/
│   ├── local/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt              # RoomDatabase definition
│   │   │   ├── Converters.kt               # Room TypeConverters (enums, lists)
│   │   │   ├── dao/
│   │   │   │   └── RecordingDao.kt         # CRUD + reactive queries for recordings & summaries
│   │   │   └── entity/
│   │   │       ├── FolderEntity.kt         # Folder metadata for organizing recordings
│   │   │       ├── RecordingEntity.kt      # Metadata, file paths, audio source, status, transcript, folderId
│   │   │       ├── SummaryEntity.kt        # Structured summary linked 1:1 with RecordingEntity
│   │   │       └── RecordingWithSummary.kt # Room @Relation POJO joining Recording + Summary
│   │   └── prefs/
│   │       └── SecureSettingsStorage.kt    # EncryptedSharedPreferences wrapper for OmniRoute credentials
│   ├── remote/
│   │   ├── OmniRouteApi.kt                 # Retrofit interface for /v1/models, /v1/audio/transcriptions, /v1/chat/completions
│   │   ├── DynamicGatewayInterceptor.kt    # OkHttp Interceptor injecting Bearer token & dynamic Base URL
│   │   └── dto/
│   │       ├── ModelsDto.kt                # ModelsResponseDto, ModelDto
│   │       ├── TranscriptionDto.kt         # TranscriptionResponseDto, TranscriptionSegmentDto
│   │       └── ChatCompletionDto.kt        # ChatCompletionRequestDto, ChatMessageDto, ChatCompletionResponseDto
│   └── repository/
│       ├── RecordingRepository.kt          # Mediates Room, audio files, transcription, and summarization
│       └── SettingsRepository.kt           # Mediates EncryptedSharedPreferences and /v1/models validation
├── service/
│   └── AudioCaptureService.kt              # Foreground service (microphone + AudioPlaybackCapture)
└── ui/
    ├── navigation/
    │   └── AppDestination.kt               # Type-safe route definitions for single-activity navigation
    ├── state/
    │   ├── RecordingSessionUiState.kt      # Live capture state (idle, recording, paused, levels, source)
    │   ├── LibraryUiState.kt               # List of recordings, search/filter state
    │   ├── RecordingDetailUiState.kt       # Single recording playback, transcript, summary, processing state
    │   └── SettingsUiState.kt              # BYOK gateway config, fetched models, connection verification state
    └── theme/
        └── Theme.kt                        # Material 3 color scheme & typography
```

---

## 3. Room Entities & Database Schema

### 3.1 Enum Types

```kotlin
package com.lecturebrief.data.local.db.entity

import kotlinx.serialization.Serializable

@Serializable
enum class AudioSourceMode {
    MICROPHONE_ONLY,
    INTERNAL_ONLY,
    MIC_AND_INTERNAL
}

@Serializable
enum class ProcessingStatus {
    RECORDING,
    PENDING,
    TRANSCRIBING,
    SUMMARIZING,
    DONE,
    ERROR
}
```

### 3.2 `FolderEntity` (`folders` table)

```kotlin
package com.lecturebrief.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
```

### 3.3 `RecordingEntity` (`recordings` table)

```kotlin
package com.lecturebrief.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recordings",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["created_at"]),
        Index(value = ["status"]),
        Index(value = ["folderId"])
    ]
)
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    var title: String,

    @ColumnInfo(name = "audio_file_path")
    val audioFilePath: String,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,

    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long,

    @ColumnInfo(name = "audio_source")
    val audioSource: AudioSourceMode,

    @ColumnInfo(name = "status")
    val status: ProcessingStatus,

    @ColumnInfo(name = "folderId")
    val folderId: Long? = null,

    @ColumnInfo(name = "transcript_text")
    val transcriptText: String? = null,

    @ColumnInfo(name = "transcription_model")
    val transcriptionModel: String? = null,

    @ColumnInfo(name = "language")
    val language: String? = null,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAtEpochMs: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAtEpochMs: Long
)
```

### 3.4 Database Migration (`v1` → `v2`) & DAO Operations

`AppDatabase` is at `version = 2` with explicit `MIGRATION_1_2` so existing recordings are preserved across upgrades:
- Creates the `folders` table and unique index `index_folders_name`.
- Runs `ALTER TABLE recordings ADD COLUMN folderId INTEGER DEFAULT NULL REFERENCES folders(id) ON DELETE SET NULL` and creates `index_recordings_folderId`.
- `RecordingDao` exposes `getAllFolders()`, `insertFolder(folder)`, `deleteFolder(folderId)`, `renameRecording(id, newTitle)`, and `moveRecordingToFolder(id, folderId)`.

### 3.3 `SummaryEntity` (`summaries` table)

```kotlin
package com.lecturebrief.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "summaries",
    foreignKeys = [
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["id"],
            childColumns = ["recording_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recording_id"], unique = true)]
)
data class SummaryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "recording_id")
    val recordingId: Long,

    @ColumnInfo(name = "model_id")
    val modelId: String,

    @ColumnInfo(name = "overview_markdown")
    val overviewMarkdown: String,

    @ColumnInfo(name = "key_takeaways_json")
    val keyTakeawaysJson: String, // JSON-encoded List<String>

    @ColumnInfo(name = "action_items_json")
    val actionItemsJson: String,  // JSON-encoded List<String>

    @ColumnInfo(name = "prompt_tokens")
    val promptTokens: Int? = null,

    @ColumnInfo(name = "completion_tokens")
    val completionTokens: Int? = null,

    @ColumnInfo(name = "created_at")
    val createdAtEpochMs: Long
)
```

### 3.4 `RecordingWithSummary` Relation

```kotlin
package com.lecturebrief.data.local.db.entity

import androidx.room.Embedded
import androidx.room.Relation

data class RecordingWithSummary(
    @Embedded
    val recording: RecordingEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "recording_id"
    )
    val summary: SummaryEntity?
)
```

---

## 4. OmniRoute Retrofit API & DTOs

### 4.1 `OmniRouteApi` Interface

```kotlin
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
```

### 4.2 Serialization DTOs (`kotlinx.serialization`)

```kotlin
// ModelsDto.kt
@Serializable
data class ModelsResponseDto(
    @SerialName("object") val objectType: String = "list",
    @SerialName("data") val data: List<ModelDto> = emptyList()
)

@Serializable
data class ModelDto(
    @SerialName("id") val id: String,
    @SerialName("object") val objectType: String = "model",
    @SerialName("created") val created: Long? = null,
    @SerialName("owned_by") val ownedBy: String? = null
)

// TranscriptionDto.kt
@Serializable
data class TranscriptionResponseDto(
    @SerialName("text") val text: String,
    @SerialName("language") val language: String? = null,
    @SerialName("duration") val durationSeconds: Double? = null,
    @SerialName("segments") val segments: List<TranscriptionSegmentDto> = emptyList()
)

@Serializable
data class TranscriptionSegmentDto(
    @SerialName("id") val id: Int,
    @SerialName("start") val startSeconds: Double,
    @SerialName("end") val endSeconds: Double,
    @SerialName("text") val text: String
)

// ChatCompletionDto.kt
@Serializable
data class ChatCompletionRequestDto(
    @SerialName("model") val model: String,
    @SerialName("messages") val messages: List<ChatMessageDto>,
    @SerialName("temperature") val temperature: Double = 0.2,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("stream") val stream: Boolean = false
)

@Serializable
data class ChatMessageDto(
    @SerialName("role") val role: String, // "system" | "user" | "assistant"
    @SerialName("content") val content: String
)

@Serializable
data class ChatCompletionResponseDto(
    @SerialName("id") val id: String? = null,
    @SerialName("model") val model: String? = null,
    @SerialName("choices") val choices: List<ChatChoiceDto> = emptyList(),
    @SerialName("usage") val usage: TokenUsageDto? = null
)

@Serializable
data class ChatChoiceDto(
    @SerialName("index") val index: Int = 0,
    @SerialName("message") val message: ChatMessageDto,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class TokenUsageDto(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0
)
```

---

## 5. UI State Classes (`com.lecturebrief.ui.state`)

All UI state classes are immutable `@Immutable` / `data class` snapshots exposed via `StateFlow` from ViewModels:

### 5.1 `RecordingSessionUiState`
Represents the active recording session (microphone + `AudioPlaybackCapture`):
- `phase`: `SessionPhase` (`IDLE`, `AWAITING_PROJECTION_PERMISSION`, `RECORDING`, `PAUSED`, `SAVING`, `ERROR`)
- `sourceMode`: `AudioSourceMode` (`MICROPHONE_ONLY`, `INTERNAL_ONLY`, `MIC_AND_INTERNAL`)
- `elapsedMs`: `Long`
- `micAmplitude`: `Float` (`0f..1f`)
- `internalAmplitude`: `Float` (`0f..1f`)
- `activeRecordingId`: `Long?`
- `errorMessage`: `String?`

### 5.2 `LibraryUiState`
Represents the main library list of recorded lectures:
- `isLoading`: `Boolean`
- `searchQuery`: `String`
- `statusFilter`: `ProcessingStatus?`
- `recordings`: `List<RecordingListItemUiModel>`
- `errorMessage`: `String?`

### 5.3 `RecordingDetailUiState`
Represents a single lecture's playback, transcript, and AI summary view:
- `isLoading`: `Boolean`
- `recordingId`: `Long`
- `title`: `String`
- `createdAtEpochMs`: `Long`
- `durationMs`: `Long`
- `playbackPositionMs`: `Long`
- `isPlaying`: `Boolean`
- `audioSource`: `AudioSourceMode`
- `status`: `ProcessingStatus`
- `transcriptText`: `String?`
- `summary`: `SummaryUiModel?` (`overviewMarkdown`, `keyTakeaways`, `actionItems`, `modelId`)
- `errorMessage`: `String?`

### 5.4 `SettingsUiState`
Represents the OmniRoute BYOK configuration and model selection state:
- `baseUrl`: `String` (default `https://api.omniroute.io/`)
- `apiKeyMasked`: `String`
- `hasApiKey`: `Boolean`
- `availableModels`: `List<String>`
- `selectedTranscriptionModel`: `String` (e.g., `whisper-1` / `gpt-4o-mini-transcribe`)
- `selectedSummaryModel`: `String` (e.g., `gpt-4o-mini`)
- `systemPrompt`: `String`
- `connectionStatus`: `GatewayConnectionStatus` (`UNTESTED`, `VERIFYING`, `CONNECTED`, `ERROR`)
- `connectionError`: `String?`

---

## 6. Manual Dependency Injection (`AppContainer`)

`AppContainer` is initialized once in `LectureBriefApplication.onCreate()` and provides lazy, thread-safe singletons without codegen overhead:

```text
LectureBriefApplication
 └── appContainer: AppContainer
      ├── json: Json
      ├── secureSettingsStorage: SecureSettingsStorage (EncryptedSharedPreferences)
      ├── database: AppDatabase (Room)
      ├── recordingDao: RecordingDao
      ├── okHttpClient: OkHttpClient (with DynamicGatewayInterceptor)
      ├── omniRouteApi: OmniRouteApi (Retrofit + kotlinx.serialization converter)
      ├── settingsRepository: SettingsRepository
      └── recordingRepository: RecordingRepository
```
