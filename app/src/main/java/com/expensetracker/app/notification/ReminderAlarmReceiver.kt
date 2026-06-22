package com.expensetracker.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        val isIncome = intent.getBooleanExtra(EXTRA_IS_INCOME, false)
        val paymentDay = intent.getIntExtra(EXTRA_PAYMENT_DAY, 1)
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, 0)

        TransactionNotificationHelper.showReminderNotification(
            context = context,
            title = title,
            amount = amount,
            isIncome = isIncome,
            paymentDay = paymentDay,
            notificationId = NOTIFICATION_ID_BASE + reminderId.toInt(),
        )
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_IS_INCOME = "extra_is_income"
        const val EXTRA_PAYMENT_DAY = "extra_payment_day"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        private const val NOTIFICATION_ID_BASE = 2000
    }
}
