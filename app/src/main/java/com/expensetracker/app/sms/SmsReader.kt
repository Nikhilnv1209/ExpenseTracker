package com.expensetracker.app.sms

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class SmsMessage(
    val address: String,
    val body: String,
    val date: Long,
)

@Singleton
class SmsReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val contentResolver: ContentResolver = context.contentResolver

    private val knownBankSenders = listOf(
        "SBIIN", "HDFCBK", "ICICIB", "AXIBNK", "KOTAK", "PNBIN", "BAROB", "CNRBN",
        "YESBNK", "YESBANK", "INDBNK", "IDFBNK", "AUSFB", "FEDBNK", "UNIONB",
        "INDIANB", "IOB", "UCOB", "BANDHN", "RBLBNK", "CITIBK", "HSBCBK", "SCBUK",
        "AMEXIN", "UPI", "BOM", "INDUS", "KMBNK", "SBI", "HDFC", "ICICI", "AXIS",
        "BOB", "PNB", "IOB", "IDBI", "UCO", "CBI", "OBC", "UNION", "BANK",
    )

    fun readAllBankSms(limit: Int = 500): List<SmsMessage> {
        val smsList = mutableListOf<SmsMessage>()
        val cursor: Cursor? = try {
            contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                ),
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT $limit",
            )
        } catch (e: SecurityException) {
            return emptyList()
        }

        cursor?.use { c ->
            val addressIndex = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = c.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (c.moveToNext()) {
                val address = c.getString(addressIndex) ?: continue
                val body = c.getString(bodyIndex) ?: continue
                val date = c.getLong(dateIndex)

                if (isBankSms(address, body)) {
                    smsList.add(
                        SmsMessage(
                            address = address,
                            body = body,
                            date = date,
                        ),
                    )
                }
            }
        }

        return smsList
    }

    private fun isBankSms(address: String, body: String): Boolean {
        val normalizedAddress = address.uppercase(Locale.ENGLISH)
            .replace("+", "")
            .replace("-", "")
            .replace(" ", "")

        // Indian transactional SMS format: VK-XXXXX, VM-XXXXX, AD-XXXXX, etc.
        if (isTransactionalSender(normalizedAddress)) {
            val senderBody = body.uppercase(Locale.ENGLISH)
            return knownBankSenders.any { senderBody.contains(it) || normalizedAddress.contains(it) } ||
                isTransactionBody(senderBody)
        }

        // Direct bank short codes or long numbers
        return knownBankSenders.any { normalizedAddress.contains(it) }
    }

    private fun isTransactionalSender(address: String): Boolean {
        // Indian telecom format for transactional messages: 2 letters, hyphen, 5-6 chars
        return address.matches(Regex("^[A-Z]{2}[A-Z]?-[A-Z0-9]{4,8}$")) ||
            address.matches(Regex("^[A-Z]{4,10}$"))
    }

    private fun isTransactionBody(body: String): Boolean {
        val debitWords = listOf("DEBITED", "SPENT", "PAID", "WITHDRAWN", "PURCHASE", "DEDUCTED")
        val creditWords = listOf("CREDITED", "DEPOSITED", "RECEIVED", "ADDED")
        return debitWords.any { body.contains(it) } || creditWords.any { body.contains(it) }
    }
}
