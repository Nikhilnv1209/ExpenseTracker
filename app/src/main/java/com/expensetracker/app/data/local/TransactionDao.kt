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

    @Query("""
        SELECT date, COALESCE(SUM(amount), 0.0) as total FROM transactions 
        WHERE type = 'INCOME' AND isExcluded = 0 AND date >= :startDate AND date <= :endDate
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getDailyIncomeTotals(startDate: Long, endDate: Long): List<DailyTotal>

    @Query("UPDATE transactions SET isExcluded = :excluded WHERE id = :id")
    suspend fun setExcluded(id: Long, excluded: Boolean)

    @Query("UPDATE transactions SET alias = :alias WHERE title = :originalTitle AND isExcluded = 0")
    suspend fun applyAliasToAll(originalTitle: String, alias: String?)

    @Query("UPDATE transactions SET alias = :alias WHERE id = :id")
    suspend fun setAlias(id: Long, alias: String?)

    @Query("UPDATE transactions SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String)

    @Query("UPDATE transactions SET category = :category WHERE title = :title AND categoryExempt = 0")
    suspend fun applyCategoryToAll(title: String, category: String)

    @Query("UPDATE transactions SET categoryExempt = :exempt WHERE id = :id")
    suspend fun setCategoryExempt(id: Long, exempt: Boolean)

    @Query("UPDATE transactions SET note = :note WHERE id = :id")
    suspend fun setNote(id: Long, note: String?)

    @Query("SELECT DISTINCT title FROM transactions ORDER BY title ASC")
    suspend fun getUniqueTitles(): List<String>

    @Query("SELECT DISTINCT bankName FROM transactions WHERE bankName IS NOT NULL ORDER BY bankName ASC")
    suspend fun getUniqueBanks(): List<String?>

    @Query("""
        SELECT * FROM transactions WHERE isExcluded = 0 
        AND date >= :startDate AND date <= :endDate
        AND (:type IS NULL OR type = :type)
        AND (:search IS NULL OR title LIKE '%' || :search || '%' OR alias LIKE '%' || :search || '%' OR bankName LIKE '%' || :search || '%')
        AND (:bank IS NULL OR bankName = :bank)
        ORDER BY date DESC, id DESC
    """)
    suspend fun getFiltered(
        startDate: Long,
        endDate: Long,
        type: String?,
        search: String?,
        bank: String?,
    ): List<TransactionEntity>

    @Query("UPDATE transactions SET isExcluded = :excluded WHERE title = :title")
    suspend fun setExcludedByTitle(title: String, excluded: Boolean)

    data class DailyTotal(
        val date: Long,
        val total: Double,
    )
}
