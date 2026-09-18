package com.sipun.netspeedindicator.data.repository

import com.sipun.netspeedindicator.data.datasource.SpeedDataSource
import com.sipun.netspeedindicator.domain.model.SpeedInfo
import com.sipun.netspeedindicator.domain.repository.SpeedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SpeedRepository
 * Bridges data source and domain layer
 */
@Singleton
class SpeedRepositoryImpl @Inject constructor(
    private val speedDataSource: SpeedDataSource
) : SpeedRepository {

    override fun observeSpeed(): Flow<SpeedInfo> {
        return speedDataSource.observeSpeed(intervalMs = 1000L)
            .map { sample ->
                SpeedInfo(
                    downloadBytesPerSecond = sample.downloadBytesPerSecond,
                    uploadBytesPerSecond = sample.uploadBytesPerSecond,
                    totalBytesPerSecond = sample.downloadBytesPerSecond + sample.uploadBytesPerSecond,
                    downloadBytes = sample.downloadBytes,
                    uploadBytes = sample.uploadBytes,
                    timestamp = System.currentTimeMillis()
                )
            }
    }
}
