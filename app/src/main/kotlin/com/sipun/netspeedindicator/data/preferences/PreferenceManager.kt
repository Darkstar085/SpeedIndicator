package com.sipun.netspeedindicator.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    companion object {
        const val KEY_APP_THEME = "app_theme"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_PURE_BLACK_THEME = "pure_black_theme"
        const val KEY_LOCK_SCREEN_NOTIFICATION = "lock_screen_notification"
        const val KEY_SHOW_UPLOAD_SPEED = "show_upload_speed"
        const val KEY_MONITORING_ENABLED = "monitoring_enabled"
        const val KEY_ACCENT = "accent"


        private val APP_THEME = intPreferencesKey(KEY_APP_THEME)
        private val DYNAMIC_COLOR = booleanPreferencesKey(KEY_DYNAMIC_COLOR)
        private val PURE_BLACK_THEME = booleanPreferencesKey(KEY_PURE_BLACK_THEME)
        private val LOCK_SCREEN_NOTIFICATION = booleanPreferencesKey(KEY_LOCK_SCREEN_NOTIFICATION)
        private val SHOW_UPLOAD_SPEED = booleanPreferencesKey(KEY_SHOW_UPLOAD_SPEED)
        private val MONITORING_ENABLED = booleanPreferencesKey(KEY_MONITORING_ENABLED)
        private val ACCENT = intPreferencesKey(KEY_ACCENT)

    }

    private val dataStore = context.appPreferencesDataStore
    private val preferences = dataStore.data.catch { exception ->
        if (exception is IOException) emit(emptyPreferences()) else throw exception
    }

    /**
     * Read the persisted monitoring flag synchronously once so [isMonitoringEnabled]
     * is accurate even during a cold start (e.g. after a reboot), before the
     * DataStore flow has emitted.
     */
    private val initialMonitoringEnabled: Boolean = runCatching {
        runBlocking { preferences.first()[MONITORING_ENABLED] ?: true }
    }.getOrDefault(true)

    val appTheme: Flow<Int> = preferences.map { it[APP_THEME] ?: 0 }
    val dynamicColor: Flow<Boolean> = preferences.map { it[DYNAMIC_COLOR] ?: true }
    val pureBlackTheme: Flow<Boolean> = preferences.map { it[PURE_BLACK_THEME] ?: false }
    val lockScreenNotification: Flow<Boolean> =
        preferences.map { it[LOCK_SCREEN_NOTIFICATION] ?: true }
    val showUploadSpeed: Flow<Boolean> = preferences.map { it[SHOW_UPLOAD_SPEED] ?: false }
    val monitoringEnabled: Flow<Boolean> = preferences.map { it[MONITORING_ENABLED] ?: true }
    /** 0 = dynamic (wallpaper) colors; 1..5 = static accent palettes. */
    val accent: Flow<Int> = preferences.map { it[ACCENT] ?: 0 }


    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    @Volatile
    private var monitoringEnabledValue = initialMonitoringEnabled

    init {
        scope.launch {
            monitoringEnabled.collect { monitoringEnabledValue = it }
        }
    }

    fun isMonitoringEnabled(): Boolean = monitoringEnabledValue

    suspend fun setAppTheme(theme: Int) = update { it[APP_THEME] = theme }
    suspend fun setDynamicColor(enabled: Boolean) = update { it[DYNAMIC_COLOR] = enabled }
    suspend fun setPureBlackTheme(enabled: Boolean) = update { it[PURE_BLACK_THEME] = enabled }
    suspend fun setLockScreenNotification(enabled: Boolean) =
        update { it[LOCK_SCREEN_NOTIFICATION] = enabled }

    suspend fun setShowUploadSpeed(enabled: Boolean) = update { it[SHOW_UPLOAD_SPEED] = enabled }
    suspend fun setAccent(accent: Int) = update { it[ACCENT] = accent }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        // Update the volatile value before dispatching so synchronous callers
        // (service start, cold-start checks) observe the new value immediately.
        monitoringEnabledValue = enabled
        withContext(ioDispatcher) { dataStore.edit { it[MONITORING_ENABLED] = enabled } }
    }

    private suspend fun update(transform: suspend (MutablePreferences) -> Unit) {
        withContext(ioDispatcher) { dataStore.edit(transform) }
    }
}
