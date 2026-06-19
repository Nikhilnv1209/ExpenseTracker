package com.expensetracker.app

import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.util.Log
import com.expensetracker.app.data.local.AppDatabase
import com.expensetracker.app.notification.DailySummaryWorker
import com.expensetracker.app.notification.TransactionNotificationHelper
import com.expensetracker.app.sms.SmsReceiver
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ExpenseTrackerApplication : Application() {
    val transactionDao by lazy { AppDatabase.getInstance(this).transactionDao() }
    val aliasDao by lazy { AppDatabase.getInstance(this).aliasDao() }
    val ignoredSenderDao by lazy { AppDatabase.getInstance(this).ignoredSenderDao() }
    val categoryRuleDao by lazy { AppDatabase.getInstance(this).categoryRuleDao() }

    private var smsReceiver: SmsReceiver? = null

    override fun onCreate() {
        super.onCreate()
        TransactionNotificationHelper.createChannels(this)
        DailySummaryWorker.schedule(this)
    }

    fun registerSmsReceiverIfNeeded() {
        if (smsReceiver != null) return
        val hasPermission = checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Log.w("ExpenseTrackerApp", "SMS permissions not granted, skipping receiver registration")
            return
        }
        val receiver = SmsReceiver()
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, android.Manifest.permission.RECEIVE_SMS, null, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
        smsReceiver = receiver
        Log.d("ExpenseTrackerApp", "SMS receiver registered")
    }
}
