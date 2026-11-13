package com.sipun.netspeedindicator.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

enum class AccentStyle { SOLID, GRADIENT }

enum class AccentGradient {
    SUNSET, OCEAN, AURORA, PURPLE_HAZE, PEACH, EMERALD,
    CHERRY, ROYAL, AMBER, MINT, ROSE, INDIGO, CUSTOM
}

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
        const val KEY_ONBOARDING_SEEN = "onboarding_seen"
        const val KEY_ACCENT = "accent"
        const val KEY_CUSTOM_COLOR = "custom_color"
        const val KEY_ACCENT_STYLE = "accent_style"
        const val KEY_ACCENT_GRADIENT = "accent_gradient"
        const val KEY_CUSTOM_GRADIENT_START = "custom_gradient_start"
        const val KEY_CUSTOM_GRADIENT_END = "custom_gradient_end"



        private val APP_THEME = intPreferencesKey(KEY_APP_THEME)
        private val DYNAMIC_COLOR = booleanPreferencesKey(KEY_DYNAMIC_COLOR)
        private val PURE_BLACK_THEME = booleanPreferencesKey(KEY_PURE_BLACK_THEME)
        private val LOCK_SCREEN_NOTIFICATION = booleanPreferencesKey(KEY_LOCK_SCREEN_NOTIFICATION)
        private val SHOW_UPLOAD_SPEED = booleanPreferencesKey(KEY_SHOW_UPLOAD_SPEED)
        private val MONITORING_ENABLED = booleanPreferencesKey(KEY_MONITORING_ENABLED)
        private val ONBOARDING_SEEN = booleanPreferencesKey(KEY_ONBOARDING_SEEN)
        private val ACCENT = intPreferencesKey(KEY_ACCENT)
        private val CUSTOM_COLOR = intPreferencesKey(KEY_CUSTOM_COLOR)
        private val ACCENT_STYLE = stringPreferencesKey(KEY_ACCENT_STYLE)
        private val ACCENT_GRADIENT = stringPreferencesKey(KEY_ACCENT_GRADIENT)
        private val CUSTOM_GRADIENT_START = intPreferencesKey(KEY_CUSTOM_GRADIENT_START)
        private val CUSTOM_GRADIENT_END = intPreferencesKey(KEY_CUSTOM_GRADIENT_END)
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
    val dynamicColor: Flow<Boolean> = preferences.map { it[DYNAMIC_COLOR] ?: false }
    val pureBlackTheme: Flow<Boolean> = preferences.map { it[PURE_BLACK_THEME] ?: false }
    val lockScreenNotification: Flow<Boolean> =
        preferences.map { it[LOCK_SCREEN_NOTIFICATION] ?: true }
    val showUploadSpeed: Flow<Boolean> = preferences.map { it[SHOW_UPLOAD_SPEED] ?: false }
    val monitoringEnabled: Flow<Boolean> = preferences.map { it[MONITORING_ENABLED] ?: true }
    val onboardingSeen: Flow<Boolean> = preferences.map { it[ONBOARDING_SEEN] ?: false }
    val accentStyle: Flow<AccentStyle> = preferences.map { prefs ->
        runCatching { AccentStyle.valueOf(prefs[ACCENT_STYLE] ?: AccentStyle.SOLID.name) }
            .getOrDefault(AccentStyle.SOLID)
    }
    val accentGradient: Flow<AccentGradient> = preferences.map { prefs ->
        runCatching { AccentGradient.valueOf(prefs[ACCENT_GRADIENT] ?: AccentGradient.SUNSET.name) }
            .getOrDefault(AccentGradient.SUNSET)
    }
    val customGradientStart: Flow<Int> =
        preferences.map { it[CUSTOM_GRADIENT_START] ?: 0xFFE92DE2.toInt() }
    val customGradientEnd: Flow<Int> =
        preferences.map { it[CUSTOM_GRADIENT_END] ?: 0xFF4A00E0.toInt() }
    val customColor: Flow<Int> = preferences.map { prefs ->
        prefs[CUSTOM_COLOR] ?: when (prefs[ACCENT]) {
            1 -> 0xFF2563EB.toInt()
            2 -> 0xFF059669.toInt()
            3 -> 0xFF7C3AED.toInt()
            4 -> 0xFFD97706.toInt()
            5 -> 0xFFE11D48.toInt()
            else -> 0xFF42A5F5.toInt()
        }
    }


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
    suspend fun setOnboardingSeen(seen: Boolean) = update { it[ONBOARDING_SEEN] = seen }
    suspend fun setCustomColor(color: Int) = update { it[CUSTOM_COLOR] = color }
    suspend fun setAccentStyle(style: AccentStyle) = update { it[ACCENT_STYLE] = style.name }
    suspend fun setAccentGradient(gradient: AccentGradient) =
        update { it[ACCENT_GRADIENT] = gradient.name }
    suspend fun setCustomGradientColors(start: Int, end: Int) = update {
        it[CUSTOM_GRADIENT_START] = start
        it[CUSTOM_GRADIENT_END] = end
    }
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
