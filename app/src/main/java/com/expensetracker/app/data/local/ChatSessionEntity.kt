package com.expensetracker.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val preview: String = "",
    val messageCount: Int = 0,
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val role: String,
    val content: String,
    val reasoning: String? = null,
    val timestamp: Long,
)

@Dao
interface ChatSessionDao {

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    suspend fun getAll(): List<ChatSessionEntity>

    @Query("SELECT * FROM chat_sessions WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ChatSessionEntity?

    @Query("SELECT * FROM chat_sessions WHERE title LIKE '%' || :search || '%' OR preview LIKE '%' || :search || '%' ORDER BY updatedAt DESC")
    fun search(search: String): Flow<List<ChatSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ChatSessionEntity): Long

    @Query("UPDATE chat_sessions SET preview = :preview, updatedAt = :updatedAt, messageCount = messageCount + 1 WHERE id = :sessionId")
    suspend fun updatePreview(sessionId: Long, preview: String, updatedAt: Long)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeBySession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySession(sessionId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)
}
