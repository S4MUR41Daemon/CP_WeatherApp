package com.atmos.weather.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

class OpenMeteoClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val http: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 10_000
        }
    }

    suspend fun fetchAll(lat: Double, lon: Double): Triple<ForecastResponse?, AirResponse?, RainViewerResponse?> =
        coroutineScope {
            val q = "latitude=$lat&longitude=$lon&timezone=auto"
            val forecastUrl = "https://api.open-meteo.com/v1/forecast?$q&forecast_days=14" +
                "&current=temperature_2m,apparent_temperature,relative_humidity_2m,pressure_msl,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index,visibility,dew_point_2m,precipitation" +
                "&hourly=temperature_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m,wind_direction_10m" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant,sunrise,sunset,uv_index_max,daylight_duration"
            val airUrl = "https://air-quality-api.open-meteo.com/v1/air-quality?$q" +
                "&current=european_aqi,pm2_5,pm10,ozone,nitrogen_dioxide,alder_pollen,birch_pollen,grass_pollen,olive_pollen,mugwort_pollen,ragweed_pollen"
            val radarUrl = "https://api.rainviewer.com/public/weather-maps.json"
            val f = async { safeGet<ForecastResponse>(forecastUrl) }
            val a = async { safeGet<AirResponse>(airUrl) }
            val r = async { safeGet<RainViewerResponse>(radarUrl) }
            Triple(f.await(), a.await(), r.await())
        }

    suspend fun search(query: String): List<GeoResult> {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${java.net.URLEncoder.encode(query, "UTF-8")}&count=6&language=es"
        return safeGet<GeoResponse>(url)?.results.orEmpty()
    }

    private suspend inline fun <reified T> safeGet(url: String): T? = try {
        val r: HttpResponse = http.get(url)
        if (r.status.isSuccess()) r.body<T>() else null
    } catch (_: Throwable) { null }
}
