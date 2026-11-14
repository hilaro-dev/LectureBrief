package com.lecturebrief.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.lecturebrief.data.local.db.entity.FolderEntity
import com.lecturebrief.data.local.db.entity.ProcessingStatus
import com.lecturebrief.data.local.db.entity.RecordingEntity
import com.lecturebrief.data.local.db.entity.RecordingWithSummary
import com.lecturebrief.data.local.db.entity.SummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Transaction
    @Query("SELECT * FROM recordings ORDER BY created_at DESC")
    fun observeAllRecordingsWithSummary(): Flow<List<RecordingWithSummary>>

    @Transaction
    @Query("SELECT * FROM recordings WHERE id = :recordingId LIMIT 1")
    fun observeRecordingWithSummaryById(recordingId: Long): Flow<RecordingWithSummary?>

    @Query("SELECT * FROM recordings WHERE id = :recordingId LIMIT 1")
    suspend fun getRecordingById(recordingId: Long): RecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity): Long

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Query(
        """
        UPDATE recordings
        SET status = :status,
            error_message = :errorMessage,
            updated_at = :updatedAtEpochMs
        WHERE id = :recordingId
        """
    )
    suspend fun updateRecordingStatus(
        recordingId: Long,
        status: ProcessingStatus,
        errorMessage: String? = null,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE recordings
        SET transcript_text = :transcriptText,
            transcription_model = :modelId,
            language = :language,
            status = :status,
            error_message = NULL,
            updated_at = :updatedAtEpochMs
        WHERE id = :recordingId
        """
    )
    suspend fun updateTranscript(
        recordingId: Long,
        transcriptText: String,
        modelId: String,
        language: String?,
        status: ProcessingStatus = ProcessingStatus.SUMMARIZING,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: SummaryEntity): Long

    @Delete
    suspend fun deleteRecording(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :recordingId")
    suspend fun deleteRecordingById(recordingId: Long)

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Long)

    @Query(
        """
        UPDATE recordings
        SET title = :newTitle,
            updated_at = :updatedAtEpochMs
        WHERE id = :id
        """
    )
    suspend fun renameRecording(
        id: Long,
        newTitle: String,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE recordings
        SET folderId = :folderId,
            updated_at = :updatedAtEpochMs
        WHERE id = :id
        """
    )
    suspend fun moveRecordingToFolder(
        id: Long,
        folderId: Long?,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE recordings
        SET audio_file_path = NULL,
            file_size_bytes = 0,
            updated_at = :updatedAtEpochMs
        WHERE id = :id
        """
    )
    suspend fun clearAudioFilePath(
        id: Long,
        updatedAtEpochMs: Long = System.currentTimeMillis()
    )
}
