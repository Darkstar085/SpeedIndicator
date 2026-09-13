package com.sipun.netspeedindicator.domain.usecase

import com.sipun.netspeedindicator.domain.model.SpeedInfo
import com.sipun.netspeedindicator.domain.repository.SpeedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeSpeedRepository : SpeedRepository {

    var speedResult: SpeedInfo = SpeedInfo()
    var observeSpeedResult: Flow<SpeedInfo> = flowOf(SpeedInfo())

    override fun observeSpeed(): Flow<SpeedInfo> = observeSpeedResult

    override suspend fun getCurrentSpeed(): SpeedInfo = speedResult
}
