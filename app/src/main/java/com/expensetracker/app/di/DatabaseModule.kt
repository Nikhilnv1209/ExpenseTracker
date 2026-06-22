package com.expensetracker.app.di

import android.content.Context
import com.expensetracker.app.data.local.AliasDao
import com.expensetracker.app.data.local.AppDatabase
import com.expensetracker.app.data.local.CategoryRuleDao
import com.expensetracker.app.data.local.IgnoredSenderDao
import com.expensetracker.app.data.local.ReminderDao
import com.expensetracker.app.data.local.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideAliasDao(database: AppDatabase): AliasDao {
        return database.aliasDao()
    }

    @Provides
    @Singleton
    fun provideIgnoredSenderDao(database: AppDatabase): IgnoredSenderDao {
        return database.ignoredSenderDao()
    }

    @Provides
    @Singleton
    fun provideCategoryRuleDao(database: AppDatabase): CategoryRuleDao {
        return database.categoryRuleDao()
    }

    @Provides
    @Singleton
    fun provideReminderDao(database: AppDatabase): ReminderDao {
        return database.reminderDao()
    }
}
