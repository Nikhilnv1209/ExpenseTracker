package com.expensetracker.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

enum class AccountKind {
    BANK,
    CARD,
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val bankName: String,
    val kind: String,
    val last4: String,
)

/** A bank/last4 pair seen in imported transactions that is not linked to an account yet. */
data class DetectedAccount(
    val bankName: String,
    val last4: String,
    val transactionCount: Int,
)

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<AccountEntity>

    @Query("SELECT * FROM accounts ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE bankName = :bankName COLLATE NOCASE AND last4 = :last4 LIMIT 1")
    suspend fun findByBankAndLast4(bankName: String, last4: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
