package com.expensetracker.app.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.expensetracker.app.ExpenseTrackerApplication
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Duration
import java.util.concurrent.TimeUnit

class ReminderCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ExpenseTrackerApplication
        val reminders = app.reminderDao.getAll()
        if (reminders.isEmpty()) return Result.success()

        val today = LocalDate.now()
        val todayDay = today.dayOfMonth

        reminders.forEach { reminder ->
            val shouldFire = if (reminder.customDate != null) {
                today.toEpochDay() == reminder.customDate
            } else {
                val remindDay = reminder.paymentDayOfMonth - reminder.daysBefore
                val actualRemindDay = if (remindDay < 1) {
                    val prevMonth = today.minusMonths(1)
                    prevMonth.lengthOfMonth() + remindDay + 1
                } else {
                    remindDay.coerceAtMost(today.lengthOfMonth())
                }
                actualRemindDay == today.dayOfMonth
            }

            if (shouldFire) {
                TransactionNotificationHelper.showReminderNotification(
                    context = applicationContext,
                    title = reminder.title,
                    amount = reminder.amount,
                    isIncome = reminder.isIncome,
                    paymentDay = reminder.paymentDayOfMonth,
                )
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "reminder_check_worker"
        private const val HOUR = 9

        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            val target = now.toLocalDate().atTime(HOUR, 0)
            val initialDelay = if (now.isAfter(target)) {
                Duration.between(now, target.plusDays(1))
            } else {
                Duration.between(now, target)
            }.toMillis()

            val request = PeriodicWorkRequestBuilder<ReminderCheckWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay.coerceAtLeast(0), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
