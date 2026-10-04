package com.atmos.weather.data.cache

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.prefsStore by preferencesDataStore("atmos_prefs")

@Serializable
data class SavedCity(
    val name: String,
    val lat: Double,
    val lon: Double,
    val region: String = "",
    val gps: Boolean = false,
    val manual: Boolean = true
)

class PrefsStore(private val context: Context) {
    private val CITIES_KEY = stringPreferencesKey("cities")
    private val RAIN_KEY = booleanPreferencesKey("rain_enabled")
    private val UNITS_C_KEY = booleanPreferencesKey("units_celsius")
    private val WIDGET_FREQ_KEY = intPreferencesKey("widget_freq_min")
    private val THEME_KEY = stringPreferencesKey("theme")
    private val json = Json { ignoreUnknownKeys = true }

    val rainEnabled: Flow<Boolean> = context.prefsStore.data.map { it[RAIN_KEY] ?: true }
    val unitsCelsius: Flow<Boolean> = context.prefsStore.data.map { it[UNITS_C_KEY] ?: true }
    val widgetFreqMin: Flow<Int> = context.prefsStore.data.map { it[WIDGET_FREQ_KEY] ?: 60 }
    val themeId: Flow<String?> = context.prefsStore.data.map { it[THEME_KEY] }
    val savedCities: Flow<List<SavedCity>> = context.prefsStore.data.map {
        val s = it[CITIES_KEY] ?: return@map emptyList()
        try { json.decodeFromString<List<SavedCity>>(s) } catch (_: Throwable) { emptyList() }
    }

    suspend fun setRainEnabled(v: Boolean) { context.prefsStore.edit { it[RAIN_KEY] = v } }
    suspend fun setUnitsCelsius(v: Boolean) { context.prefsStore.edit { it[UNITS_C_KEY] = v } }
    suspend fun setWidgetFreq(v: Int) { context.prefsStore.edit { it[WIDGET_FREQ_KEY] = v } }
    suspend fun setThemeId(id: String) { context.prefsStore.edit { it[THEME_KEY] = id } }
    suspend fun setSavedCities(list: List<SavedCity>) {
        context.prefsStore.edit { it[CITIES_KEY] = json.encodeToString(list) }
    }
}
