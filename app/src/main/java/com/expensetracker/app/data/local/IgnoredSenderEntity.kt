package com.expensetracker.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "ignored_senders")
data class IgnoredSenderEntity(
    @PrimaryKey
    val sender: String,
)

@Dao
interface IgnoredSenderDao {

    @Query("SELECT * FROM ignored_senders ORDER BY sender ASC")
    suspend fun getAll(): List<IgnoredSenderEntity>

    @Query("SELECT * FROM ignored_senders ORDER BY sender ASC")
    fun observeAll(): Flow<List<IgnoredSenderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(sender: IgnoredSenderEntity)

    @Query("DELETE FROM ignored_senders WHERE sender = :sender")
    suspend fun delete(sender: String)
}
