package com.sipun.netspeedindicator.core.service

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context

object NetworkMonitorScheduler {
    private const val JOB_ID = 1001
    private const val RECONNECT_JOB_ID = 1002

    fun schedule(context: Context) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val job =
            JobInfo.Builder(JOB_ID, ComponentName(context, NetworkMonitorJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
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
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
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
