package com.atmos.weather.data.repository

import com.atmos.weather.data.api.AirResponse
import com.atmos.weather.data.api.ForecastResponse
import com.atmos.weather.data.api.OpenMeteoClient
import com.atmos.weather.data.api.RainViewerResponse
import com.atmos.weather.data.cache.SavedCity
import com.atmos.weather.data.cache.WeatherCache
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WeatherBundle(
    val forecast: ForecastResponse? = null,
    val air: AirResponse? = null,
    val radar: RainViewerResponse? = null,
    val fetchedAt: Long = 0L,
    val fromMock: Boolean = false
)

data class CachedBundle(val bundle: WeatherBundle, val location: SavedCity)

class WeatherRepository(
    private val client: OpenMeteoClient,
    private val cache: WeatherCache
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun loadCached(): CachedBundle? {
        val e = cache.read() ?: return null
        return try {
            val b = json.decodeFromString<WeatherBundle>(e.json)
            val l = json.decodeFromString<SavedCity>(e.locJson)
            CachedBundle(b, l)
        } catch (_: Throwable) { null }
    }

    suspend fun refresh(loc: SavedCity): WeatherBundle {
        val (f, a, r) = client.fetchAll(loc.lat, loc.lon)
        val bundle = WeatherBundle(f, a, r, System.currentTimeMillis(), fromMock = f?.current == null)
        try {
            cache.write(json.encodeToString(bundle), json.encodeToString(loc))
        } catch (_: Throwable) {}
        return bundle
    }
}
