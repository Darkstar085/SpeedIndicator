package com.sipun.netspeedindicator.ui.screens.main.home

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sipun.netspeedindicator.core.service.NetworkMonitorScheduler
import com.sipun.netspeedindicator.core.service.SpeedMonitorService
import com.sipun.netspeedindicator.data.manager.TrafficStateManager
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trafficStateManager: TrafficStateManager,
    private val preferenceManager: PreferenceManager
) : ViewModel() {
    private val _showStopDialog = MutableStateFlow(false)
    private val _sessionDurationSeconds = MutableStateFlow(0L)

    init {
        viewModelScope.launch {
            trafficStateManager.monitoringStartElapsedRealtime.collectLatest { startTime ->
                if (startTime == 0L) { _sessionDurationSeconds.value = 0L; return@collectLatest }
                while (true) {
                    _sessionDurationSeconds.value = (android.os.SystemClock.elapsedRealtime() - startTime) / 1000
                    delay(1000)
                }
            }
        }
    }

    val uiState = combine(
        trafficStateManager.isServiceRunning,
        trafficStateManager.speed,
        trafficStateManager.dailyUsage,
        trafficStateManager.peakSpeedBytesPerSecond,
        _sessionDurationSeconds
    ) { isRunning, speed, usage, peak, session -> HomeUiState(isRunning, speed, usage, peak, session) }
        .combine(_showStopDialog) { state, showDialog -> state.copy(showStopDialog = showDialog) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.OnStopClick -> if (uiState.value.isServiceRunning) _showStopDialog.value = true else startService()
            HomeUiEvent.OnDismissDialog -> _showStopDialog.value = false
            HomeUiEvent.OnConfirmStop -> stopService()
            HomeUiEvent.OnActivityFinished -> Unit
        }
    }

    private fun startService() {
        preferenceManager.setMonitoringEnabled(true)
        NetworkMonitorScheduler.schedule(context)
        ContextCompat.startForegroundService(context, Intent(context, SpeedMonitorService::class.java))
    }

    private fun stopService() {
        preferenceManager.setMonitoringEnabled(false)
        NetworkMonitorScheduler.cancel(context)
        context.stopService(Intent(context, SpeedMonitorService::class.java))
        _showStopDialog.value = false
    }
}
