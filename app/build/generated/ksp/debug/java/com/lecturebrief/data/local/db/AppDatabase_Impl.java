package com.lecturebrief.data.local.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.lecturebrief.data.local.db.dao.RecordingDao;
import com.lecturebrief.data.local.db.dao.RecordingDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile RecordingDao _recordingDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `recordings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `audio_file_path` TEXT, `duration_ms` INTEGER NOT NULL, `file_size_bytes` INTEGER NOT NULL, `audio_source` TEXT NOT NULL, `status` TEXT NOT NULL, `folderId` INTEGER, `transcript_text` TEXT, `transcription_model` TEXT, `language` TEXT, `error_message` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, FOREIGN KEY(`folderId`) REFERENCES `folders`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recordings_created_at` ON `recordings` (`created_at`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recordings_status` ON `recordings` (`status`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recordings_folderId` ON `recordings` (`folderId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `summaries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recording_id` INTEGER NOT NULL, `model_id` TEXT NOT NULL, `overview_markdown` TEXT NOT NULL, `key_takeaways_json` TEXT NOT NULL, `action_items_json` TEXT NOT NULL, `prompt_tokens` INTEGER, `completion_tokens` INTEGER, `created_at` INTEGER NOT NULL, FOREIGN KEY(`recording_id`) REFERENCES `recordings`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_summaries_recording_id` ON `summaries` (`recording_id`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `created_at` INTEGER NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_folders_name` ON `folders` (`name`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'ab6126a33124d547585ae51bd6b1be48')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `recordings`");
        db.execSQL("DROP TABLE IF EXISTS `summaries`");
        db.execSQL("DROP TABLE IF EXISTS `folders`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsRecordings = new HashMap<String, TableInfo.Column>(14);
        _columnsRecordings.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecordings.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecordings.put("audio_file_path", new TableInfo.Column("audio_file_path", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecordings.put("duration_ms", new TableInfo.Column("duration_ms", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecordings.put("file_size_bytes", new TableInfo.Column("file_size_bytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecordings.put("audio_source", new TableInfo.Column("audio_source", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecordings.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecordings.put("folderId", new TableInfo.Column("folderId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecordings.put("transcript_text", new TableInfo.Column("transcript_text", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecordings.put("transcription_model", new TableInfo.Column("transcription_model", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecordings.put("language", new TableInfo.Column("language", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecordings.put("error_message", new TableInfo.Column("error_message", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecordings.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecordings.put("updated_at", new TableInfo.Column("updated_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRecordings = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysRecordings.add(new TableInfo.ForeignKey("folders", "SET NULL", "NO ACTION", Arrays.asList("folderId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesRecordings = new HashSet<TableInfo.Index>(3);
        _indicesRecordings.add(new TableInfo.Index("index_recordings_created_at", false, Arrays.asList("created_at"), Arrays.asList("ASC")));
        _indicesRecordings.add(new TableInfo.Index("index_recordings_status", false, Arrays.asList("status"), Arrays.asList("ASC")));
        _indicesRecordings.add(new TableInfo.Index("index_recordings_folderId", false, Arrays.asList("folderId"), Arrays.asList("ASC")));
        final TableInfo _infoRecordings = new TableInfo("recordings", _columnsRecordings, _foreignKeysRecordings, _indicesRecordings);
        final TableInfo _existingRecordings = TableInfo.read(db, "recordings");
        if (!_infoRecordings.equals(_existingRecordings)) {
          return new RoomOpenHelper.ValidationResult(false, "recordings(com.lecturebrief.data.local.db.entity.RecordingEntity).\n"
                  + " Expected:\n" + _infoRecordings + "\n"
                  + " Found:\n" + _existingRecordings);
        }
        final HashMap<String, TableInfo.Column> _columnsSummaries = new HashMap<String, TableInfo.Column>(9);
        _columnsSummaries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSummaries.put("recording_id", new TableInfo.Column("recording_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSummaries.put("model_id", new TableInfo.Column("model_id", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSummaries.put("overview_markdown", new TableInfo.Column("overview_markdown", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSummaries.put("key_takeaways_json", new TableInfo.Column("key_takeaways_json", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSummaries.put("action_items_json", new TableInfo.Column("action_items_json", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSummaries.put("prompt_tokens", new TableInfo.Column("prompt_tokens", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSummaries.put("completion_tokens", new TableInfo.Column("completion_tokens", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSummaries.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSummaries = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysSummaries.add(new TableInfo.ForeignKey("recordings", "CASCADE", "NO ACTION", Arrays.asList("recording_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesSummaries = new HashSet<TableInfo.Index>(1);
        _indicesSummaries.add(new TableInfo.Index("index_summaries_recording_id", true, Arrays.asList("recording_id"), Arrays.asList("ASC")));
        final TableInfo _infoSummaries = new TableInfo("summaries", _columnsSummaries, _foreignKeysSummaries, _indicesSummaries);
        final TableInfo _existingSummaries = TableInfo.read(db, "summaries");
        if (!_infoSummaries.equals(_existingSummaries)) {
          return new RoomOpenHelper.ValidationResult(false, "summaries(com.lecturebrief.data.local.db.entity.SummaryEntity).\n"
                  + " Expected:\n" + _infoSummaries + "\n"
                  + " Found:\n" + _existingSummaries);
        }
        final HashMap<String, TableInfo.Column> _columnsFolders = new HashMap<String, TableInfo.Column>(3);
        _columnsFolders.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFolders.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFolders.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFolders = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFolders = new HashSet<TableInfo.Index>(1);
        _indicesFolders.add(new TableInfo.Index("index_folders_name", true, Arrays.asList("name"), Arrays.asList("ASC")));
        final TableInfo _infoFolders = new TableInfo("folders", _columnsFolders, _foreignKeysFolders, _indicesFolders);
        final TableInfo _existingFolders = TableInfo.read(db, "folders");
        if (!_infoFolders.equals(_existingFolders)) {
          return new RoomOpenHelper.ValidationResult(false, "folders(com.lecturebrief.data.local.db.entity.FolderEntity).\n"
                  + " Expected:\n" + _infoFolders + "\n"
                  + " Found:\n" + _existingFolders);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "ab6126a33124d547585ae51bd6b1be48", "6c6581dab65a507ae653729b799baecf");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "recordings","summaries","folders");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `recordings`");
      _db.execSQL("DELETE FROM `summaries`");
      _db.execSQL("DELETE FROM `folders`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(RecordingDao.class, RecordingDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public RecordingDao recordingDao() {
    if (_recordingDao != null) {
      return _recordingDao;
    } else {
      synchronized(this) {
        if(_recordingDao == null) {
          _recordingDao = new RecordingDao_Impl(this);
        }
        return _recordingDao;
      }
    }
  }
}
