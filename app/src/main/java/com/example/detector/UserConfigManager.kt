package com.example.detector

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("user_config")

class UserConfigManager(private val context: Context) {

    companion object {
        private val CONFIGURED_KEY = booleanPreferencesKey("is_configured")
    }

    suspend fun markAsConfigured() {
        context.dataStore.edit { prefs ->
            prefs[CONFIGURED_KEY] = true
        }
    }

    suspend fun isConfigured(): Boolean {
        return context.dataStore.data
            .map { prefs -> prefs[CONFIGURED_KEY] ?: false }
            .first()
    }

    suspend fun resetConfig() {
        context.dataStore.edit { prefs ->
            prefs[CONFIGURED_KEY] = false
        }
    }
}
