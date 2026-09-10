package com.sipun.netspeedindicator.data.repository

import com.sipun.netspeedindicator.data.datasource.SpeedDataSource
import com.sipun.netspeedindicator.domain.model.SpeedInfo
import com.sipun.netspeedindicator.domain.repository.SpeedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeedRepositoryImpl @Inject constructor(
    private val speedDataSource: SpeedDataSource
) : SpeedRepository {
    override fun observeSpeed(): Flow<SpeedInfo> = speedDataSource.observeSpeed().map { (download, upload) ->
        SpeedInfo(download, upload, download + upload, System.currentTimeMillis())
    }

    override suspend fun getCurrentSpeed(): SpeedInfo = SpeedInfo(
        timestamp = System.currentTimeMillis()
    )
}
