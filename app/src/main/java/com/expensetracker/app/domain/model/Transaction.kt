package com.expensetracker.app.domain.model

import java.time.LocalDate

enum class TransactionType {
    INCOME,
    EXPENSE
}

enum class Category(val displayName: String) {
    FOOD("Food"),
    TRANSPORT("Transport"),
    SHOPPING("Shopping"),
    ENTERTAINMENT("Entertainment"),
    HEALTH("Health"),
    BILLS("Bills"),
    GROCERY("Grocery"),
    SALARY("Salary"),
    CASH("Cash"),
    TRANSFER("Transfer"),
    OTHER("Other");

    companion object {
        fun fromDisplayName(name: String): Category {
            return entries.find { it.displayName.equals(name, ignoreCase = true) } ?: OTHER
        }
    }
}

data class Transaction(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: Category,
    val isIncome: Boolean,
    val date: LocalDate,
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
