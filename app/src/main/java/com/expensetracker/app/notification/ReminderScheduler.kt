package com.expensetracker.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.expensetracker.app.data.local.ReminderEntity
import java.time.LocalDate
import java.time.ZoneId

object ReminderScheduler {

    private const val TAG = "ReminderScheduler"

    fun schedule(context: Context, reminder: ReminderEntity) {
        val triggerDate = computeTriggerDate(reminder, LocalDate.now())
        val triggerTime = triggerDate
            .atTime(reminder.hour, reminder.minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerTime <= System.currentTimeMillis()) {
            Log.d(TAG, "Reminder ${reminder.id} trigger time is in the past, skipping")
            return
        }

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderAlarmReceiver.EXTRA_AMOUNT, reminder.amount)
            putExtra(ReminderAlarmReceiver.EXTRA_IS_INCOME, reminder.isIncome)
            putExtra(ReminderAlarmReceiver.EXTRA_PAYMENT_DAY, reminder.paymentDayOfMonth)
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                pendingIntent,
            )
            Log.d(TAG, "Scheduled reminder ${reminder.id} for ${triggerDate} at ${reminder.hour}:${reminder.minute}")
        } catch (e: SecurityException) {
            Log.e(TAG, "No permission to schedule exact alarm, falling back", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent,
            )
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled reminder $reminderId")
    }

    private fun computeTriggerDate(reminder: ReminderEntity, today: LocalDate): LocalDate {
        if (reminder.customDate != null) {
            return LocalDate.ofEpochDay(reminder.customDate)
        }

        val remindDay = reminder.paymentDayOfMonth - reminder.daysBefore

        val thisMonthDay = if (remindDay < 1) {
            val prevMonth = today.minusMonths(1)
            prevMonth.lengthOfMonth() + remindDay + 1
        } else {
            remindDay.coerceAtMost(today.lengthOfMonth())
        }

        val thisMonth = LocalDate.of(today.year, today.month, thisMonthDay.coerceIn(1, today.lengthOfMonth()))
        if (!thisMonth.isBefore(today)) return thisMonth

        val nextMonth = today.plusMonths(1)
        val nextRemindDay = reminder.paymentDayOfMonth - reminder.daysBefore
        val nextMonthDay = if (nextRemindDay < 1) {
            val prevOfNext = nextMonth.minusMonths(1)
            prevOfNext.lengthOfMonth() + nextRemindDay + 1
        } else {
            nextRemindDay.coerceAtMost(nextMonth.lengthOfMonth())
        }

        return LocalDate.of(nextMonth.year, nextMonth.month, nextMonthDay.coerceIn(1, nextMonth.lengthOfMonth()))
    }
}
