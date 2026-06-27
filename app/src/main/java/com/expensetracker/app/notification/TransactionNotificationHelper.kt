package com.expensetracker.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import com.expensetracker.app.R

object TransactionNotificationHelper {

    private const val CHANNEL_ID_NEW = "new_transaction_channel"
    private const val CHANNEL_ID_SUMMARY = "daily_summary_channel"
    private const val CHANNEL_ID_REMINDER = "reminder_channel"
    private const val NOTIFICATION_ID_NEW = 1001
    private const val NOTIFICATION_ID_SUMMARY = 1002
    private const val NOTIFICATION_ID_REMINDER = 1003
    private const val PREFS_NAME = "notification_prefs"
    private const val KEY_NOTIFIED_IDS = "notified_transaction_ids"
    private const val MAX_TRACKED_IDS = 100

    fun createChannels(context: Context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)

        val newChannel = NotificationChannel(
            CHANNEL_ID_NEW,
            "New Transactions",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Notifications for newly imported bank transactions" }

        val summaryChannel = NotificationChannel(
            CHANNEL_ID_SUMMARY,
            "Daily Summary",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "End-of-day spending summary" }

        val reminderChannel = NotificationChannel(
            CHANNEL_ID_REMINDER,
            "Payment Reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Reminders for upcoming subscription payments" }

        manager?.createNotificationChannels(listOf(newChannel, summaryChannel, reminderChannel))
    }

    fun showNewTransactionNotification(
        context: Context,
        transactionId: Long,
        title: String,
        amount: Double,
        isIncome: Boolean,
    ) {
        if (transactionId < 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (isAlreadyNotified(prefs, transactionId)) return

        val label = if (isIncome) "Received" else "Paid"
        val sign = if (isIncome) "+" else "-"
        val content = "$label ₹${String.format("%.2f", amount)} · $title"

        val openIntent = android.content.Intent(context, com.expensetracker.app.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            transactionId.toInt(),
            openIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_NEW)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle("New transaction")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(transactionId.toInt(), builder.build())
        markNotified(prefs, transactionId)
    }

    fun showDailySummaryNotification(
        context: Context,
        todayExpense: Double,
        todayIncome: Double,
        yesterdayExpense: Double,
        yesterdayIncome: Double,
    ) {
        val net = todayIncome - todayExpense
        val netText = if (net >= 0) "+₹${String.format("%.2f", net)} net" else "-₹${String.format("%.2f", kotlin.math.abs(net))} net"
        val expenseDiff = todayExpense - yesterdayExpense
        val expenseCompare = when {
            expenseDiff > 0 -> "₹${String.format("%.2f", expenseDiff)} more than yesterday"
            expenseDiff < 0 -> "₹${String.format("%.2f", kotlin.math.abs(expenseDiff))} less than yesterday"
            else -> "same as yesterday"
        }

        val title = "Daily summary · ${String.format("%.2f", todayIncome)} in, ${String.format("%.2f", todayExpense)} out"
        val content = "$netText — you spent $expenseCompare"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SUMMARY)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_SUMMARY, builder.build())
    }

    private fun isAlreadyNotified(prefs: SharedPreferences, transactionId: Long): Boolean {
        return prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet())?.contains(transactionId.toString()) == true
    }

    private fun markNotified(prefs: SharedPreferences, transactionId: Long) {
        val current = prefs.getStringSet(KEY_NOTIFIED_IDS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        current.add(transactionId.toString())
        if (current.size > MAX_TRACKED_IDS) {
            val sorted = current.mapNotNull { it.toLongOrNull() }.sortedDescending().take(MAX_TRACKED_IDS)
            current.clear()
            current.addAll(sorted.map { it.toString() })
        }
        prefs.edit().putStringSet(KEY_NOTIFIED_IDS, current).apply()
    }

    fun showReminderNotification(
        context: Context,
        title: String,
        amount: Double,
        isIncome: Boolean,
        paymentDateEpochDay: Long,
        transactionId: Long,
        notificationId: Int = NOTIFICATION_ID_REMINDER,
    ) {
        val today = java.time.LocalDate.now()
        val paymentDate = java.time.LocalDate.ofEpochDay(paymentDateEpochDay)
        val dayDiff = java.time.temporal.ChronoUnit.DAYS.between(today, paymentDate)

        val verb = if (isIncome) "Expected" else "Due"
        val dateText = when {
            dayDiff <= 0L -> "$verb today"
            dayDiff == 1L -> "$verb tomorrow"
            dayDiff <= 7L -> "$verb in $dayDiff days"
            else -> "$verb on ${paymentDate.dayOfMonth} ${paymentDate.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())}"
        }

        val content = "$title · ₹${String.format("%.0f", amount)} · $dateText"

        val openIntent = android.content.Intent(context, com.expensetracker.app.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            transactionId.toInt(),
            openIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_REMINDER)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(if (isIncome) "Payment expected" else "Payment reminder")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
}
