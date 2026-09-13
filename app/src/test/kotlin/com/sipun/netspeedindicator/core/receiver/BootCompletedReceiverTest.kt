package com.sipun.netspeedindicator.core.receiver

import android.content.Context
import android.content.Intent
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
    fun `onReceive starts service on ACTION_BOOT_COMPLETED`() {
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        receiver.onReceive(context, intent)

        val shadowApp = ShadowApplication.getInstance()
        val startedService = shadowApp.peekNextStartedService()
        assertNotNull(startedService)
        assertEquals(SpeedMonitorService::class.java.name, startedService.component?.className)
    }

    @Test
    fun `onReceive starts service on QUICKBOOT_POWERON`() {
        val intent = Intent("android.intent.action.QUICKBOOT_POWERON")

        receiver.onReceive(context, intent)

        val shadowApp = ShadowApplication.getInstance()
        val startedService = shadowApp.peekNextStartedService()
        assertNotNull(startedService)
        assertEquals(SpeedMonitorService::class.java.name, startedService.component?.className)
    }

    @Test
    fun `onReceive does not start service on unrelated action`() {
        val intent = Intent(Intent.ACTION_VIEW)

        receiver.onReceive(context, intent)

        val shadowApp = ShadowApplication.getInstance()
        val startedService = shadowApp.peekNextStartedService()
        assertNull(startedService)
    }
}
