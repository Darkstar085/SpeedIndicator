package com.sipun.netspeedindicator.core.tile

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.core.service.NetworkMonitorScheduler
import com.sipun.netspeedindicator.core.service.SpeedMonitorService
import com.sipun.netspeedindicator.core.util.SpeedNotificationUpdater
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quick Settings tile mirroring the speed pill in the notification shade.
 * Tapping toggles speed monitoring on/off; the subtitle shows the latest
 * speed reading when available.
 */
@AndroidEntryPoint
class SpeedTileService : TileService() {
    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val tileScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val snapshotPreferences by lazy {
        getSharedPreferences(SpeedNotificationUpdater.TILE_PREFS_NAME, MODE_PRIVATE)
    }

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        tileScope.launch {
            val newValue = !preferenceManager.isMonitoringEnabled()
            preferenceManager.setMonitoringEnabled(newValue)
            if (newValue) {
                NetworkMonitorScheduler.schedule(this@SpeedTileService)
                ContextCompat.startForegroundService(
                    this@SpeedTileService,
                    Intent(this@SpeedTileService, SpeedMonitorService::class.java)
                )
            } else {
                NetworkMonitorScheduler.cancel(this@SpeedTileService)
                this@SpeedTileService.stopService(
                    Intent(this@SpeedTileService, SpeedMonitorService::class.java)
                )
            }
            refreshTile()
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val enabled = preferenceManager.isMonitoringEnabled()
        val snapshot = snapshotPreferences.getString(SpeedNotificationUpdater.KEY_TILE_SNAPSHOT, null)

        tile.label = getString(R.string.tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.subtitle = when {
                !enabled -> getString(R.string.tile_monitoring_off)
                snapshot.isNullOrBlank() -> getString(R.string.tile_monitoring_waiting)
                else -> snapshot
            }
        }
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
