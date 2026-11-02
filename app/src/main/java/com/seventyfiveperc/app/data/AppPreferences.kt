package com.seventyfiveperc.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_ROLL_NUMBER = stringPreferencesKey("roll_number")
        private val KEY_EXTRA_KEYWORDS = stringPreferencesKey("extra_keywords")
        private val KEY_USE_OFFLINE = booleanPreferencesKey("use_offline")
        private val KEY_PROFILE_SET = booleanPreferencesKey("profile_set")
        private val KEY_SENSITIVITY = stringPreferencesKey("sensitivity") // low, medium, high
        private val KEY_ACADEMIC_CALENDAR = stringPreferencesKey("academic_calendar")
    }

    val userName: Flow<String> = context.dataStore.data.map { it[KEY_USER_NAME] ?: "" }
    val rollNumber: Flow<String> = context.dataStore.data.map { it[KEY_ROLL_NUMBER] ?: "" }
    val extraKeywords: Flow<String> = context.dataStore.data.map { it[KEY_EXTRA_KEYWORDS] ?: "" }
    val useOfflineMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_USE_OFFLINE] ?: true }
    val isProfileSet: Flow<Boolean> = context.dataStore.data.map { it[KEY_PROFILE_SET] ?: false }
    val sensitivity: Flow<String> = context.dataStore.data.map { it[KEY_SENSITIVITY] ?: "medium" }
    val academicCalendar: Flow<String> = context.dataStore.data.map { it[KEY_ACADEMIC_CALENDAR] ?: "" }

    suspend fun saveAcademicCalendar(text: String) {
        context.dataStore.edit { it[KEY_ACADEMIC_CALENDAR] = text }
    }

    suspend fun saveProfile(
        name: String,
        rollNumber: String,
        extraKeywords: String,
        useOffline: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = name
            prefs[KEY_ROLL_NUMBER] = rollNumber
            prefs[KEY_EXTRA_KEYWORDS] = extraKeywords
            prefs[KEY_USE_OFFLINE] = useOffline
            prefs[KEY_PROFILE_SET] = true
        }
    }

    suspend fun setSensitivity(level: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SENSITIVITY] = level
        }
    }

    suspend fun clearProfile() {
        context.dataStore.edit { it.clear() }
    }
}
