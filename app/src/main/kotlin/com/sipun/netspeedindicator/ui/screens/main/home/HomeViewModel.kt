package com.sipun.netspeedindicator.ui.screens.main.home

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sipun.netspeedindicator.core.service.NetworkMonitorScheduler
import com.sipun.netspeedindicator.core.service.SpeedMonitorService
import com.sipun.netspeedindicator.core.state.TrafficStateManager
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import com.sipun.netspeedindicator.domain.usecase.GetDailyUsageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trafficStateManager: TrafficStateManager,
    private val preferenceManager: PreferenceManager,
    private val getDailyUsageUseCase: GetDailyUsageUseCase
) : ViewModel() {
    private val _showStopDialog = MutableStateFlow(false)

    private val sessionDurationSeconds = trafficStateManager.monitoringStartElapsedRealtime
        .flatMapLatest { startTime ->
            if (startTime == 0L) {
                flowOf(0L)
            } else {
                flow {
                    while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                        emit((android.os.SystemClock.elapsedRealtime() - startTime) / 1000)
                        delay(1000)
                    }
                }
            }
        }

    init {
        viewModelScope.launch {
            getDailyUsageUseCase.getToday()?.let { trafficStateManager.updateDailyUsage(it) }
        }
    }

    val uiState = combine(
        trafficStateManager.isServiceRunning,
        trafficStateManager.speed,
        trafficStateManager.dailyUsage,
        trafficStateManager.peakSpeedBytesPerSecond,
        sessionDurationSeconds
    ) { isRunning, speed, usage, peak, session ->
        HomeUiState(
            isRunning,
            speed,
            usage,
            peak,
            session
        )
    }
        .combine(_showStopDialog) { state, showDialog -> state.copy(showStopDialog = showDialog) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.OnStopClick -> if (uiState.value.isServiceRunning) _showStopDialog.value =
                true else startService()

            HomeUiEvent.OnDismissDialog -> _showStopDialog.value = false
            HomeUiEvent.OnConfirmStop -> stopService()
        }
    }

    private fun startService() {
        viewModelScope.launch {
            // Persist the flag first so the service's cold-start check observes it.
            preferenceManager.setMonitoringEnabled(true)
            NetworkMonitorScheduler.schedule(context)
            ContextCompat.startForegroundService(
                context,
                Intent(context, SpeedMonitorService::class.java)
            )
        }
    }

    private fun stopService() {
        viewModelScope.launch { preferenceManager.setMonitoringEnabled(false) }
        NetworkMonitorScheduler.cancel(context)
        context.stopService(Intent(context, SpeedMonitorService::class.java))
        _showStopDialog.value = false
    }
}
