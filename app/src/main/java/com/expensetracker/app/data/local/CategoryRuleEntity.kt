package com.expensetracker.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "category_rules")
data class CategoryRuleEntity(
    @PrimaryKey
    val title: String,
    val category: String,
)

@Dao
interface CategoryRuleDao {

    @Query("SELECT * FROM category_rules ORDER BY title ASC")
    suspend fun getAll(): List<CategoryRuleEntity>

    @Query("SELECT * FROM category_rules ORDER BY title ASC")
    fun observeAll(): Flow<List<CategoryRuleEntity>>

    @Query("SELECT * FROM category_rules WHERE title = :title LIMIT 1")
    suspend fun findByTitle(title: String): CategoryRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: CategoryRuleEntity)

    @Query("DELETE FROM category_rules WHERE title = :title")
    suspend fun delete(title: String)
}
