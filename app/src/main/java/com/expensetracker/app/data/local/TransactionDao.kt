package com.expensetracker.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE isExcluded = 0 ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isExcluded = 0 ORDER BY date DESC, id DESC")
    suspend fun getAllSorted(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isExcluded = 0 AND date >= :startDate AND date <= :endDate ORDER BY date DESC, id DESC")
    suspend fun getByDateRange(startDate: Long, endDate: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isExcluded = 0 ORDER BY date DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isExcluded = 1 ORDER BY date DESC, id DESC")
    suspend fun getExcluded(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE smsDate = :smsDate LIMIT 1")
    suspend fun findBySmsDate(smsDate: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE type = 'INCOME' AND isExcluded = 0 AND date >= :startDate AND date <= :endDate
    """)
    suspend fun getIncomeTotal(startDate: Long, endDate: Long): Double

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE type = 'EXPENSE' AND isExcluded = 0 AND date >= :startDate AND date <= :endDate
    """)
    suspend fun getExpenseTotal(startDate: Long, endDate: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'INCOME' AND isExcluded = 0")
    suspend fun getAllIncomeTotal(): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'EXPENSE' AND isExcluded = 0")
    suspend fun getAllExpenseTotal(): Double

    @Query("""
        SELECT date, COALESCE(SUM(amount), 0.0) as total FROM transactions 
        WHERE type = 'EXPENSE' AND isExcluded = 0 AND date >= :startDate AND date <= :endDate
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getDailyExpenseTotals(startDate: Long, endDate: Long): List<DailyTotal>

    @Query("UPDATE transactions SET isExcluded = :excluded WHERE id = :id")
    suspend fun setExcluded(id: Long, excluded: Boolean)

    @Query("UPDATE transactions SET alias = :alias WHERE title = :originalTitle AND isExcluded = 0")
    suspend fun applyAliasToAll(originalTitle: String, alias: String?)

    @Query("UPDATE transactions SET alias = :alias WHERE id = :id")
    suspend fun setAlias(id: Long, alias: String?)

    data class DailyTotal(
        val date: Long,
        val total: Double,
    )
}
