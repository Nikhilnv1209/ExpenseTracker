package com.expensetracker.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val title: String,
    val amount: Double,
    val isIncome: Boolean,
    val paymentDayOfMonth: Int,
    val daysBefore: Int,
    val customDate: Long? = null,
    val hour: Int = 9,
    val minute: Int = 0,
)

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY paymentDayOfMonth ASC")
    suspend fun getAll(): List<ReminderEntity>

    @Query("SELECT * FROM reminders ORDER BY paymentDayOfMonth ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE transactionId = :transactionId LIMIT 1")
    suspend fun findByTransactionId(transactionId: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderEntity): Long

    @Query("DELETE FROM reminders WHERE transactionId = :transactionId")
    suspend fun deleteByTransactionId(transactionId: Long)
}
