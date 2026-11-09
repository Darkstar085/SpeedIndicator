package com.sipun.netspeedindicator.core.receiver

import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.sipun.netspeedindicator.core.service.NetworkMonitorJobService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BootCompletedReceiverTest {

    private lateinit var context: Context
    private lateinit var receiver: BootCompletedReceiver
    private lateinit var jobScheduler: JobScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        receiver = BootCompletedReceiver()
        jobScheduler = context.getSystemService(JobScheduler::class.java)
        jobScheduler.cancelAll()
    }

    @Test
    fun `onReceive schedules monitoring after ACTION_BOOT_COMPLETED`() {
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        val job = jobScheduler.allPendingJobs.singleOrNull()
        assertNotNull(job)
        assertEquals(NetworkMonitorJobService::class.java.name, job?.service?.className)
    }

    @Test
    fun `onReceive schedules monitoring after QUICKBOOT_POWERON`() {
        receiver.onReceive(context, Intent("android.intent.action.QUICKBOOT_POWERON"))

        val job = jobScheduler.allPendingJobs.singleOrNull()
        assertNotNull(job)
        assertEquals(NetworkMonitorJobService::class.java.name, job?.service?.className)
    }

    @Test
    fun `onReceive ignores unrelated actions`() {
        receiver.onReceive(context, Intent(Intent.ACTION_VIEW))

        assertTrue(jobScheduler.allPendingJobs.isEmpty())
    }
}
