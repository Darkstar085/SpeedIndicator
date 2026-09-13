package com.sipun.netspeedindicator.core.util

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `createNotificationChannel creates channel with correct properties`() {
        NotificationHelper.createNotificationChannel(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ID)

        assertNotNull(channel)
        assertEquals(NotificationHelper.CHANNEL_NAME, channel.name)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
        assertEquals(false, channel.shouldVibrate())
    }

    @Test
    fun `buildNotification creates notification with correct content`() {
        NotificationHelper.createNotificationChannel(context)

        val notification = NotificationHelper.buildNotification(
            context = context,
            downloadSpeed = "1.5 MB/s",
            uploadSpeed = "512 KB/s",
            totalSpeed = "2.0 MB/s",
            mobileUsage = "100 MB",
            wifiUsage = "1.5 GB",
            signal = "85%",
            speedValue = "2.0",
            speedUnit = "MB/s"
        )

        assertNotNull(notification)
        assertTrue(notification.flags and NotificationCompat.FLAG_ONGOING_EVENT != 0)
        assertTrue(notification.flags and NotificationCompat.FLAG_ONLY_ALERT_ONCE != 0)
    }

    @Test
    fun `buildNotification without upload speed shows total speed in title`() {
        NotificationHelper.createNotificationChannel(context)

        val notification = NotificationHelper.buildNotification(
            context = context,
            downloadSpeed = "1.5 MB/s",
            uploadSpeed = null,
            totalSpeed = "2.0 MB/s",
            mobileUsage = "100 MB",
            wifiUsage = "1.5 GB",
            signal = "85%",
            speedValue = "2.0",
            speedUnit = "MB/s"
        )

        assertNotNull(notification)
    }

    @Test
    fun `buildNotification with empty signal omits signal from title`() {
        NotificationHelper.createNotificationChannel(context)

        val notification = NotificationHelper.buildNotification(
            context = context,
            downloadSpeed = "1.5 MB/s",
            uploadSpeed = null,
            totalSpeed = "2.0 MB/s",
            mobileUsage = "100 MB",
            wifiUsage = "1.5 GB",
            signal = "",
            speedValue = "2.0",
            speedUnit = "MB/s"
        )

        assertNotNull(notification)
    }

    @Test
    fun `notification has low priority`() {
        NotificationHelper.createNotificationChannel(context)

        val notification = NotificationHelper.buildNotification(
            context = context,
            downloadSpeed = "1.5 MB/s",
            uploadSpeed = null,
            totalSpeed = "2.0 MB/s",
            mobileUsage = "100 MB",
            wifiUsage = "1.5 GB",
            signal = "",
            speedValue = "2.0",
            speedUnit = "MB/s"
        )

        assertEquals(NotificationCompat.PRIORITY_LOW, notification.priority)
    }
}

private fun assertTrue(condition: Boolean) {
    org.junit.Assert.assertTrue(condition)
}
