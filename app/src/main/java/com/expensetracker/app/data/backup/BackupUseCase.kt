package com.expensetracker.app.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.expensetracker.app.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val BACKUP_APP_ID = "com.expensetracker.app"
private const val BACKUP_VERSION = 1
private const val MANIFEST_ENTRY = "manifest.json"
private const val DATABASE_ENTRY = "expense_tracker.db"
private const val MAX_DB_BYTES = 50L * 1024 * 1024 // safety cap on the staged database

data class RestoreInfo(
    val bytes: Long,
    val createdAt: Long,
)

/**
 * Tags a staged restore so it is applied during the next app process start,
 * before Room opens the database. This avoids touching a live Open connection.
 */
object BackupPendingRestore {
    private const val KEY = "pending_restore_db"

    private fun prefs(context: Context) =
        context.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)

    fun mark(context: Context, stagedFilePath: String) {
        prefs(context).edit().putString(KEY, stagedFilePath).apply()
    }

    fun has(context: Context): Boolean = prefs(context).getString(KEY, null) != null

    /**
     * Must run before any database connection is opened. Replaces the live
     * database file with the staged backup and clears the stale -wal/-shm so
     * the self-contained restored database opens cleanly.
     */
    fun apply(context: Context) {
        val prefs = prefs(context)
        val path = prefs.getString(KEY, null) ?: return
        val staged = File(path)
        try {
            if (staged.exists()) {
                val target = context.getDatabasePath(AppDatabase.DATABASE_NAME)
                target.parentFile?.mkdirs()
                File(target.absolutePath + "-wal").delete()
                File(target.absolutePath + "-shm").delete()
                staged.copyTo(target, overwrite = true)
            }
        } finally {
            prefs.edit().remove(KEY).apply()
            staged.delete()
        }
    }
}

@Singleton
class BackupUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /**
     * Writes a self-contained backup (manifest.json + checkpointed database)
     * into [outputStream], e.g. a URI returned by the SAF save picker.
     */
    suspend fun exportBackup(outputStream: OutputStream): Long = withContext(Dispatchers.IO) {
        val appDb = AppDatabase.getInstance(context)
        appDb.openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(TRUNCATE)")
            .use { /* truncate WAL so the .db file is complete on its own */ }

        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        require(dbFile.exists()) { "Database file not found" }

        val manifest = JSONObject().apply {
            put("app", BACKUP_APP_ID)
            put("version", BACKUP_VERSION)
            put("createdAt", System.currentTimeMillis())
            put("database", DATABASE_ENTRY)
        }

        ZipOutputStream(outputStream).use { zos ->
            zos.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zos.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry(DATABASE_ENTRY))
            dbFile.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }
        dbFile.length()
    }

    /**
     * Reads a backup archive, validates it, and stages it for restore.
     * The actual file swap is deferred to the next process start via
     * [BackupPendingRestore] so the running database connection is not disturbed.
     */
    suspend fun importBackup(inputStream: InputStream): RestoreInfo = withContext(Dispatchers.IO) {
        var manifest: JSONObject? = null
        var dbPresent = false

        ZipInputStream(inputStream.buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                when (entry.name) {
                    MANIFEST_ENTRY -> {
                        val raw = zis.readBytes().takeIf { it.size <= MAX_DB_BYTES }
                        manifest = raw?.let { JSONObject(it.toString(Charsets.UTF_8)) }
                    }
                    DATABASE_ENTRY -> {
                        val staged = File(context.cacheDir, "restore_pending.db")
                        staged.outputStream().use { out ->
                            var total = 0L
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val read = zis.read(buffer)
                                if (read < 0) break
                                total += read
                                if (total > MAX_DB_BYTES) error("Backup database is too large")
                                out.write(buffer, 0, read)
                            }
                        }
                        dbPresent = true
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val appId = manifest?.optString("app")
        require(appId == BACKUP_APP_ID) { "This file is not an ExpenseTracker backup" }
        require(dbPresent) { "Backup is missing its database" }

        val stagedFile = File(context.cacheDir, "restore_pending.db")
        require(stagedFile.exists() && stagedFile.length() > 0) { "Backup database is empty" }

        BackupPendingRestore.mark(context, stagedFile.absolutePath)
        RestoreInfo(
            bytes = stagedFile.length(),
            createdAt = manifest?.optLong("createdAt") ?: 0L,
        )
    }
}