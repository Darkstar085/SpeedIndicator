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
import com.sipun.netspeedindicator.domain.model.LiveAppRate
import com.sipun.netspeedindicator.domain.repository.InstalledAppsProvider
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
import kotlinx.coroutines.flow.map
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
    private val getDailyUsageUseCase: GetDailyUsageUseCase,
    private val installedAppsProvider: InstalledAppsProvider
) : ViewModel() {
    companion object {
        private const val MAX_ACTIVE_APPS = 4
    }

    private data class ActiveApps(
        val top: List<LiveAppRate> = emptyList(),
        val totalCount: Int = 0
    )

    private val _showStopDialog = MutableStateFlow(false)

    /** Live per-app rates, resolved against installed package info, sorted desc. */
    private val activeApps = trafficStateManager.appLiveRates
        .map { rates ->
            val sorted = rates
                .filterValues { it > 0L }
                .mapNotNull { (uid, rate) ->
                    installedAppsProvider.appInfoForUid(uid)?.let { info ->
                        LiveAppRate(uid, info.packageName, info.appName, rate)
                    }
                }
                .sortedByDescending { it.bytesPerSecond }
            ActiveApps(top = sorted.take(MAX_ACTIVE_APPS), totalCount = sorted.size)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveApps())

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
        .combine(activeApps) { state, apps ->
            state.copy(activeApps = apps.top, activeAppCount = apps.totalCount)
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
