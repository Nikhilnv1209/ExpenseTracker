package com.expensetracker.app.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.expensetracker.app.ExpenseTrackerApplication
import com.expensetracker.app.data.local.TransactionDao
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

class DailySummaryWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ExpenseTrackerApplication
        val dao = app.transactionDao

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val todayExpense = dao.getExpenseTotal(today.toEpochDay(), today.toEpochDay())
        val todayIncome = dao.getIncomeTotal(today.toEpochDay(), today.toEpochDay())
        val yesterdayExpense = dao.getExpenseTotal(yesterday.toEpochDay(), yesterday.toEpochDay())
        val yesterdayIncome = dao.getIncomeTotal(yesterday.toEpochDay(), yesterday.toEpochDay())

        val noData = todayExpense == 0.0 && todayIncome == 0.0 && yesterdayExpense == 0.0 && yesterdayIncome == 0.0
        if (noData) return Result.success()

        TransactionNotificationHelper.showDailySummaryNotification(
            context = applicationContext,
            todayExpense = todayExpense,
            todayIncome = todayIncome,
            yesterdayExpense = yesterdayExpense,
            yesterdayIncome = yesterdayIncome,
        )
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_summary_worker"
        private const val HOUR = 20

        fun schedule(context: Context) {
            val now = java.time.LocalDateTime.now()
            val target = now.toLocalDate().atTime(HOUR, 0)
            val initialDelay = if (now.isAfter(target)) {
                java.time.Duration.between(now, target.plusDays(1))
            } else {
                java.time.Duration.between(now, target)
            }.toMillis()

            val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(24, TimeUnit.HOURS)
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
