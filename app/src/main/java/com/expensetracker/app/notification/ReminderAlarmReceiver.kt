package com.expensetracker.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        val isIncome = intent.getBooleanExtra(EXTRA_IS_INCOME, false)
        val paymentDateEpochDay = intent.getLongExtra(EXTRA_PAYMENT_DATE, 0)
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, 0)
        val transactionId = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1)

        TransactionNotificationHelper.showReminderNotification(
            context = context,
            title = title,
            amount = amount,
            isIncome = isIncome,
            paymentDateEpochDay = paymentDateEpochDay,
            transactionId = transactionId,
            notificationId = NOTIFICATION_ID_BASE + reminderId.toInt(),
        )
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_IS_INCOME = "extra_is_income"
        const val EXTRA_PAYMENT_DATE = "extra_payment_date"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
        private const val NOTIFICATION_ID_BASE = 2000
    }
}
