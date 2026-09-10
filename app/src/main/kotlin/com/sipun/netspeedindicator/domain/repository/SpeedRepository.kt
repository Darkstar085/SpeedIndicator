package com.sipun.netspeedindicator.domain.repository

import com.sipun.netspeedindicator.domain.model.SpeedInfo
import kotlinx.coroutines.flow.Flow

interface SpeedRepository {
    fun observeSpeed(): Flow<SpeedInfo>
    suspend fun getCurrentSpeed(): SpeedInfo
}
