package com.sipun.netspeedindicator.core.di

import android.content.Context
import androidx.room.Room
import com.sipun.netspeedindicator.data.local.AppDatabase
import com.sipun.netspeedindicator.data.local.dao.UsageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        // Never use destructive migration for usage history. A future schema
        // change must ship with an explicit Room Migration instead.
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideUsageDao(database: AppDatabase): UsageDao = database.usageDao()
}
