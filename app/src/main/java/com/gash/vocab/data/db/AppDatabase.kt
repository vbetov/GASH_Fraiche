package com.gash.vocab.data.db

import android.content.Context
import android.net.Uri
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import java.io.File

@Database(
    entities = [WordEntity::class, ProgressEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao
    abstract fun progressDao(): ProgressDao

    companion object {
        private const val DB_NAME = "gash_vocab.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }

        /**
         * Copy the current database file to the app's external files directory
         * (accessible via file manager / USB).
         */
        fun backupToExternalStorage(context: Context) {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return

            val baseDir = context.externalMediaDirs.firstOrNull()
            if (baseDir == null || (!baseDir.exists() && !baseDir.mkdirs())) return

            val backupDir = File(baseDir, "backup")
            if (!backupDir.exists()) backupDir.mkdirs()

            val backupFile = File(backupDir, DB_NAME)

            // Checkpoint WAL to ensure all data is in the main db file
            try {
                INSTANCE?.let { db ->
                    val cursor = db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")
                    cursor.moveToFirst()
                    cursor.close()
                }
            } catch (_: Exception) { }

            dbFile.copyTo(backupFile, overwrite = true)

            // Also copy WAL and SHM if they exist
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")
            if (walFile.exists()) walFile.copyTo(File(backupDir, "$DB_NAME-wal"), overwrite = true) else File(backupDir, "$DB_NAME-wal").delete()
            if (shmFile.exists()) shmFile.copyTo(File(backupDir, "$DB_NAME-shm"), overwrite = true) else File(backupDir, "$DB_NAME-shm").delete()
        }

        /**
         * Replace the current database with one selected by the user.
         * The app must be restarted after this operation.
         */
        fun restoreFromUri(context: Context, uri: Uri): Result<Unit> {
            return try {
                // Close the current database
                INSTANCE?.close()
                INSTANCE = null

                val dbFile = context.getDatabasePath(DB_NAME)

                // Delete existing database files
                dbFile.delete()
                File(dbFile.path + "-wal").delete()
                File(dbFile.path + "-shm").delete()
                File(dbFile.path + "-journal").delete()

                // Copy the user's file into the database location
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: return Result.failure(Exception("Could not read selected file"))

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
