package com.sipun.netspeedindicator.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.sipun.netspeedindicator.MainActivity
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.core.util.SpeedNotificationUpdater

/**
 * Minimal live-speed widget for the home screen. The monitoring service pushes
 * updates whenever the notification text changes (see SpeedNotificationUpdater);
 * the snapshot is shared through the same preferences used by the Quick Settings
 * tile. Tapping the widget opens the app.
 */
class SpeedWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAll(context)
    }

    override fun onEnabled(context: Context) {
        updateAll(context)
    }

    companion object {
        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, SpeedWidget::class.java)
            )
            if (ids.isEmpty()) return

            val snapshot = context.getSharedPreferences(
                SpeedNotificationUpdater.TILE_PREFS_NAME,
                Context.MODE_PRIVATE
            ).getString(SpeedNotificationUpdater.KEY_TILE_SNAPSHOT, null)

            val views = RemoteViews(context.packageName, R.layout.widget_speed)
            views.setTextViewText(
                R.id.widget_value,
                snapshot ?: context.getString(R.string.tile_monitoring_off)
            )

            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            appWidgetManager.updateAppWidget(ids, views)
        }
    }
}
