package com.sipun.netspeedindicator.ui.screens.main.appusage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sipun.netspeedindicator.domain.model.AppDataUsage
import com.sipun.netspeedindicator.domain.repository.AppDataUsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class AppUsagePeriod(val label: String) {
    TODAY("Today"),
    WEEK("7 Days"),
    MONTH("This Month")
}

data class AppDataUsageUiState(
    val period: AppUsagePeriod = AppUsagePeriod.TODAY,
    val apps: List<AppDataUsage> = emptyList(),
    val isLoading: Boolean = false,
    val hasUsageAccess: Boolean = false
) {
    val totalBytes: Long get() = apps.sumOf { it.totalBytes }
    val wifiBytes: Long get() = apps.sumOf { it.wifiBytes }
    val mobileBytes: Long get() = apps.sumOf { it.mobileBytes }
}

@HiltViewModel
class AppDataUsageViewModel @Inject constructor(
    private val repository: AppDataUsageRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppDataUsageUiState())
    val uiState: StateFlow<AppDataUsageUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun setPeriod(period: AppUsagePeriod) {
        _uiState.value = _uiState.value.copy(period = period)
        refresh()
    }

    fun refresh() {
        val access = repository.hasUsageAccess()
        _uiState.value = _uiState.value.copy(hasUsageAccess = access)
        if (!access) {
            _uiState.value = _uiState.value.copy(apps = emptyList(), isLoading = false)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val today = LocalDate.now()
            val startDate = when (_uiState.value.period) {
                AppUsagePeriod.TODAY -> today
                AppUsagePeriod.WEEK -> today.minusDays(6)
                AppUsagePeriod.MONTH -> today.minusDays(29)
            }
            val start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val apps = repository.getAppDataUsage(start, System.currentTimeMillis())
            _uiState.value = _uiState.value.copy(apps = apps, isLoading = false, hasUsageAccess = true)
        }
    }
}