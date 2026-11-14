package com.lecturebrief.data.repository

import com.lecturebrief.data.local.db.dao.RecordingDao
import com.lecturebrief.data.local.db.entity.AudioSourceMode
import com.lecturebrief.data.local.db.entity.FolderEntity
import com.lecturebrief.data.local.db.entity.ProcessingStatus
import com.lecturebrief.data.local.db.entity.RecordingEntity
import com.lecturebrief.data.local.db.entity.RecordingWithSummary
import com.lecturebrief.data.local.db.entity.SummaryEntity
import com.lecturebrief.data.local.prefs.SecureSettingsStorage
import com.lecturebrief.data.remote.OmniRouteApi
import com.lecturebrief.data.remote.dto.ChatCompletionRequestDto
import com.lecturebrief.data.remote.dto.ChatMessageDto
import com.lecturebrief.data.remote.dto.StructuredLectureSummaryPayload
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class RecordingRepository(
    private val recordingDao: RecordingDao,
    private val omniRouteApi: OmniRouteApi,
    private val secureSettingsStorage: SecureSettingsStorage,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun observeAllRecordings(): Flow<List<RecordingWithSummary>> =
        recordingDao.observeAllRecordingsWithSummary()

    fun observeRecordingById(recordingId: Long): Flow<RecordingWithSummary?> =
        recordingDao.observeRecordingWithSummaryById(recordingId)

    fun observeAllFolders(): Flow<List<FolderEntity>> =
        recordingDao.getAllFolders()

    suspend fun createFolder(name: String): Long = withContext(ioDispatcher) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Folder name cannot be empty" }
        recordingDao.insertFolder(
            FolderEntity(
                name = trimmed,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteFolder(folderId: Long) = withContext(ioDispatcher) {
        recordingDao.deleteFolder(folderId)
    }

    suspend fun renameRecording(id: Long, newTitle: String) = withContext(ioDispatcher) {
        val trimmed = newTitle.trim()
        if (trimmed.isNotEmpty()) {
            recordingDao.renameRecording(id = id, newTitle = trimmed)
        }
    }

    suspend fun moveRecordingToFolder(id: Long, folderId: Long?) = withContext(ioDispatcher) {
        recordingDao.moveRecordingToFolder(id = id, folderId = folderId)
    }

    suspend fun createRecordingEntry(
        title: String,
        audioFilePath: String,
        audioSource: AudioSourceMode,
        folderId: Long? = null,
        status: ProcessingStatus = ProcessingStatus.RECORDING
    ): Long = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        recordingDao.insertRecording(
            RecordingEntity(
                title = title,
                audioFilePath = audioFilePath,
                durationMs = 0L,
                fileSizeBytes = 0L,
                audioSource = audioSource,
                status = status,
                folderId = folderId,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
    }

    suspend fun createImportedRecording(
        title: String,
        audioFilePath: String,
        durationMs: Long,
        fileSizeBytes: Long,
        folderId: Long?
    ): Long = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        recordingDao.insertRecording(
            RecordingEntity(
                title = title,
                audioFilePath = audioFilePath,
                durationMs = durationMs,
                fileSizeBytes = fileSizeBytes,
                audioSource = AudioSourceMode.INTERNAL_ONLY,
                status = ProcessingStatus.PENDING,
                folderId = folderId,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
    }

    suspend fun finalizeRecordedAudio(
        recordingId: Long,
        durationMs: Long,
        fileSizeBytes: Long
    ) = withContext(ioDispatcher) {
        val existing = recordingDao.getRecordingById(recordingId) ?: return@withContext
        recordingDao.updateRecording(
            existing.copy(
                durationMs = durationMs,
                fileSizeBytes = fileSizeBytes,
                status = ProcessingStatus.PENDING,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun transcribeRecording(recordingId: Long): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val recording = recordingDao.getRecordingById(recordingId)
                ?: error("Recording $recordingId not found")
            val audioPath = recording.audioFilePath
                ?: error("Audio file has been deleted")
            val audioFile = File(audioPath)
            require(audioFile.exists()) { "Audio file missing at $audioPath" }

            val config = secureSettingsStorage.getCurrentConfig()
            recordingDao.updateRecordingStatus(
                recordingId = recordingId,
                status = ProcessingStatus.TRANSCRIBING
            )

            val mimeType = when (audioFile.extension.lowercase()) {
                "m4a", "mp4" -> "audio/mp4"
                "mp3" -> "audio/mpeg"
                else -> "audio/wav"
            }
            val fileRequestBody = audioFile.asRequestBody(mimeType.toMediaType())
            val filePart = MultipartBody.Part.createFormData(
                name = "file",
                filename = audioFile.name,
                body = fileRequestBody
            )
            val modelBody = config.sttModel.toRequestBody("text/plain".toMediaType())

            val response = omniRouteApi.createTranscription(
                file = filePart,
                model = modelBody
            )

            recordingDao.updateTranscript(
                recordingId = recordingId,
                transcriptText = response.text,
                modelId = config.sttModel,
                language = response.language,
                status = ProcessingStatus.SUMMARIZING
            )
            response.text
        }.onFailure { throwable ->
            recordingDao.updateRecordingStatus(
                recordingId = recordingId,
                status = ProcessingStatus.ERROR,
                errorMessage = throwable.message ?: "Transcription failed"
            )
        }
    }

    suspend fun summarizeRecording(recordingId: Long): Result<SummaryEntity> = withContext(ioDispatcher) {
        runCatching {
            val recording = recordingDao.getRecordingById(recordingId)
                ?: error("Recording $recordingId not found")
            val transcript = recording.transcriptText?.takeIf { it.isNotBlank() }
                ?: error("Transcript is empty for recording $recordingId")

            val config = secureSettingsStorage.getCurrentConfig()
            recordingDao.updateRecordingStatus(
                recordingId = recordingId,
                status = ProcessingStatus.SUMMARIZING
            )

            val promptInstruction = """
                ${config.systemPrompt}
                Respond strictly with a JSON object containing keys:
                - "overview_markdown": string
                - "key_takeaways": array of strings
                - "action_items": array of strings
            """.trimIndent()

            val chatResponse = omniRouteApi.createChatCompletion(
                ChatCompletionRequestDto(
                    model = config.chatModel,
                    messages = listOf(
                        ChatMessageDto(role = "system", content = promptInstruction),
                        ChatMessageDto(role = "user", content = transcript)
                    ),
                    temperature = 0.2
                )
            )

            val rawContent = chatResponse.choices.firstOrNull()?.message?.content?.trim()
                ?: error("Empty summary response from OmniRoute")

            val cleanedJson = rawContent
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsedPayload = runCatching {
                json.decodeFromString<StructuredLectureSummaryPayload>(cleanedJson)
            }.getOrElse {
                StructuredLectureSummaryPayload(
                    overviewMarkdown = rawContent,
                    keyTakeaways = emptyList(),
                    actionItems = emptyList()
                )
            }

            val summaryEntity = SummaryEntity(
                recordingId = recordingId,
                modelId = chatResponse.model ?: config.chatModel,
                overviewMarkdown = parsedPayload.overviewMarkdown,
                keyTakeawaysJson = json.encodeToString(parsedPayload.keyTakeaways),
                actionItemsJson = json.encodeToString(parsedPayload.actionItems),
                promptTokens = chatResponse.usage?.promptTokens,
                completionTokens = chatResponse.usage?.completionTokens,
                createdAtEpochMs = System.currentTimeMillis()
            )

            recordingDao.upsertSummary(summaryEntity)
            recordingDao.updateRecordingStatus(
                recordingId = recordingId,
                status = ProcessingStatus.DONE
            )
            summaryEntity
        }.onFailure { throwable ->
            recordingDao.updateRecordingStatus(
                recordingId = recordingId,
                status = ProcessingStatus.ERROR,
                errorMessage = throwable.message ?: "Summarization failed"
            )
        }
    }

    suspend fun insertSampleLecture(): Long = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val sampleTranscript = """
            Welcome everyone to Lecture 7 on Distributed Systems and Consensus Protocols.
            Today we will cover why single-leader replication struggles under network partitions,
            and how Raft solves leader election and log replication using randomized election timeouts.
            Remember that Homework 3 on implementing a Raft state machine is due next Friday at 11:59 PM.
            Also, please review Chapter 6 of Kleppmann's Designing Data-Intensive Applications before Monday's lab.
            Let's start by walking through the three states a Raft node can be in: Follower, Candidate, and Leader.
        """.trimIndent()

        val sampleMarkdown = """
            ## TL;DR
            This lecture introduces consensus in distributed systems using the **Raft** protocol, explaining how randomized election timeouts prevent split votes and how log entries are committed via majority quorum.

            ## Key Concepts
            - **Network Partitions**: Split-brain scenarios occur when nodes cannot communicate; consensus requires a strict majority quorum (`N/2 + 1`).
            - **Raft Node States**: Every node operates as a **Follower**, **Candidate**, or **Leader** within monotonically increasing **Terms**.
            - **Leader Election**: Followers transition to Candidates when an election timeout expires; randomized timeouts (`150ms–300ms`) minimize split votes.
            - **Log Replication**: The Leader appends client commands to its log and commits them once replicated to a majority of followers.

            ## Action Items / Deadlines
            - **Homework 3 (Raft State Machine)**: Due next Friday at **11:59 PM**.
            - **Reading**: Review **Chapter 6** of *Designing Data-Intensive Applications* before Monday's lab.

            ## Detailed Notes
            ### 1. Why Consensus Matters
            Single-leader replication is simple when the leader is healthy, but failover requires agreement on who the new leader is to avoid stale reads or conflicting writes.
            ### 2. Raft Terms & Safety
            Each term acts as a logical clock. A node with a higher term always supersedes stale messages from an older term, ensuring at most one leader is elected per term.
        """.trimIndent()

        val recordingId = recordingDao.insertRecording(
            RecordingEntity(
                title = "CS 416 • Distributed Consensus (Sample)",
                audioFilePath = "sample://distributed_consensus.m4a",
                durationMs = 42 * 60_000L,
                fileSizeBytes = 19_840_000L,
                audioSource = AudioSourceMode.MIC_AND_INTERNAL,
                status = ProcessingStatus.DONE,
                transcriptText = sampleTranscript,
                transcriptionModel = "whisper-large-v3",
                language = "en",
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )

        recordingDao.upsertSummary(
            SummaryEntity(
                recordingId = recordingId,
                modelId = "auto",
                overviewMarkdown = sampleMarkdown,
                keyTakeawaysJson = "[]",
                actionItemsJson = "[]",
                createdAtEpochMs = now
            )
        )

        recordingId
    }

    suspend fun deleteAudioFileOnly(recordingId: Long) = withContext(ioDispatcher) {
        val existing = recordingDao.getRecordingById(recordingId) ?: return@withContext
        existing.audioFilePath?.let { path ->
            runCatching { File(path).delete() }
        }
        recordingDao.clearAudioFilePath(recordingId)
    }

    suspend fun deleteRecording(recordingId: Long) = withContext(ioDispatcher) {
        val existing = recordingDao.getRecordingById(recordingId)
        if (existing != null) {
            existing.audioFilePath?.let { path ->
                runCatching { File(path).delete() }
            }
            recordingDao.deleteRecordingById(recordingId)
        }
    }
}
