package com.sipun.netspeedindicator.core.service

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NetworkMonitorSchedulerTest {
    private lateinit var context: Context
    private lateinit var jobScheduler: JobScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        jobScheduler = context.getSystemService(JobScheduler::class.java)
        jobScheduler.cancelAll()
    }

    @Test
    fun `scheduleReconnect waits for validated internet without a periodic interval`() {
        NetworkMonitorScheduler.scheduleReconnect(context)

        val job = jobScheduler.allPendingJobs.single()
        assertEquals(JobInfo.NETWORK_TYPE_ANY, job.networkType)
        assertFalse(job.isPeriodic)
        assertTrue(job.isPersisted)
    }

    @Test
    fun `cancel removes periodic and reconnect jobs`() {
        NetworkMonitorScheduler.schedule(context)
        NetworkMonitorScheduler.scheduleReconnect(context)

        assertEquals(2, jobScheduler.allPendingJobs.size)

        NetworkMonitorScheduler.cancel(context)

        assertTrue(jobScheduler.allPendingJobs.isEmpty())
    }
}
