package com.expensetracker.app.di

import android.content.Context
import com.expensetracker.app.data.local.TransactionDao
import com.expensetracker.app.sms.SmsExportUseCase
import com.expensetracker.app.sms.SmsImportUseCase
import com.expensetracker.app.sms.SmsReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SmsModule {

    @Provides
    @Singleton
    fun provideSmsReader(@ApplicationContext context: Context): SmsReader {
        return SmsReader(context)
    }

    @Provides
    @Singleton
    fun provideSmsImportUseCase(
        smsReader: SmsReader,
        transactionDao: TransactionDao,
    ): SmsImportUseCase {
        return SmsImportUseCase(smsReader, transactionDao)
    }

    @Provides
    @Singleton
    fun provideSmsExportUseCase(
        @ApplicationContext context: Context,
        smsReader: SmsReader,
    ): SmsExportUseCase {
        return SmsExportUseCase(context, smsReader)
    }
}
