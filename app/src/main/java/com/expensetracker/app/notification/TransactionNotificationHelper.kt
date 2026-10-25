package com.expensetracker.app.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.expensetracker.app.MainActivity
import com.expensetracker.app.R
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object TransactionNotificationHelper {

    private const val CHANNEL_GROUP_ID = "expense_tracker_alerts"
    private const val CHANNEL_ID_NEW = "new_transaction_channel"
    private const val CHANNEL_ID_SUMMARY = "daily_summary_channel"
    private const val CHANNEL_ID_REMINDER = "reminder_channel"
    private const val NOTIFICATION_ID_SUMMARY = 1002
    private const val NOTIFICATION_ID_REMINDER = 1003
    private const val NOTIFICATION_ID_NEW_BASE = 100_000
    private const val PREFS_NAME = "notification_prefs"
    private const val KEY_NOTIFIED_IDS = "notified_transaction_ids"
    private const val MAX_TRACKED_IDS = 100

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannelGroup(
            NotificationChannelGroup(CHANNEL_GROUP_ID, "ExpenseTracker"),
        )

        val newChannel = NotificationChannel(
            CHANNEL_ID_NEW,
            "Transaction alerts",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Shown when a new bank transaction is imported from SMS"
            group = CHANNEL_GROUP_ID
            setShowBadge(true)
        }

        val summaryChannel = NotificationChannel(
            CHANNEL_ID_SUMMARY,
            "Daily summary",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "End-of-day spending comparison"
            group = CHANNEL_GROUP_ID
            setShowBadge(false)
        }

        val reminderChannel = NotificationChannel(
            CHANNEL_ID_REMINDER,
            "Payment reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders for upcoming bills and subscriptions"
            group = CHANNEL_GROUP_ID
            setShowBadge(true)
        }

        manager.createNotificationChannels(listOf(newChannel, summaryChannel, reminderChannel))
    }

    fun showNewTransactionNotification(
        context: Context,
        transactionId: Long,
        title: String,
        amount: Double,
        isIncome: Boolean,
        bankName: String? = null,
        accountLast4: String? = null,
    ) {
        if (transactionId < 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (isAlreadyNotified(prefs, transactionId)) return

        val money = formatMoney(amount)
        val verb = if (isIncome) "received" else "paid"
        val account = listOfNotNull(
            bankName?.takeIf { it.isNotBlank() },
            accountLast4?.takeIf { it.isNotBlank() }?.let { "··$it" },
        ).joinToString(" ")

        val notificationTitle = if (title.isNotBlank()) "$money $verb · $title" else "$money $verb"
        val contentText = account.ifBlank { "Tap to view details" }
        val bigText = contentText

        val accent = context.getColor(R.color.notification_accent)
        val openTransaction = openTransactionIntent(context, transactionId)

        val builder = baseBuilder(context, CHANNEL_ID_NEW, accent)
            .setContentTitle(notificationTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openTransaction)
            .addAction(R.drawable.ic_notification, "View", openTransaction)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                publicVersion(context, CHANNEL_ID_NEW, "New transaction", accent, openTransaction),
            )

        notificationManager(context).notify(
            NOTIFICATION_ID_NEW_BASE + transactionId.toInt(),
            builder.build(),
        )
        markNotified(prefs, transactionId)
    }

    fun showDailySummaryNotification(
        context: Context,
        todayExpense: Double,
        todayIncome: Double,
        yesterdayExpense: Double,
        yesterdayIncome: Double,
    ) {
        val expenseDiff = todayExpense - yesterdayExpense
        val comparison = when {
            yesterdayExpense == 0.0 && todayExpense > 0.0 -> "No spending yesterday"
            expenseDiff > 0.0 -> "${formatMoney(expenseDiff)} more than yesterday"
            expenseDiff < 0.0 -> "${formatMoney(-expenseDiff)} less than yesterday"
            else -> "Same as yesterday"
        }

        val notificationTitle = when {
            todayExpense > 0.0 -> "${formatMoney(todayExpense)} spent today"
            todayIncome > 0.0 -> "${formatMoney(todayIncome)} received today"
            else -> "No transactions today"
        }
        val contentText = buildString {
            if (todayIncome > 0.0 && todayExpense > 0.0) {
                append(formatMoney(todayIncome)).append(" received · ")
            }
            append(comparison)
        }
        val bigText = buildString {
            if (todayExpense > 0.0 && todayIncome > 0.0) {
                append(formatMoney(todayIncome)).append(" received today")
                append('\n')
                append("Net: ").append(signedMoney(todayIncome - todayExpense))
                append('\n')
            }
            append(comparison)
        }

        val accent = context.getColor(R.color.notification_accent)
        val openApp = openAppIntent(context)
        val builder = baseBuilder(context, CHANNEL_ID_SUMMARY, accent)
            .setContentTitle(notificationTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openApp)
            .addAction(R.drawable.ic_notification, "Open app", openApp)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                publicVersion(context, CHANNEL_ID_SUMMARY, "Daily summary", accent, openApp),
            )

        notificationManager(context).notify(NOTIFICATION_ID_SUMMARY, builder.build())
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
        val today = LocalDate.now()
        val paymentDate = LocalDate.ofEpochDay(paymentDateEpochDay)
        val dayDiff = ChronoUnit.DAYS.between(today, paymentDate)

        val dateText = when {
            dayDiff <= 0L -> if (isIncome) "Expected today" else "Due today"
            dayDiff == 1L -> if (isIncome) "Expected tomorrow" else "Due tomorrow"
            dayDiff <= 7L -> if (isIncome) "Expected in $dayDiff days" else "Due in $dayDiff days"
            else -> "${if (isIncome) "Expected" else "Due"} on ${paymentDate.dayOfMonth} ${
                paymentDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }"
        }

        val money = formatMoney(amount)
        val notificationTitle = title.ifBlank { if (isIncome) "Expected payment" else "Payment reminder" }
        val contentText = "$money · $dateText"
        val bigText = contentText

        val accent = context.getColor(R.color.notification_reminder)
        val openTarget = if (transactionId >= 0) {
            openTransactionIntent(context, transactionId)
        } else {
            openAppIntent(context)
        }

        val builder = baseBuilder(context, CHANNEL_ID_REMINDER, accent)
            .setContentTitle(notificationTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openTarget)
            .addAction(R.drawable.ic_notification, "View", openTarget)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                publicVersion(
                    context,
                    CHANNEL_ID_REMINDER,
                    if (isIncome) "Payment expected" else "Payment reminder",
                    accent,
                    openTarget,
                ),
            )

        notificationManager(context).notify(notificationId, builder.build())
    }

    private fun baseBuilder(
        context: Context,
        channelId: String,
        accent: Int,
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
    }

    private fun publicVersion(
        context: Context,
        channelId: String,
        title: String,
        accent: Int,
        contentIntent: PendingIntent,
    ): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accent)
            .setContentTitle(title)
            .setContentText("Tap to view details")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun openTransactionIntent(context: Context, transactionId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
        }
        return PendingIntent.getActivity(
            context,
            transactionId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationManager(context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun formatMoney(amount: Double): String {
        return "₹" + String.format(Locale.US, "%,.2f", amount)
    }

    private fun signedMoney(amount: Double): String {
        return if (amount >= 0) "+${formatMoney(amount)}" else "-${formatMoney(-amount)}"
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

    const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
}
