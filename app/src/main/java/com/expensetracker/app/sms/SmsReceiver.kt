package com.expensetracker.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.expensetracker.app.ExpenseTrackerApplication
import com.expensetracker.app.data.local.IgnoredSenderEntity
import com.expensetracker.app.data.local.toEntity
import com.expensetracker.app.domain.model.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val app = context.applicationContext as ExpenseTrackerApplication
        val smsReader = SmsReader(context)

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val address = sms.displayOriginatingAddress ?: continue
            val body = sms.messageBody ?: continue
            val date = System.currentTimeMillis()

            if (!smsReader.isBankSmsPublic(address, body)) continue

            Log.d(TAG, "Bank SMS received from $address")

            val parsed = BankSmsParser.parse(SmsMessage(address, body, date)) ?: continue

            CoroutineScope(Dispatchers.IO).launch {
                val transactionDao = app.transactionDao
                val aliasDao = app.aliasDao
                val ignoredSenderDao = app.ignoredSenderDao

                val ignoredSenders = ignoredSenderDao.getAll().map { it.sender.lowercase() }.toSet()
                if (parsed.description.lowercase() in ignoredSenders) {
                    Log.d(TAG, "Skipped ignored sender: ${parsed.description}")
                    return@launch
                }

                val existing = parsed.smsDate?.let { transactionDao.findBySmsDate(it) }
                if (existing != null) {
                    Log.d(TAG, "Transaction already exists for smsDate=${parsed.smsDate}")
                    return@launch
                }

                val resolvedAlias = aliasDao.findByOriginalTitle(parsed.description)?.alias
                val transaction = com.expensetracker.app.domain.model.Transaction(
                    title = parsed.description,
                    amount = parsed.amount,
                    category = parsed.category,
                    isIncome = parsed.type == TransactionType.CREDIT,
                    date = parsed.date,
                    note = buildString {
                        parsed.bankName?.let { append(it); append(" · ") }
                        parsed.accountLast4?.let { append("A/c ..$it"); append(" · ") }
                        parsed.time?.let { append(it) }
                    }.trimEnd(' ', '·').ifBlank { "Auto-imported from SMS" },
                    bankName = parsed.bankName,
                    accountLast4 = parsed.accountLast4,
                    rawSms = parsed.rawSms,
                    smsDate = parsed.smsDate,
                    alias = resolvedAlias,
                )

                transactionDao.insert(transaction.toEntity())
                Log.d(TAG, "Auto-imported: ${parsed.description} ₹${parsed.amount}")
            }
        }
    }
}
