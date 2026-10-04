package com.atmos.weather.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

const val USER_AGENT = "ATMOS-Weather/2.0 (Android; github.com/S4MUR41Daemon/CP_WeatherApp)"

/** Un único OkHttpClient para la app: datos JSON y teselas del mapa (Coil) comparten pool y caché. */
fun buildHttpClient(cacheDir: java.io.File): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .callTimeout(15, TimeUnit.SECONDS)
    .cache(okhttp3.Cache(java.io.File(cacheDir, "http"), 24L * 1024 * 1024))
    // OSM bloquea los User-Agent genéricos: identificamos la app en todas las peticiones.
    .addInterceptor { chain ->
        chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
    }
    .build()

class OpenMeteoClient(private val http: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    suspend fun fetchAll(lat: Double, lon: Double): Triple<ForecastResponse?, AirResponse?, RainViewerResponse?> =
        coroutineScope {
            val q = "latitude=$lat&longitude=$lon&timezone=auto"
            val forecastUrl = "https://api.open-meteo.com/v1/forecast?$q&forecast_days=14&forecast_hours=72" +
                "&current=temperature_2m,apparent_temperature,relative_humidity_2m,pressure_msl,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index,visibility,dew_point_2m,precipitation,is_day" +
                "&hourly=temperature_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m,wind_direction_10m" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant,sunrise,sunset,uv_index_max,daylight_duration"
            val airUrl = "https://air-quality-api.open-meteo.com/v1/air-quality?$q" +
                "&current=european_aqi,pm2_5,pm10,ozone,nitrogen_dioxide,alder_pollen,birch_pollen,grass_pollen,olive_pollen,mugwort_pollen,ragweed_pollen"
            val radarUrl = "https://api.rainviewer.com/public/weather-maps.json"
            val f = async { get<ForecastResponse>(forecastUrl) }
            val a = async { get<AirResponse>(airUrl) }
            val r = async { get<RainViewerResponse>(radarUrl) }
            Triple(f.await(), a.await(), r.await())
        }

    suspend fun search(query: String): List<GeoResult> {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(query.trim(), "UTF-8")}&count=6&language=es"
        return get<GeoResponse>(url)?.results.orEmpty()
    }

    private suspend inline fun <reified T> get(url: String): T? = withContext(Dispatchers.IO) {
        try {
            http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful) null else r.body?.string()?.let { json.decodeFromString<T>(it) }
            }
        } catch (_: Throwable) { null }
    }
}
