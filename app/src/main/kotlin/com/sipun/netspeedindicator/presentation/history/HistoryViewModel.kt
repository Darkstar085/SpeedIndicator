package com.sipun.netspeedindicator.presentation.history

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

/**
 * ViewModel for History screen
 * Manages daily and monthly usage history
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getMonthlyUsageUseCase: GetMonthlyUsageUseCase,
    private val trafficStateManager: TrafficStateManager
) : ViewModel() {

    companion object {
        private const val TAG = "HistoryViewModel"
    }

    private val _dailyUsage = MutableStateFlow<List<UsageInfo>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _selectedMonthIndex = MutableStateFlow(0)
    private var loadJob: Job? = null

    val uiState: StateFlow<HistoryUiState> = combine(
        _dailyUsage, _isLoading, _selectedMonthIndex
    ) { dailyUsage, isLoading, selectedMonthIndex ->
        HistoryUiState(
            dailyUsage = dailyUsage,
            isLoading = isLoading,
            selectedMonthIndex = selectedMonthIndex
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    init {
        loadDailyUsage(0)
        observeRealtimeUsage()
    }

    fun onEvent(event: HistoryUiEvent) {
        when (event) {
            is HistoryUiEvent.OnSelectMonth -> selectMonth(event.monthsBack)
        }
    }

    private fun selectMonth(monthsBack: Int) {
        _selectedMonthIndex.value = monthsBack
        loadDailyUsage(monthsBack)
    }

    /** Overlays the live, in-memory "today" reading onto the current month's list as it streams in. */
    private fun observeRealtimeUsage() {
        viewModelScope.launch {
            trafficStateManager.dailyUsage.collect { liveUsage ->
                if (_selectedMonthIndex.value == 0) {
                    _dailyUsage.value = mergeLiveUsage(_dailyUsage.value, liveUsage)
                }
            }
        }
    }

    private fun loadDailyUsage(monthsBack: Int) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                var calendar = getMonthlyUsageUseCase.getMonthCalendar(monthsBack)
                if (monthsBack == 0) {
                    calendar = mergeLiveUsage(calendar, trafficStateManager.dailyUsage.value)
                }
                _dailyUsage.value = calendar
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load usage history for monthsBack=$monthsBack", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** [list] is assumed date-descending; replaces or prepends the entry matching [liveUsage]'s date. */
    private fun mergeLiveUsage(list: List<UsageInfo>, liveUsage: UsageInfo): List<UsageInfo> {
        val index = list.indexOfFirst { it.date == liveUsage.date }
        if (index == -1) return listOf(liveUsage) + list
        return list.toMutableList().apply { this[index] = liveUsage }
    }
}
