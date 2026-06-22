package com.expensetracker.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.domain.model.TransactionType
import java.time.LocalDate

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val type: String,
    val date: Long,
    val note: String? = null,
    val bankName: String? = null,
    val accountLast4: String? = null,
    val rawSms: String? = null,
    val smsDate: Long? = null,
    val isExcluded: Boolean = false,
    val alias: String? = null,
    val categoryExempt: Boolean = false,
    val noteIsManual: Boolean = false,
)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    title = title,
    amount = amount,
    category = Category.fromDisplayName(category),
    isIncome = type == TransactionType.INCOME.name,
    date = LocalDate.ofEpochDay(date),
    note = note,
    bankName = bankName,
    accountLast4 = accountLast4,
    rawSms = rawSms,
    smsDate = smsDate,
    isExcluded = isExcluded,
    alias = alias,
    categoryExempt = categoryExempt,
    noteIsManual = noteIsManual,
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    title = title,
    amount = amount,
    category = category.displayName,
    type = if (isIncome) TransactionType.INCOME.name else TransactionType.EXPENSE.name,
    date = date.toEpochDay(),
    note = note,
    bankName = bankName,
    accountLast4 = accountLast4,
    rawSms = rawSms,
    smsDate = smsDate,
    isExcluded = isExcluded,
    alias = alias,
    categoryExempt = categoryExempt,
    noteIsManual = noteIsManual,
)
