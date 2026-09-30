package com.sipun.netspeedindicator.core.di

import com.sipun.netspeedindicator.data.repository.AppDataUsageRepositoryImpl
import com.sipun.netspeedindicator.data.repository.SpeedRepositoryImpl
import com.sipun.netspeedindicator.data.repository.UsageRepositoryImpl
import com.sipun.netspeedindicator.domain.repository.AppDataUsageRepository
import com.sipun.netspeedindicator.domain.repository.SpeedRepository
import com.sipun.netspeedindicator.domain.repository.UsageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSpeedRepository(speedRepositoryImpl: SpeedRepositoryImpl): SpeedRepository

    @Binds
    @Singleton
    abstract fun bindUsageRepository(usageRepositoryImpl: UsageRepositoryImpl): UsageRepository

    @Binds
    @Singleton
    abstract fun bindAppDataUsageRepository(
        appDataUsageRepositoryImpl: AppDataUsageRepositoryImpl
    ): AppDataUsageRepository
}