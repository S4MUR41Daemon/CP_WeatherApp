package com.atmos.weather.data.repository

import com.atmos.weather.data.api.AirResponse
import com.atmos.weather.data.api.ForecastResponse
import com.atmos.weather.data.api.OpenMeteoClient
import com.atmos.weather.data.api.RainViewerResponse
import com.atmos.weather.data.cache.SavedCity
import com.atmos.weather.data.cache.WeatherCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }

    suspend fun loadCached(): CachedBundle? {
        val e = cache.read() ?: return null
        return withContext(Dispatchers.Default) {
            try {
                CachedBundle(json.decodeFromString<WeatherBundle>(e.json), json.decodeFromString<SavedCity>(e.locJson))
            } catch (_: Throwable) { null }
        }
    }

    /**
     * Pide datos nuevos. Si la red falla y había caché para la misma ubicación, devuelve la caché
     * (así la UI nunca se queda vacía por un corte de red) y no la sobrescribe.
     */
    suspend fun refresh(loc: SavedCity): WeatherBundle {
        val (f, a, r) = client.fetchAll(loc.lat, loc.lon)
        if (f?.current == null) {
            val old = loadCached()
            if (old != null && old.location.sameAs(loc)) return old.bundle
            return WeatherBundle(f, a, r, 0L, fromMock = true)
        }
        val bundle = WeatherBundle(f, a, r, System.currentTimeMillis())
        try {
            val (bj, lj) = withContext(Dispatchers.Default) { json.encodeToString(bundle) to json.encodeToString(loc) }
            cache.write(bj, lj)
        } catch (_: Throwable) {}
        return bundle
    }
}

fun SavedCity.sameAs(o: SavedCity): Boolean =
    kotlin.math.abs(lat - o.lat) < 0.01 && kotlin.math.abs(lon - o.lon) < 0.01
