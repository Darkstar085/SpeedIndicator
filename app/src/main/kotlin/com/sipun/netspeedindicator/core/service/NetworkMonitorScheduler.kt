package com.sipun.netspeedindicator.core.service

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.net.NetworkCapabilities
import android.net.NetworkRequest

object NetworkMonitorScheduler {
    private const val JOB_ID = 1001
    private const val RECONNECT_JOB_ID = 1002

    private fun buildValidatedNetworkRequest(): NetworkRequest =
        NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
            .addTransportType(NetworkCapabilities.TRANSPORT_USB)
            .build()

    fun schedule(context: Context) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val job =
            JobInfo.Builder(JOB_ID, ComponentName(context, NetworkMonitorJobService::class.java))
                .setRequiredNetwork(buildValidatedNetworkRequest())
                .setPeriodic(6 * 60 * 60 * 1000L)
                .setPersisted(true)
                .build()
        scheduler.schedule(job)
    }

    fun scheduleReconnect(context: Context) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val job = JobInfo.Builder(
            RECONNECT_JOB_ID,
            ComponentName(context, NetworkMonitorJobService::class.java)
        )
            .setRequiredNetwork(buildValidatedNetworkRequest())
            .setPersisted(true)
            .build()
        scheduler.schedule(job)
    }

    fun cancel(context: Context) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        scheduler.cancel(JOB_ID)
        scheduler.cancel(RECONNECT_JOB_ID)
    }
}
