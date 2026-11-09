package com.sipun.netspeedindicator.core.util

import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpeedNotificationUpdaterTest {
    private lateinit var notificationManager: NotificationManager
    private lateinit var updater: SpeedNotificationUpdater

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        NotificationHelper.createNotificationChannel(context)
        notificationManager = context.getSystemService(NotificationManager::class.java)
        updater = SpeedNotificationUpdater(context)
        notificationManager.cancelAll()
    }

    @Test
    fun `update after stop does not recreate the notification`() {
        updater.start()
        updater.stop()

        updater.update(
            downloadSpeed = "1 KB/s",
            uploadSpeed = null,
            totalSpeed = "1 KB/s",
            mobileUsage = "0 B",
            wifiUsage = "0 B",
            speedValue = "1",
            speedUnit = "KB/s",
            showOnLockScreen = true
        )

        assertFalse(
            notificationManager.activeNotifications.any {
                it.id == NotificationHelper.NOTIFICATION_ID
            }
        )
    }

    @Test
    fun `update posts notification while enabled`() {
        updater.start()

        updater.update(
            downloadSpeed = "1 KB/s",
            uploadSpeed = null,
            totalSpeed = "1 KB/s",
            mobileUsage = "0 B",
            wifiUsage = "0 B",
            speedValue = "1",
            speedUnit = "KB/s",
            showOnLockScreen = true
        )

        assertTrue(
            notificationManager.activeNotifications.any {
                it.id == NotificationHelper.NOTIFICATION_ID
            }
        )
        updater.stop()
    }
}
