package com.sipun.netspeedindicator.presentation.home

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sipun.netspeedindicator.core.service.SpeedMonitorService
import com.sipun.netspeedindicator.data.manager.TrafficStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trafficStateManager: TrafficStateManager
) : ViewModel() {

    private val _showStopDialog = MutableStateFlow(false)
    private val _shouldFinishActivity = MutableStateFlow(false)
    private val _sessionDurationSeconds = MutableStateFlow(0L)

    init {
        viewModelScope.launch {
            trafficStateManager.monitoringStartElapsedRealtime.collectLatest { startTime ->
                if (startTime == 0L) {
                    _sessionDurationSeconds.value = 0L
                    return@collectLatest
                }
                while (true) {
                    _sessionDurationSeconds.value = (SystemClock.elapsedRealtime() - startTime) / 1000
                    delay(1000)
                }
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        trafficStateManager.isServiceRunning,
        trafficStateManager.speed,
        trafficStateManager.dailyUsage,
        trafficStateManager.peakSpeedBytesPerSecond,
        _sessionDurationSeconds
    ) { isRunning, speed, usage, peak, session ->
        HomeUiState(
            isServiceRunning = isRunning,
            currentSpeed = speed,
            todayUsage = usage,
            peakSpeed = peak,
            sessionDurationSeconds = session
        )
    }.combine(_showStopDialog) { state, showDialog ->
        state.copy(showStopDialog = showDialog)
    }.combine(_shouldFinishActivity) { state, shouldFinish ->
        state.copy(shouldFinishActivity = shouldFinish)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeUiState()
    )

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.OnStopClick -> _showStopDialog.value = true
            HomeUiEvent.OnDismissDialog -> _showStopDialog.value = false
            HomeUiEvent.OnConfirmStop -> stopService()
            HomeUiEvent.OnActivityFinished -> _shouldFinishActivity.value = false
        }
    }

    private fun stopService() {
        context.stopService(Intent(context, SpeedMonitorService::class.java))
        _showStopDialog.value = false
        _shouldFinishActivity.value = true
    }
}
