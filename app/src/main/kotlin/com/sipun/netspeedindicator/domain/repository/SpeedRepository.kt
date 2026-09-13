package com.sipun.netspeedindicator.domain.repository

import com.sipun.netspeedindicator.domain.model.SpeedInfo
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for speed monitoring
 * Domain layer - defines contract without implementation details
 */
interface SpeedRepository {
    /**
     * Observe current internet speed in real-time
     * Emits speed updates periodically
     */
    fun observeSpeed(): Flow<SpeedInfo>
    
    /**
     * Get current speed snapshot
     */
    suspend fun getCurrentSpeed(): SpeedInfo
}
