package com.sipun.netspeedindicator.domain.repository

import com.sipun.netspeedindicator.domain.model.AppDataUsage

interface AppDataUsageRepository {
    suspend fun getAppDataUsage(startMillis: Long, endMillis: Long): List<AppDataUsage>
    fun hasUsageAccess(): Boolean
}