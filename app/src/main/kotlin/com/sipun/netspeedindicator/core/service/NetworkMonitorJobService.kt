package com.sipun.netspeedindicator.core.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat

class NetworkMonitorJobService : JobService() {
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var connectivityManager: ConnectivityManager? = null
    private var jobCompleted = false

    override fun onStartJob(params: JobParameters): Boolean {
        jobCompleted = false

        if (!isMonitoringEnabled()) {
            jobFinished(params, false)
            return false
        }

        val manager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager = manager

        if (hasValidatedNetwork()) {
            startMonitoringService()
            finishJob(params, false)
            return false
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    startMonitoringService()
                    finishJob(params, false)
                }
            }
        }
        networkCallback = callback

        return try {
            manager.registerDefaultNetworkCallback(callback)
            true
        } catch (_: Exception) {
            finishJob(params, true)
            false
        }
    }

    override fun onStopJob(params: JobParameters): Boolean {
        cleanup()
        return !jobCompleted
    }

    private fun startMonitoringService() {
        if (isMonitoringEnabled()) {
            ContextCompat.startForegroundService(this, Intent(this, SpeedMonitorService::class.java))
        }
    }

    private fun hasValidatedNetwork(): Boolean {
        val manager = connectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun finishJob(params: JobParameters, reschedule: Boolean) {
        if (jobCompleted) return
        jobCompleted = true
        cleanup()
        jobFinished(params, reschedule)
    }

    private fun cleanup() {
        networkCallback?.let { callback ->
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
        networkCallback = null
        connectivityManager = null
    }

    private fun isMonitoringEnabled(): Boolean =
        getSharedPreferences("app_preferences", MODE_PRIVATE)
            .getBoolean(com.sipun.netspeedindicator.data.preferences.PreferenceManager.KEY_MONITORING_ENABLED, true)
}
