package com.sipun.netspeedindicator.ui.screens.main.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sipun.netspeedindicator.core.state.TrafficStateManager
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
import java.time.LocalDate

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getMonthlyUsageUseCase: GetMonthlyUsageUseCase,
    private val trafficStateManager: TrafficStateManager
) : ViewModel() {
    companion object { private const val TAG = "HistoryViewModel" }

    private val _dailyUsage = MutableStateFlow<List<UsageInfo>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _selectedRange = MutableStateFlow(HistoryRange.SEVEN_DAYS)
    private var loadJob: Job? = null

    val uiState: StateFlow<HistoryUiState> = combine(
        _dailyUsage,
        _isLoading,
        _selectedRange
    ) { dailyUsage, isLoading, selectedRange ->
        HistoryUiState(dailyUsage, isLoading, selectedRange)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HistoryUiState()
    )

    init {
        loadRange(HistoryRange.SEVEN_DAYS)
        observeRealtimeUsage()
    }

    fun onEvent(event: HistoryUiEvent) {
        when (event) {
            is HistoryUiEvent.OnSelectRange -> selectRange(event.range)
        }
    }

    private fun selectRange(range: HistoryRange) {
        _selectedRange.value = range
        loadRange(range)
    }

    private fun observeRealtimeUsage() {
        viewModelScope.launch {
            trafficStateManager.dailyUsage.collect { liveUsage ->
                if (liveUsage.date.isBlank()) return@collect
                val range = _selectedRange.value
                val today = LocalDate.now()
                val shouldInclude = when (range) {
                    HistoryRange.SEVEN_DAYS -> liveUsage.date >= today.minusDays(6).toString()
                    HistoryRange.THIS_MONTH,
                    HistoryRange.THREE_MONTHS -> true
                }
                if (shouldInclude) {
                    _dailyUsage.value = mergeLiveUsage(_dailyUsage.value, liveUsage)
                }
            }
        }
    }

    private fun loadRange(range: HistoryRange) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                val today = LocalDate.now()
                val (startDate, endDate) = when (range) {
                    HistoryRange.SEVEN_DAYS -> today.minusDays(6) to today
                    HistoryRange.THIS_MONTH -> today.withDayOfMonth(1) to today.withDayOfMonth(1).plusMonths(1).minusDays(1)
                    HistoryRange.THREE_MONTHS -> today.withDayOfMonth(1).minusMonths(2) to today.withDayOfMonth(1).plusMonths(1).minusDays(1)
                }

                var calendar = getMonthlyUsageUseCase.getDateRangeCalendar(startDate, endDate)
                calendar = mergeLiveUsage(calendar, trafficStateManager.dailyUsage.value)

                if (_selectedRange.value == range) {
                    _dailyUsage.value = calendar
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load usage history for range=$range", e)
            } finally {
                if (_selectedRange.value == range) _isLoading.value = false
            }
        }
    }

    private fun mergeLiveUsage(list: List<UsageInfo>, liveUsage: UsageInfo): List<UsageInfo> {
        if (liveUsage.date.isBlank()) return list.sortedByDescending { it.date }
        return (list.filterNot { it.date == liveUsage.date } + liveUsage)
            .sortedByDescending { it.date }
    }
}
