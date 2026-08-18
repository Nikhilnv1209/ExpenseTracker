package com.expensetracker.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, AliasEntity::class, IgnoredSenderEntity::class, CategoryRuleEntity::class, ReminderEntity::class, ChatSessionEntity::class, ChatMessageEntity::class],
    version = 9,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun aliasDao(): AliasDao
    abstract fun ignoredSenderDao(): IgnoredSenderDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun reminderDao(): ReminderDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        private const val DATABASE_NAME = "expense_tracker.db"

        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN isExcluded INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN alias TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS aliases (originalTitle TEXT NOT NULL PRIMARY KEY, alias TEXT NOT NULL)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS ignored_senders (sender TEXT NOT NULL PRIMARY KEY)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN categoryExempt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS category_rules (title TEXT NOT NULL PRIMARY KEY, category TEXT NOT NULL)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN noteIsManual INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS reminders (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, transactionId INTEGER NOT NULL, title TEXT NOT NULL, amount REAL NOT NULL, isIncome INTEGER NOT NULL, paymentDayOfMonth INTEGER NOT NULL, daysBefore INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN customDate INTEGER")
                db.execSQL("ALTER TABLE reminders ADD COLUMN hour INTEGER NOT NULL DEFAULT 9")
                db.execSQL("ALTER TABLE reminders ADD COLUMN minute INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_sessions (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, preview TEXT NOT NULL DEFAULT '', messageCount INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE IF NOT EXISTS chat_messages (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, sessionId INTEGER NOT NULL, role TEXT NOT NULL, content TEXT NOT NULL, reasoning TEXT, timestamp INTEGER NOT NULL)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
