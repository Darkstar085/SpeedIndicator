package com.sipun.netspeedindicator.core.receiver

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.sipun.netspeedindicator.core.service.SpeedMonitorService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
class BootCompletedReceiverTest {

    private lateinit var context: Context
    private lateinit var receiver: BootCompletedReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        receiver = BootCompletedReceiver()
    }

    @Test
    fun `onReceive handles boot completed according to Android version`() {
        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertBootBehavior()
    }

    @Test
    fun `onReceive handles quick boot according to Android version`() {
        receiver.onReceive(context, Intent("android.intent.action.QUICKBOOT_POWERON"))
        assertBootBehavior()
    }

    @Test
    fun `onReceive ignores unrelated action`() {
        receiver.onReceive(context, Intent(Intent.ACTION_VIEW))
        assertNull(ShadowApplication.getInstance().peekNextStartedService())
    }

    private fun assertBootBehavior() {
        val startedService = ShadowApplication.getInstance().peekNextStartedService()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            assertNull(startedService)
        } else {
            assertNotNull(startedService)
            assertEquals(SpeedMonitorService::class.java.name, startedService.component?.className)
        }
    }
}
