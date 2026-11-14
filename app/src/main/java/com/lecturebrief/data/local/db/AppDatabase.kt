package com.lecturebrief.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lecturebrief.data.local.db.dao.RecordingDao
import com.lecturebrief.data.local.db.entity.FolderEntity
import com.lecturebrief.data.local.db.entity.RecordingEntity
import com.lecturebrief.data.local.db.entity.SummaryEntity

@Database(
    entities = [
        RecordingEntity::class,
        SummaryEntity::class,
        FolderEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordingDao(): RecordingDao

    companion object {
        private const val DATABASE_NAME = "lecturebrief.db"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `folders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_folders_name` ON `folders` (`name`)"
                )
                db.execSQL(
                    """
                    ALTER TABLE `recordings`
                    ADD COLUMN `folderId` INTEGER DEFAULT NULL
                    REFERENCES `folders`(`id`) ON DELETE SET NULL
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recordings_folderId` ON `recordings` (`folderId`)"
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recordings_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `audio_file_path` TEXT,
                        `duration_ms` INTEGER NOT NULL,
                        `file_size_bytes` INTEGER NOT NULL,
                        `audio_source` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `folderId` INTEGER DEFAULT NULL REFERENCES `folders`(`id`) ON DELETE SET NULL,
                        `transcript_text` TEXT,
                        `transcription_model` TEXT,
                        `language` TEXT,
                        `error_message` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `recordings_new` (
                        `id`, `title`, `audio_file_path`, `duration_ms`, `file_size_bytes`,
                        `audio_source`, `status`, `folderId`, `transcript_text`,
                        `transcription_model`, `language`, `error_message`, `created_at`, `updated_at`
                    )
                    SELECT
                        `id`, `title`, `audio_file_path`, `duration_ms`, `file_size_bytes`,
                        `audio_source`, `status`, `folderId`, `transcript_text`,
                        `transcription_model`, `language`, `error_message`, `created_at`, `updated_at`
                    FROM `recordings`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `recordings`")
                db.execSQL("ALTER TABLE `recordings_new` RENAME TO `recordings`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recordings_created_at` ON `recordings` (`created_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recordings_status` ON `recordings` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recordings_folderId` ON `recordings` (`folderId`)")
            }
        }

        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}
