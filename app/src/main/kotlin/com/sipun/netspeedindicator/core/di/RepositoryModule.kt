package com.sipun.netspeedindicator.core.di

import com.sipun.netspeedindicator.data.repository.SpeedRepositoryImpl
import com.sipun.netspeedindicator.data.repository.UsageRepositoryImpl
import com.sipun.netspeedindicator.domain.repository.SpeedRepository
import com.sipun.netspeedindicator.domain.repository.UsageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository bindings
 * Binds repository interfaces to implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindSpeedRepository(
        speedRepositoryImpl: SpeedRepositoryImpl
    ): SpeedRepository
    
    @Binds
    @Singleton
    abstract fun bindUsageRepository(
        usageRepositoryImpl: UsageRepositoryImpl
    ): UsageRepository
}
