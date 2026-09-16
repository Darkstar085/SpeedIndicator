package com.sipun.netspeedindicator.ui.screens.main.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sipun.netspeedindicator.data.manager.TrafficStateManager
import com.sipun.netspeedindicator.domain.model.UsageInfo
import com.sipun.netspeedindicator.domain.usecase.GetMonthlyUsageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getMonthlyUsageUseCase: GetMonthlyUsageUseCase,
    private val trafficStateManager: TrafficStateManager
) : ViewModel() {
    companion object { private const val TAG = "HistoryViewModel" }
    private val _dailyUsage = MutableStateFlow<List<UsageInfo>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _selectedMonthIndex = MutableStateFlow(0)
    private var loadJob: Job? = null

    val uiState: StateFlow<HistoryUiState> = combine(_dailyUsage, _isLoading, _selectedMonthIndex) { dailyUsage, isLoading, selectedMonthIndex -> HistoryUiState(dailyUsage, isLoading, selectedMonthIndex) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    init { loadDailyUsage(0); observeRealtimeUsage() }

    fun onEvent(event: HistoryUiEvent) {
        when (event) { is HistoryUiEvent.OnSelectMonth -> selectMonth(event.monthsBack) }
    }

    private fun selectMonth(monthsBack: Int) {
        _selectedMonthIndex.value = monthsBack
        loadDailyUsage(monthsBack)
    }

    private fun observeRealtimeUsage() {
        viewModelScope.launch {
            trafficStateManager.dailyUsage.collect { liveUsage ->
                if (_selectedMonthIndex.value == 0) _dailyUsage.value = mergeLiveUsage(_dailyUsage.value, liveUsage)
            }
        }
    }

    private fun loadDailyUsage(monthsBack: Int) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                var calendar = getMonthlyUsageUseCase.getMonthCalendar(monthsBack)
                if (monthsBack == 0) calendar = mergeLiveUsage(calendar, trafficStateManager.dailyUsage.value)
                if (_selectedMonthIndex.value == monthsBack) _dailyUsage.value = calendar
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load usage history for monthsBack=$monthsBack", e)
            } finally {
                if (_selectedMonthIndex.value == monthsBack) _isLoading.value = false
            }
        }
    }

    private fun mergeLiveUsage(list: List<UsageInfo>, liveUsage: UsageInfo): List<UsageInfo> {
        val merged = list.filterNot { it.date == liveUsage.date } + liveUsage
        return merged.sortedByDescending { it.date }
    }
}
