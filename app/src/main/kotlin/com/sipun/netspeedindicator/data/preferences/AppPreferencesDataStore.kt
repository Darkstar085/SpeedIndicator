package com.sipun.netspeedindicator.data.preferences

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.SharedPreferencesMigration

private const val PREFERENCES_FILE_NAME = "app_preferences"

val Context.appPreferencesDataStore by preferencesDataStore(
    name = PREFERENCES_FILE_NAME,
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, PREFERENCES_FILE_NAME))
    }
)
