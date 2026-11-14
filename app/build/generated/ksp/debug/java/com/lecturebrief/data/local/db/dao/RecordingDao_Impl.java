package com.lecturebrief.data.local.db.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.RelationUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.lecturebrief.data.local.db.Converters;
import com.lecturebrief.data.local.db.entity.AudioSourceMode;
import com.lecturebrief.data.local.db.entity.FolderEntity;
import com.lecturebrief.data.local.db.entity.ProcessingStatus;
import com.lecturebrief.data.local.db.entity.RecordingEntity;
import com.lecturebrief.data.local.db.entity.RecordingWithSummary;
import com.lecturebrief.data.local.db.entity.SummaryEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class RecordingDao_Impl implements RecordingDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<RecordingEntity> __insertionAdapterOfRecordingEntity;

  private final Converters __converters = new Converters();

  private final EntityInsertionAdapter<SummaryEntity> __insertionAdapterOfSummaryEntity;

  private final EntityInsertionAdapter<FolderEntity> __insertionAdapterOfFolderEntity;

  private final EntityDeletionOrUpdateAdapter<RecordingEntity> __deletionAdapterOfRecordingEntity;

  private final EntityDeletionOrUpdateAdapter<RecordingEntity> __updateAdapterOfRecordingEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateRecordingStatus;

  private final SharedSQLiteStatement __preparedStmtOfUpdateTranscript;

  private final SharedSQLiteStatement __preparedStmtOfDeleteRecordingById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteFolder;

  private final SharedSQLiteStatement __preparedStmtOfRenameRecording;

  private final SharedSQLiteStatement __preparedStmtOfMoveRecordingToFolder;

  private final SharedSQLiteStatement __preparedStmtOfClearAudioFilePath;

  public RecordingDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRecordingEntity = new EntityInsertionAdapter<RecordingEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `recordings` (`id`,`title`,`audio_file_path`,`duration_ms`,`file_size_bytes`,`audio_source`,`status`,`folderId`,`transcript_text`,`transcription_model`,`language`,`error_message`,`created_at`,`updated_at`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RecordingEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        if (entity.getAudioFilePath() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getAudioFilePath());
        }
        statement.bindLong(4, entity.getDurationMs());
        statement.bindLong(5, entity.getFileSizeBytes());
        final String _tmp = __converters.fromAudioSourceMode(entity.getAudioSource());
        statement.bindString(6, _tmp);
        final String _tmp_1 = __converters.fromProcessingStatus(entity.getStatus());
        statement.bindString(7, _tmp_1);
        if (entity.getFolderId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getFolderId());
        }
        if (entity.getTranscriptText() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getTranscriptText());
        }
        if (entity.getTranscriptionModel() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getTranscriptionModel());
        }
        if (entity.getLanguage() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getLanguage());
        }
        if (entity.getErrorMessage() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getErrorMessage());
        }
        statement.bindLong(13, entity.getCreatedAtEpochMs());
        statement.bindLong(14, entity.getUpdatedAtEpochMs());
      }
    };
    this.__insertionAdapterOfSummaryEntity = new EntityInsertionAdapter<SummaryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `summaries` (`id`,`recording_id`,`model_id`,`overview_markdown`,`key_takeaways_json`,`action_items_json`,`prompt_tokens`,`completion_tokens`,`created_at`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SummaryEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getRecordingId());
        statement.bindString(3, entity.getModelId());
        statement.bindString(4, entity.getOverviewMarkdown());
        statement.bindString(5, entity.getKeyTakeawaysJson());
        statement.bindString(6, entity.getActionItemsJson());
        if (entity.getPromptTokens() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getPromptTokens());
        }
        if (entity.getCompletionTokens() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getCompletionTokens());
        }
        statement.bindLong(9, entity.getCreatedAtEpochMs());
      }
    };
    this.__insertionAdapterOfFolderEntity = new EntityInsertionAdapter<FolderEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `folders` (`id`,`name`,`created_at`) VALUES (nullif(?, 0),?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FolderEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindLong(3, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfRecordingEntity = new EntityDeletionOrUpdateAdapter<RecordingEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `recordings` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RecordingEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfRecordingEntity = new EntityDeletionOrUpdateAdapter<RecordingEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `recordings` SET `id` = ?,`title` = ?,`audio_file_path` = ?,`duration_ms` = ?,`file_size_bytes` = ?,`audio_source` = ?,`status` = ?,`folderId` = ?,`transcript_text` = ?,`transcription_model` = ?,`language` = ?,`error_message` = ?,`created_at` = ?,`updated_at` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RecordingEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        if (entity.getAudioFilePath() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getAudioFilePath());
        }
        statement.bindLong(4, entity.getDurationMs());
        statement.bindLong(5, entity.getFileSizeBytes());
        final String _tmp = __converters.fromAudioSourceMode(entity.getAudioSource());
        statement.bindString(6, _tmp);
        final String _tmp_1 = __converters.fromProcessingStatus(entity.getStatus());
        statement.bindString(7, _tmp_1);
        if (entity.getFolderId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getFolderId());
        }
        if (entity.getTranscriptText() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getTranscriptText());
        }
        if (entity.getTranscriptionModel() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getTranscriptionModel());
        }
        if (entity.getLanguage() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getLanguage());
        }
        if (entity.getErrorMessage() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getErrorMessage());
        }
        statement.bindLong(13, entity.getCreatedAtEpochMs());
        statement.bindLong(14, entity.getUpdatedAtEpochMs());
        statement.bindLong(15, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateRecordingStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE recordings\n"
                + "        SET status = ?,\n"
                + "            error_message = ?,\n"
                + "            updated_at = ?\n"
                + "        WHERE id = ?\n"
                + "        ";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateTranscript = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE recordings\n"
                + "        SET transcript_text = ?,\n"
                + "            transcription_model = ?,\n"
                + "            language = ?,\n"
                + "            status = ?,\n"
                + "            error_message = NULL,\n"
                + "            updated_at = ?\n"
                + "        WHERE id = ?\n"
                + "        ";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteRecordingById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM recordings WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteFolder = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM folders WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfRenameRecording = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE recordings\n"
                + "        SET title = ?,\n"
                + "            updated_at = ?\n"
                + "        WHERE id = ?\n"
                + "        ";
        return _query;
      }
    };
    this.__preparedStmtOfMoveRecordingToFolder = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE recordings\n"
                + "        SET folderId = ?,\n"
                + "            updated_at = ?\n"
                + "        WHERE id = ?\n"
                + "        ";
        return _query;
      }
    };
    this.__preparedStmtOfClearAudioFilePath = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE recordings\n"
                + "        SET audio_file_path = NULL,\n"
                + "            file_size_bytes = 0,\n"
                + "            updated_at = ?\n"
                + "        WHERE id = ?\n"
                + "        ";
        return _query;
      }
    };
  }

  @Override
  public Object insertRecording(final RecordingEntity recording,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfRecordingEntity.insertAndReturnId(recording);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertSummary(final SummaryEntity summary,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSummaryEntity.insertAndReturnId(summary);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertFolder(final FolderEntity folder,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfFolderEntity.insertAndReturnId(folder);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRecording(final RecordingEntity recording,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfRecordingEntity.handle(recording);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateRecording(final RecordingEntity recording,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfRecordingEntity.handle(recording);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateRecordingStatus(final long recordingId, final ProcessingStatus status,
      final String errorMessage, final long updatedAtEpochMs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateRecordingStatus.acquire();
        int _argIndex = 1;
        final String _tmp = __converters.fromProcessingStatus(status);
        _stmt.bindString(_argIndex, _tmp);
        _argIndex = 2;
        if (errorMessage == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, errorMessage);
        }
        _argIndex = 3;
        _stmt.bindLong(_argIndex, updatedAtEpochMs);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, recordingId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateRecordingStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTranscript(final long recordingId, final String transcriptText,
      final String modelId, final String language, final ProcessingStatus status,
      final long updatedAtEpochMs, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateTranscript.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, transcriptText);
        _argIndex = 2;
        _stmt.bindString(_argIndex, modelId);
        _argIndex = 3;
        if (language == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, language);
        }
        _argIndex = 4;
        final String _tmp = __converters.fromProcessingStatus(status);
        _stmt.bindString(_argIndex, _tmp);
        _argIndex = 5;
        _stmt.bindLong(_argIndex, updatedAtEpochMs);
        _argIndex = 6;
        _stmt.bindLong(_argIndex, recordingId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateTranscript.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRecordingById(final long recordingId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteRecordingById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, recordingId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteRecordingById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteFolder(final long folderId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteFolder.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, folderId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteFolder.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object renameRecording(final long id, final String newTitle, final long updatedAtEpochMs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRenameRecording.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, newTitle);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, updatedAtEpochMs);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfRenameRecording.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object moveRecordingToFolder(final long id, final Long folderId,
      final long updatedAtEpochMs, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMoveRecordingToFolder.acquire();
        int _argIndex = 1;
        if (folderId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, folderId);
        }
        _argIndex = 2;
        _stmt.bindLong(_argIndex, updatedAtEpochMs);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMoveRecordingToFolder.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAudioFilePath(final long id, final long updatedAtEpochMs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAudioFilePath.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, updatedAtEpochMs);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAudioFilePath.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<RecordingWithSummary>> observeAllRecordingsWithSummary() {
    final String _sql = "SELECT * FROM recordings ORDER BY created_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"summaries",
        "recordings"}, new Callable<List<RecordingWithSummary>>() {
      @Override
      @NonNull
      public List<RecordingWithSummary> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
            final int _cursorIndexOfAudioFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "audio_file_path");
            final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "duration_ms");
            final int _cursorIndexOfFileSizeBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "file_size_bytes");
            final int _cursorIndexOfAudioSource = CursorUtil.getColumnIndexOrThrow(_cursor, "audio_source");
            final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
            final int _cursorIndexOfFolderId = CursorUtil.getColumnIndexOrThrow(_cursor, "folderId");
            final int _cursorIndexOfTranscriptText = CursorUtil.getColumnIndexOrThrow(_cursor, "transcript_text");
            final int _cursorIndexOfTranscriptionModel = CursorUtil.getColumnIndexOrThrow(_cursor, "transcription_model");
            final int _cursorIndexOfLanguage = CursorUtil.getColumnIndexOrThrow(_cursor, "language");
            final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "error_message");
            final int _cursorIndexOfCreatedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
            final int _cursorIndexOfUpdatedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
            final LongSparseArray<SummaryEntity> _collectionSummary = new LongSparseArray<SummaryEntity>();
            while (_cursor.moveToNext()) {
              final long _tmpKey;
              _tmpKey = _cursor.getLong(_cursorIndexOfId);
              _collectionSummary.put(_tmpKey, null);
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshipsummariesAscomLecturebriefDataLocalDbEntitySummaryEntity(_collectionSummary);
            final List<RecordingWithSummary> _result = new ArrayList<RecordingWithSummary>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final RecordingWithSummary _item;
              final RecordingEntity _tmpRecording;
              final long _tmpId;
              _tmpId = _cursor.getLong(_cursorIndexOfId);
              final String _tmpTitle;
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
              final String _tmpAudioFilePath;
              if (_cursor.isNull(_cursorIndexOfAudioFilePath)) {
                _tmpAudioFilePath = null;
              } else {
                _tmpAudioFilePath = _cursor.getString(_cursorIndexOfAudioFilePath);
              }
              final long _tmpDurationMs;
              _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
              final long _tmpFileSizeBytes;
              _tmpFileSizeBytes = _cursor.getLong(_cursorIndexOfFileSizeBytes);
              final AudioSourceMode _tmpAudioSource;
              final String _tmp;
              _tmp = _cursor.getString(_cursorIndexOfAudioSource);
              _tmpAudioSource = __converters.toAudioSourceMode(_tmp);
              final ProcessingStatus _tmpStatus;
              final String _tmp_1;
              _tmp_1 = _cursor.getString(_cursorIndexOfStatus);
              _tmpStatus = __converters.toProcessingStatus(_tmp_1);
              final Long _tmpFolderId;
              if (_cursor.isNull(_cursorIndexOfFolderId)) {
                _tmpFolderId = null;
              } else {
                _tmpFolderId = _cursor.getLong(_cursorIndexOfFolderId);
              }
              final String _tmpTranscriptText;
              if (_cursor.isNull(_cursorIndexOfTranscriptText)) {
                _tmpTranscriptText = null;
              } else {
                _tmpTranscriptText = _cursor.getString(_cursorIndexOfTranscriptText);
              }
              final String _tmpTranscriptionModel;
              if (_cursor.isNull(_cursorIndexOfTranscriptionModel)) {
                _tmpTranscriptionModel = null;
              } else {
                _tmpTranscriptionModel = _cursor.getString(_cursorIndexOfTranscriptionModel);
              }
              final String _tmpLanguage;
              if (_cursor.isNull(_cursorIndexOfLanguage)) {
                _tmpLanguage = null;
              } else {
                _tmpLanguage = _cursor.getString(_cursorIndexOfLanguage);
              }
              final String _tmpErrorMessage;
              if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
                _tmpErrorMessage = null;
              } else {
                _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
              }
              final long _tmpCreatedAtEpochMs;
              _tmpCreatedAtEpochMs = _cursor.getLong(_cursorIndexOfCreatedAtEpochMs);
              final long _tmpUpdatedAtEpochMs;
              _tmpUpdatedAtEpochMs = _cursor.getLong(_cursorIndexOfUpdatedAtEpochMs);
              _tmpRecording = new RecordingEntity(_tmpId,_tmpTitle,_tmpAudioFilePath,_tmpDurationMs,_tmpFileSizeBytes,_tmpAudioSource,_tmpStatus,_tmpFolderId,_tmpTranscriptText,_tmpTranscriptionModel,_tmpLanguage,_tmpErrorMessage,_tmpCreatedAtEpochMs,_tmpUpdatedAtEpochMs);
              final SummaryEntity _tmpSummary;
              final long _tmpKey_1;
              _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
              _tmpSummary = _collectionSummary.get(_tmpKey_1);
              _item = new RecordingWithSummary(_tmpRecording,_tmpSummary);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<RecordingWithSummary> observeRecordingWithSummaryById(final long recordingId) {
    final String _sql = "SELECT * FROM recordings WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, recordingId);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"summaries",
        "recordings"}, new Callable<RecordingWithSummary>() {
      @Override
      @Nullable
      public RecordingWithSummary call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
            final int _cursorIndexOfAudioFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "audio_file_path");
            final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "duration_ms");
            final int _cursorIndexOfFileSizeBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "file_size_bytes");
            final int _cursorIndexOfAudioSource = CursorUtil.getColumnIndexOrThrow(_cursor, "audio_source");
            final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
            final int _cursorIndexOfFolderId = CursorUtil.getColumnIndexOrThrow(_cursor, "folderId");
            final int _cursorIndexOfTranscriptText = CursorUtil.getColumnIndexOrThrow(_cursor, "transcript_text");
            final int _cursorIndexOfTranscriptionModel = CursorUtil.getColumnIndexOrThrow(_cursor, "transcription_model");
            final int _cursorIndexOfLanguage = CursorUtil.getColumnIndexOrThrow(_cursor, "language");
            final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "error_message");
            final int _cursorIndexOfCreatedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
            final int _cursorIndexOfUpdatedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
            final LongSparseArray<SummaryEntity> _collectionSummary = new LongSparseArray<SummaryEntity>();
            while (_cursor.moveToNext()) {
              final long _tmpKey;
              _tmpKey = _cursor.getLong(_cursorIndexOfId);
              _collectionSummary.put(_tmpKey, null);
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshipsummariesAscomLecturebriefDataLocalDbEntitySummaryEntity(_collectionSummary);
            final RecordingWithSummary _result;
            if (_cursor.moveToFirst()) {
              final RecordingEntity _tmpRecording;
              final long _tmpId;
              _tmpId = _cursor.getLong(_cursorIndexOfId);
              final String _tmpTitle;
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
              final String _tmpAudioFilePath;
              if (_cursor.isNull(_cursorIndexOfAudioFilePath)) {
                _tmpAudioFilePath = null;
              } else {
                _tmpAudioFilePath = _cursor.getString(_cursorIndexOfAudioFilePath);
              }
              final long _tmpDurationMs;
              _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
              final long _tmpFileSizeBytes;
              _tmpFileSizeBytes = _cursor.getLong(_cursorIndexOfFileSizeBytes);
              final AudioSourceMode _tmpAudioSource;
              final String _tmp;
              _tmp = _cursor.getString(_cursorIndexOfAudioSource);
              _tmpAudioSource = __converters.toAudioSourceMode(_tmp);
              final ProcessingStatus _tmpStatus;
              final String _tmp_1;
              _tmp_1 = _cursor.getString(_cursorIndexOfStatus);
              _tmpStatus = __converters.toProcessingStatus(_tmp_1);
              final Long _tmpFolderId;
              if (_cursor.isNull(_cursorIndexOfFolderId)) {
                _tmpFolderId = null;
              } else {
                _tmpFolderId = _cursor.getLong(_cursorIndexOfFolderId);
              }
              final String _tmpTranscriptText;
              if (_cursor.isNull(_cursorIndexOfTranscriptText)) {
                _tmpTranscriptText = null;
              } else {
                _tmpTranscriptText = _cursor.getString(_cursorIndexOfTranscriptText);
              }
              final String _tmpTranscriptionModel;
              if (_cursor.isNull(_cursorIndexOfTranscriptionModel)) {
                _tmpTranscriptionModel = null;
              } else {
                _tmpTranscriptionModel = _cursor.getString(_cursorIndexOfTranscriptionModel);
              }
              final String _tmpLanguage;
              if (_cursor.isNull(_cursorIndexOfLanguage)) {
                _tmpLanguage = null;
              } else {
                _tmpLanguage = _cursor.getString(_cursorIndexOfLanguage);
              }
              final String _tmpErrorMessage;
              if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
                _tmpErrorMessage = null;
              } else {
                _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
              }
              final long _tmpCreatedAtEpochMs;
              _tmpCreatedAtEpochMs = _cursor.getLong(_cursorIndexOfCreatedAtEpochMs);
              final long _tmpUpdatedAtEpochMs;
              _tmpUpdatedAtEpochMs = _cursor.getLong(_cursorIndexOfUpdatedAtEpochMs);
              _tmpRecording = new RecordingEntity(_tmpId,_tmpTitle,_tmpAudioFilePath,_tmpDurationMs,_tmpFileSizeBytes,_tmpAudioSource,_tmpStatus,_tmpFolderId,_tmpTranscriptText,_tmpTranscriptionModel,_tmpLanguage,_tmpErrorMessage,_tmpCreatedAtEpochMs,_tmpUpdatedAtEpochMs);
              final SummaryEntity _tmpSummary;
              final long _tmpKey_1;
              _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
              _tmpSummary = _collectionSummary.get(_tmpKey_1);
              _result = new RecordingWithSummary(_tmpRecording,_tmpSummary);
            } else {
              _result = null;
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getRecordingById(final long recordingId,
      final Continuation<? super RecordingEntity> $completion) {
    final String _sql = "SELECT * FROM recordings WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, recordingId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<RecordingEntity>() {
      @Override
      @Nullable
      public RecordingEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfAudioFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "audio_file_path");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "duration_ms");
          final int _cursorIndexOfFileSizeBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "file_size_bytes");
          final int _cursorIndexOfAudioSource = CursorUtil.getColumnIndexOrThrow(_cursor, "audio_source");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfFolderId = CursorUtil.getColumnIndexOrThrow(_cursor, "folderId");
          final int _cursorIndexOfTranscriptText = CursorUtil.getColumnIndexOrThrow(_cursor, "transcript_text");
          final int _cursorIndexOfTranscriptionModel = CursorUtil.getColumnIndexOrThrow(_cursor, "transcription_model");
          final int _cursorIndexOfLanguage = CursorUtil.getColumnIndexOrThrow(_cursor, "language");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "error_message");
          final int _cursorIndexOfCreatedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final int _cursorIndexOfUpdatedAtEpochMs = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final RecordingEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpAudioFilePath;
            if (_cursor.isNull(_cursorIndexOfAudioFilePath)) {
              _tmpAudioFilePath = null;
            } else {
              _tmpAudioFilePath = _cursor.getString(_cursorIndexOfAudioFilePath);
            }
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final long _tmpFileSizeBytes;
            _tmpFileSizeBytes = _cursor.getLong(_cursorIndexOfFileSizeBytes);
            final AudioSourceMode _tmpAudioSource;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfAudioSource);
            _tmpAudioSource = __converters.toAudioSourceMode(_tmp);
            final ProcessingStatus _tmpStatus;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toProcessingStatus(_tmp_1);
            final Long _tmpFolderId;
            if (_cursor.isNull(_cursorIndexOfFolderId)) {
              _tmpFolderId = null;
            } else {
              _tmpFolderId = _cursor.getLong(_cursorIndexOfFolderId);
            }
            final String _tmpTranscriptText;
            if (_cursor.isNull(_cursorIndexOfTranscriptText)) {
              _tmpTranscriptText = null;
            } else {
              _tmpTranscriptText = _cursor.getString(_cursorIndexOfTranscriptText);
            }
            final String _tmpTranscriptionModel;
            if (_cursor.isNull(_cursorIndexOfTranscriptionModel)) {
              _tmpTranscriptionModel = null;
            } else {
              _tmpTranscriptionModel = _cursor.getString(_cursorIndexOfTranscriptionModel);
            }
            final String _tmpLanguage;
            if (_cursor.isNull(_cursorIndexOfLanguage)) {
              _tmpLanguage = null;
            } else {
              _tmpLanguage = _cursor.getString(_cursorIndexOfLanguage);
            }
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            final long _tmpCreatedAtEpochMs;
            _tmpCreatedAtEpochMs = _cursor.getLong(_cursorIndexOfCreatedAtEpochMs);
            final long _tmpUpdatedAtEpochMs;
            _tmpUpdatedAtEpochMs = _cursor.getLong(_cursorIndexOfUpdatedAtEpochMs);
            _result = new RecordingEntity(_tmpId,_tmpTitle,_tmpAudioFilePath,_tmpDurationMs,_tmpFileSizeBytes,_tmpAudioSource,_tmpStatus,_tmpFolderId,_tmpTranscriptText,_tmpTranscriptionModel,_tmpLanguage,_tmpErrorMessage,_tmpCreatedAtEpochMs,_tmpUpdatedAtEpochMs);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<FolderEntity>> getAllFolders() {
    final String _sql = "SELECT * FROM folders ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"folders"}, new Callable<List<FolderEntity>>() {
      @Override
      @NonNull
      public List<FolderEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<FolderEntity> _result = new ArrayList<FolderEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FolderEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new FolderEntity(_tmpId,_tmpName,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private void __fetchRelationshipsummariesAscomLecturebriefDataLocalDbEntitySummaryEntity(
      @NonNull final LongSparseArray<SummaryEntity> _map) {
    if (_map.isEmpty()) {
      return;
    }
    if (_map.size() > RoomDatabase.MAX_BIND_PARAMETER_CNT) {
      RelationUtil.recursiveFetchLongSparseArray(_map, false, (map) -> {
        __fetchRelationshipsummariesAscomLecturebriefDataLocalDbEntitySummaryEntity(map);
        return Unit.INSTANCE;
      });
      return;
    }
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT `id`,`recording_id`,`model_id`,`overview_markdown`,`key_takeaways_json`,`action_items_json`,`prompt_tokens`,`completion_tokens`,`created_at` FROM `summaries` WHERE `recording_id` IN (");
    final int _inputSize = _map.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _stmt = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (int i = 0; i < _map.size(); i++) {
      final long _item = _map.keyAt(i);
      _stmt.bindLong(_argIndex, _item);
      _argIndex++;
    }
    final Cursor _cursor = DBUtil.query(__db, _stmt, false, null);
    try {
      final int _itemKeyIndex = CursorUtil.getColumnIndex(_cursor, "recording_id");
      if (_itemKeyIndex == -1) {
        return;
      }
      final int _cursorIndexOfId = 0;
      final int _cursorIndexOfRecordingId = 1;
      final int _cursorIndexOfModelId = 2;
      final int _cursorIndexOfOverviewMarkdown = 3;
      final int _cursorIndexOfKeyTakeawaysJson = 4;
      final int _cursorIndexOfActionItemsJson = 5;
      final int _cursorIndexOfPromptTokens = 6;
      final int _cursorIndexOfCompletionTokens = 7;
      final int _cursorIndexOfCreatedAtEpochMs = 8;
      while (_cursor.moveToNext()) {
        final long _tmpKey;
        _tmpKey = _cursor.getLong(_itemKeyIndex);
        if (_map.containsKey(_tmpKey)) {
          final SummaryEntity _item_1;
          final long _tmpId;
          _tmpId = _cursor.getLong(_cursorIndexOfId);
          final long _tmpRecordingId;
          _tmpRecordingId = _cursor.getLong(_cursorIndexOfRecordingId);
          final String _tmpModelId;
          _tmpModelId = _cursor.getString(_cursorIndexOfModelId);
          final String _tmpOverviewMarkdown;
          _tmpOverviewMarkdown = _cursor.getString(_cursorIndexOfOverviewMarkdown);
          final String _tmpKeyTakeawaysJson;
          _tmpKeyTakeawaysJson = _cursor.getString(_cursorIndexOfKeyTakeawaysJson);
          final String _tmpActionItemsJson;
          _tmpActionItemsJson = _cursor.getString(_cursorIndexOfActionItemsJson);
          final Integer _tmpPromptTokens;
          if (_cursor.isNull(_cursorIndexOfPromptTokens)) {
            _tmpPromptTokens = null;
          } else {
            _tmpPromptTokens = _cursor.getInt(_cursorIndexOfPromptTokens);
          }
          final Integer _tmpCompletionTokens;
          if (_cursor.isNull(_cursorIndexOfCompletionTokens)) {
            _tmpCompletionTokens = null;
          } else {
            _tmpCompletionTokens = _cursor.getInt(_cursorIndexOfCompletionTokens);
          }
          final long _tmpCreatedAtEpochMs;
          _tmpCreatedAtEpochMs = _cursor.getLong(_cursorIndexOfCreatedAtEpochMs);
          _item_1 = new SummaryEntity(_tmpId,_tmpRecordingId,_tmpModelId,_tmpOverviewMarkdown,_tmpKeyTakeawaysJson,_tmpActionItemsJson,_tmpPromptTokens,_tmpCompletionTokens,_tmpCreatedAtEpochMs);
          _map.put(_tmpKey, _item_1);
        }
      }
    } finally {
      _cursor.close();
    }
  }
}
