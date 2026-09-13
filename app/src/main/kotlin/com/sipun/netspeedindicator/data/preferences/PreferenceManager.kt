package com.sipun.netspeedindicator.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    companion object {
        const val KEY_APP_THEME = "app_theme"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_PURE_BLACK_THEME = "pure_black_theme"
        const val KEY_LOCK_SCREEN_NOTIFICATION = "lock_screen_notification"
        const val KEY_SHOW_UPLOAD_SPEED = "show_upload_speed"
        const val KEY_MONITORING_ENABLED = "monitoring_enabled"
    }

    val appTheme: Flow<Int> = getIntFlow(KEY_APP_THEME, 0)
    fun setAppTheme(theme: Int) { sharedPreferences.edit().putInt(KEY_APP_THEME, theme).apply() }
    val dynamicColor: Flow<Boolean> = getBooleanFlow(KEY_DYNAMIC_COLOR, true)
    fun setDynamicColor(enabled: Boolean) { sharedPreferences.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply() }
    val pureBlackTheme: Flow<Boolean> = getBooleanFlow(KEY_PURE_BLACK_THEME, false)
    fun setPureBlackTheme(enabled: Boolean) { sharedPreferences.edit().putBoolean(KEY_PURE_BLACK_THEME, enabled).apply() }
    val lockScreenNotification: Flow<Boolean> = getBooleanFlow(KEY_LOCK_SCREEN_NOTIFICATION, true)
    fun setLockScreenNotification(enabled: Boolean) { sharedPreferences.edit().putBoolean(KEY_LOCK_SCREEN_NOTIFICATION, enabled).apply() }
    val showUploadSpeed: Flow<Boolean> = getBooleanFlow(KEY_SHOW_UPLOAD_SPEED, false)
    fun setShowUploadSpeed(enabled: Boolean) { sharedPreferences.edit().putBoolean(KEY_SHOW_UPLOAD_SPEED, enabled).apply() }
    val monitoringEnabled: Flow<Boolean> = getBooleanFlow(KEY_MONITORING_ENABLED, true)
    fun isMonitoringEnabled(): Boolean = sharedPreferences.getBoolean(KEY_MONITORING_ENABLED, true)
    fun setMonitoringEnabled(enabled: Boolean) { sharedPreferences.edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply() }

    private fun getIntFlow(key: String, defaultValue: Int): Flow<Int> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, k -> if (k == key) trySend(prefs.getInt(key, defaultValue)) }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.onStart { emit(sharedPreferences.getInt(key, defaultValue)) }

    private fun getBooleanFlow(key: String, defaultValue: Boolean): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, k -> if (k == key) trySend(prefs.getBoolean(key, defaultValue)) }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.onStart { emit(sharedPreferences.getBoolean(key, defaultValue)) }
}
