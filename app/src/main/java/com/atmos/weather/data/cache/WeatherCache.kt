package com.atmos.weather.data.cache

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.weatherStore by preferencesDataStore("atmos_weather")

class WeatherCache(private val context: Context) {
    private val JSON_KEY = stringPreferencesKey("json")
    private val TS_KEY = longPreferencesKey("ts")
    private val LOC_KEY = stringPreferencesKey("loc")

    data class Entry(val json: String, val timestamp: Long, val locJson: String)

    suspend fun read(): Entry? {
        val p = context.weatherStore.data.first()
        val j = p[JSON_KEY] ?: return null
        val ts = p[TS_KEY] ?: 0L
        val lj = p[LOC_KEY] ?: "{}"
        return Entry(j, ts, lj)
    }

    suspend fun write(json: String, locJson: String) {
        context.weatherStore.edit {
            it[JSON_KEY] = json
            it[TS_KEY] = System.currentTimeMillis()
            it[LOC_KEY] = locJson
        }
    }
}
