package com.expensetracker.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "aliases")
data class AliasEntity(
    @PrimaryKey
    val originalTitle: String,
    val alias: String,
)

@Dao
interface AliasDao {

    @Query("SELECT * FROM aliases ORDER BY originalTitle ASC")
    suspend fun getAll(): List<AliasEntity>

    @Query("SELECT * FROM aliases ORDER BY originalTitle ASC")
    fun observeAll(): Flow<List<AliasEntity>>

    @Query("SELECT * FROM aliases WHERE originalTitle = :originalTitle LIMIT 1")
    suspend fun findByOriginalTitle(originalTitle: String): AliasEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alias: AliasEntity)

    @Query("DELETE FROM aliases WHERE originalTitle = :originalTitle")
    suspend fun delete(originalTitle: String)
}
