package com.example.voicerecorder.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val apiKey: Flow<String> = dataStore.data.map { prefs ->
        prefs[API_KEY] ?: ""
    }

    suspend fun saveApiKey(key: String) {
        dataStore.edit { it[API_KEY] = key }
    }

    companion object {
        private val API_KEY = stringPreferencesKey("gladia_api_key")
    }
}
