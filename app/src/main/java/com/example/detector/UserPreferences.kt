package com.example.detector

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val EMAIL_KEY = stringPreferencesKey("email")
        val USERS_KEY = stringPreferencesKey("users")
    }

    val email: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[EMAIL_KEY]
    }

    suspend fun saveEmail(email: String) {
        context.dataStore.edit { prefs ->
            prefs[EMAIL_KEY] = email
        }
    }

    suspend fun clearEmail() {
        context.dataStore.edit { prefs ->
            prefs.remove(EMAIL_KEY)
        }
    }

    suspend fun registerUser(email: String, password: String) {
        val cleanEmail = email.trim()
        val cleanPass = password.trim()

        context.dataStore.edit { prefs ->
            val current = prefs[USERS_KEY] ?: ""
            val updated = current + "$cleanEmail:$cleanPass;"
            prefs[USERS_KEY] = updated
            prefs[EMAIL_KEY] = cleanEmail
        }
    }


    suspend fun validateUser(email: String, password: String): Boolean {
        val cleanEmail = email.trim()
        val cleanPass = password.trim()

        val stored = context.dataStore.data.map { it[USERS_KEY] ?: "" }.first()

        return stored
            .split(";")
            .filter { it.isNotBlank() }
            .any { it.trim() == "$cleanEmail:$cleanPass" }
    }

}
