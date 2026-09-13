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
 * Hilt module for database dependencies
 * Provides Room database and DAO instances
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideUsageDao(database: AppDatabase): UsageDao {
        return database.usageDao()
    }
}
