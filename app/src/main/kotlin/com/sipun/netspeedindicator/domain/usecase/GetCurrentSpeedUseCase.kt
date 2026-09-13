package com.sipun.netspeedindicator.domain.usecase

import com.sipun.netspeedindicator.domain.model.SpeedInfo
import com.sipun.netspeedindicator.domain.repository.SpeedRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe current internet speed
 * Follows single responsibility principle
 */
class GetCurrentSpeedUseCase @Inject constructor(
    private val speedRepository: SpeedRepository
) {
    operator fun invoke(): Flow<SpeedInfo> {
        return speedRepository.observeSpeed()
    }
}
