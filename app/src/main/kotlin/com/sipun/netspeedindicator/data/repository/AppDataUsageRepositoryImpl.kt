package com.sipun.netspeedindicator.data.repository

import com.sipun.netspeedindicator.data.datasource.AppDataUsageDataSource
import com.sipun.netspeedindicator.domain.model.AppDataUsage
import com.sipun.netspeedindicator.domain.repository.AppDataUsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDataUsageRepositoryImpl @Inject constructor(
    private val dataSource: AppDataUsageDataSource
) : AppDataUsageRepository {
    override suspend fun getAppDataUsage(startMillis: Long, endMillis: Long): List<AppDataUsage> =
        withContext(Dispatchers.IO) {
            dataSource.getAppDataUsage(startMillis, endMillis).map {
                AppDataUsage(it.uid, it.packageName, it.appName, it.totalBytes, it.wifiBytes, it.mobileBytes, it.downloadBytes, it.uploadBytes)
            }
        }

    override fun hasUsageAccess(): Boolean = dataSource.hasUsageAccess()
}