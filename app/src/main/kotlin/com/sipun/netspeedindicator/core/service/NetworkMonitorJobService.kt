package com.sipun.netspeedindicator.core.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import androidx.core.content.ContextCompat
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NetworkMonitorJobService : JobService() {
    @Inject
    lateinit var preferenceManager: PreferenceManager
    override fun onStartJob(params: JobParameters): Boolean {
        if (!preferenceManager.isMonitoringEnabled()) {
            jobFinished(params, false)
            return false
        }

        return try {
            startMonitoringService()
            jobFinished(params, false)
            false
        } catch (error: Exception) {
            android.util.Log.e(
                "NetworkMonitorJobService",
                "Unable to restart speed monitoring",
                error
            )
            jobFinished(params, true)
            false
        }
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    private fun startMonitoringService() {
        if (preferenceManager.isMonitoringEnabled()) {
            ContextCompat.startForegroundService(
                this,
                Intent(this, SpeedMonitorService::class.java)
            )
        }
    }
}
