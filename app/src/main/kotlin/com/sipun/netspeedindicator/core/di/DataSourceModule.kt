package com.sipun.netspeedindicator.core.di

import com.sipun.netspeedindicator.data.datasource.InstalledAppsProviderImpl
import com.sipun.netspeedindicator.domain.repository.InstalledAppsProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {
    @Binds
    @Singleton
    abstract fun bindInstalledAppsProvider(
        impl: InstalledAppsProviderImpl
    ): InstalledAppsProvider
}
