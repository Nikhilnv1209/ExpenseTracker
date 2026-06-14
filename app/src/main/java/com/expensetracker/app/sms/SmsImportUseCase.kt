package com.expensetracker.app.sms

import android.util.Log
import com.expensetracker.app.data.local.TransactionDao
import com.expensetracker.app.data.local.toEntity
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Transaction
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsImportUseCase @Inject constructor(
    private val smsReader: SmsReader,
    private val transactionDao: TransactionDao,
) {

    suspend fun importTransactions(): SmsImportResult {
        Log.d("SmsImportUseCase", "Starting SMS import...")
        val messages = smsReader.readAllBankSms(limit = 500)
        Log.d("SmsImportUseCase", "Found ${messages.size} bank SMS messages")

        val parsed = messages.mapNotNull { BankSmsParser.parse(it) }
        Log.d("SmsImportUseCase", "Parsed ${parsed.size} transactions")

        var imported = 0
        var skipped = 0

        parsed.forEach { parsedTx ->
            val existing = parsedTx.smsDate?.let { transactionDao.findBySmsDate(it) }
            if (existing != null) {
                val updated = existing.copy(
                    title = parsedTx.description,
                    amount = parsedTx.amount,
                    category = parsedTx.category.displayName,
                    type = if (parsedTx.type == TransactionType.CREDIT) "INCOME" else "EXPENSE",
                    bankName = parsedTx.bankName,
                    accountLast4 = parsedTx.accountLast4,
                    rawSms = parsedTx.rawSms,
                )
                transactionDao.update(updated)
                skipped++
                return@forEach
            }

            val transaction = Transaction(
                title = parsedTx.description,
                amount = parsedTx.amount,
                category = parsedTx.category,
                isIncome = parsedTx.type == TransactionType.CREDIT,
                date = parsedTx.date,
                note = buildNote(parsedTx),
                bankName = parsedTx.bankName,
                accountLast4 = parsedTx.accountLast4,
                rawSms = parsedTx.rawSms,
                smsDate = parsedTx.smsDate,
            )

            transactionDao.insert(transaction.toEntity())
            imported++
        }

        return SmsImportResult(
            scanned = messages.size,
            parsed = parsed.size,
            imported = imported,
            skipped = skipped,
        )
    }

    private fun buildNote(parsed: ParsedSmsTransaction): String {
        val parts = mutableListOf<String>()
        parsed.bankName?.let { parts.add(it) }
        parsed.accountLast4?.let { parts.add("A/c ..$it") }
        parsed.time?.let { parts.add(it.toString()) }
        return if (parts.isEmpty()) "Imported from SMS" else parts.joinToString(" · ")
    }
}

data class SmsImportResult(
    val scanned: Int,
    val parsed: Int,
    val imported: Int,
    val skipped: Int,
)
