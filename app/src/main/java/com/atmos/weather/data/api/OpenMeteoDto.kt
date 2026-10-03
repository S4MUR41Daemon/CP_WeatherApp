package com.atmos.weather.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForecastResponse(
    val current: Current? = null,
    val hourly: Hourly? = null,
    val daily: Daily? = null
)

@Serializable
data class Current(
    val time: String,
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("apparent_temperature") val apparent: Double,
    @SerialName("relative_humidity_2m") val humidity: Double,
    @SerialName("pressure_msl") val pressure: Double,
    @SerialName("weather_code") val code: Int,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("wind_direction_10m") val windDir: Double,
    @SerialName("wind_gusts_10m") val gusts: Double,
    @SerialName("uv_index") val uv: Double? = null,
    val visibility: Double? = null,
    @SerialName("dew_point_2m") val dew: Double,
    val precipitation: Double = 0.0
)

@Serializable
data class Hourly(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature: List<Double>,
    @SerialName("precipitation_probability") val precipProb: List<Double?> = emptyList(),
    val precipitation: List<Double> = emptyList(),
    @SerialName("weather_code") val code: List<Int>,
    @SerialName("wind_speed_10m") val windSpeed: List<Double>,
    @SerialName("wind_direction_10m") val windDir: List<Double>
)

@Serializable
data class Daily(
    val time: List<String>,
    @SerialName("weather_code") val code: List<Int>,
    @SerialName("temperature_2m_max") val tmax: List<Double>,
    @SerialName("temperature_2m_min") val tmin: List<Double>,
    @SerialName("precipitation_sum") val precipSum: List<Double>,
    @SerialName("precipitation_probability_max") val precipMax: List<Double?> = emptyList(),
    @SerialName("wind_speed_10m_max") val windMax: List<Double>,
    @SerialName("wind_gusts_10m_max") val gustMax: List<Double>,
    @SerialName("wind_direction_10m_dominant") val windDir: List<Double>,
    val sunrise: List<String>,
    val sunset: List<String>,
    @SerialName("uv_index_max") val uvMax: List<Double?> = emptyList(),
    @SerialName("daylight_duration") val daylight: List<Double>
)

@Serializable
data class AirResponse(val current: AirCurrent? = null)

@Serializable
data class AirCurrent(
    @SerialName("european_aqi") val aqi: Double? = null,
    @SerialName("pm2_5") val pm25: Double? = null,
    val pm10: Double? = null,
    val ozone: Double? = null,
    @SerialName("nitrogen_dioxide") val no2: Double? = null,
    @SerialName("alder_pollen") val alder: Double? = null,
    @SerialName("birch_pollen") val birch: Double? = null,
    @SerialName("grass_pollen") val grass: Double? = null,
    @SerialName("olive_pollen") val olive: Double? = null,
    @SerialName("mugwort_pollen") val mugwort: Double? = null,
    @SerialName("ragweed_pollen") val ragweed: Double? = null
)

@Serializable
data class RainViewerResponse(val host: String? = null, val radar: RvRadar? = null)
@Serializable
data class RvRadar(val past: List<RvFrame> = emptyList())
@Serializable
data class RvFrame(val time: Long, val path: String)

@Serializable
data class GeoResponse(val results: List<GeoResult> = emptyList())
@Serializable
data class GeoResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val admin1: String? = null,
    val country: String? = null
)
