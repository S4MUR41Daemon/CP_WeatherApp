package com.atmos.weather.domain

import androidx.compose.runtime.Immutable
import com.atmos.weather.data.repository.WeatherBundle
import kotlin.math.roundToInt

/*
 * Modelo de presentación ya calculado. Se construye una sola vez por refresco (fuera del hilo
 * principal) y las secciones reciben solo su trozo: Compose puede saltarse la recomposición de
 * todo lo que no cambia (antes cada pulsación recalculaba y redibujaba la pantalla entera).
 */

@Immutable
data class NowUi(
    val temp: Int,
    val code: String,
    val label: String,
    val hi: Int,
    val lo: Int,
    val feels: Int,
    val humidity: Int,
    val pressure: Int,
    val uv: Int,
    val uvLabel: String,
    val visKm: Int,
    val dew: Int,
    val precip: Double,
    val isDay: Boolean,
    val wmoCode: Int
)

@Immutable
data class HourUi(
    val label: String,
    val temp: Double,
    val prob: Int,
    val precip: Double,
    val wind: Int,
    val windDir: Float,
    val code: String
)

@Immutable
data class DayUi(
    val label: String,
    val weekday: String,
    val code: String,
    val desc: String,
    val tmin: Int,
    val tmax: Int,
    /** Posición 0..1 de mín/máx dentro del rango global de los 14 días. */
    val minF: Float,
    val maxF: Float,
    val prob: Int,
    val precip: Double,
    val uv: Int,
    val wind: Int,
    val gust: Int,
    val dir: String,
    val sunrise: String,
    val sunset: String
)

@Immutable
data class WindUi(
    val speed: Int,
    val dirDeg: Float,
    val dir: String,
    val gust: Int,
    val beaufort: Int,
    val beaufortName: String,
    val maxToday: Int
)

@Immutable
data class AirUi(val aqi: Int?, val level: Int, val levelName: String, val cells: List<Pair<String, String>>)

@Immutable
data class PollenRow(val name: String, val level: Int, val label: String)

@Immutable
data class SunMoonUi(
    val sunrise: String,
    val sunset: String,
    val daylight: String,
    /** 0..1 dentro del día solar, o null si es de noche. */
    val dayFrac: Float?,
    val moonName: String,
    val moonIllum: Int,
    val moonDaysToFull: Int,
    val moonPhase: Double
)

@Immutable
data class RadarFrame(val time: Long, val path: String)

@Immutable
data class RadarUi(val host: String, val frames: List<RadarFrame>)

@Immutable
data class WeatherUi(
    val now: NowUi,
    val hours: List<HourUi>,
    val days: List<DayUi>,
    val wind: WindUi,
    val air: AirUi,
    val pollen: List<PollenRow>,
    val sunMoon: SunMoonUi?,
    val radar: RadarUi?,
    val alerts: List<String>,
    val rainIntensity: Double
)

private val WEEKDAYS = arrayOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")

private fun weekday(isoDate: String): String = try {
    WEEKDAYS[java.time.LocalDate.parse(isoDate.take(10)).dayOfWeek.value - 1]
} catch (_: Throwable) { "" }

private fun <T> List<T?>.at(i: Int, def: T): T = getOrNull(i) ?: def

/** Índice de la hora actual dentro de la serie horaria (la serie de Open-Meteo empieza a las 00:00). */
fun currentHourIndex(times: List<String>, currentIso: String): Int {
    if (currentIso.length < 13) return 0
    val key = currentIso.substring(0, 13)
    val i = times.indexOfFirst { it.length >= 13 && it.substring(0, 13) >= key }
    return if (i < 0) 0 else i
}

/** Hora local actual de la ubicación (ISO, sin zona) para no mostrar horas ya pasadas con caché antigua. */
fun localNowIso(utcOffsetSec: Int, nowMs: Long = System.currentTimeMillis()): String =
    java.time.LocalDateTime.ofEpochSecond(nowMs / 1000, 0, java.time.ZoneOffset.ofTotalSeconds(utcOffsetSec)).toString()

fun WeatherBundle.toUi(nowMs: Long = System.currentTimeMillis()): WeatherUi? {
    val f = forecast ?: return null
    val cur = f.current ?: return null
    val d = f.daily
    val h = f.hourly
    val nowIso = maxOf(cur.time, localNowIso(f.utcOffset, nowMs))

    val w = wmo(cur.code)
    val hi = d?.tmax.orEmpty().at(0, cur.temperature).roundToInt()
    val lo = d?.tmin.orEmpty().at(0, cur.temperature).roundToInt()
    val uv = cur.uv ?: 0.0
    val now = NowUi(
        temp = cur.temperature.roundToInt(), code = w.code, label = w.label, hi = hi, lo = lo,
        feels = cur.apparent.roundToInt(), humidity = cur.humidity.roundToInt(),
        pressure = cur.pressure.roundToInt(), uv = uv.roundToInt(), uvLabel = uvLabel(uv),
        visKm = ((cur.visibility ?: 0.0) / 1000).roundToInt(), dew = cur.dew.roundToInt(),
        precip = cur.precipitation, isDay = cur.isDay == 1, wmoCode = cur.code
    )

    val hours = if (h == null) emptyList() else {
        val start = currentHourIndex(h.time, nowIso)
        val end = minOf(h.time.size, start + 48)
        (start until end).map { i ->
            HourUi(
                label = if (i == start) "YA" else h.time[i].substring(11, 13),
                temp = h.temperature.at(i, cur.temperature),
                prob = h.precipProb.at(i, 0.0).roundToInt(),
                precip = h.precipitation.at(i, 0.0),
                wind = h.windSpeed.at(i, 0.0).roundToInt(),
                windDir = h.windDir.at(i, 0.0).toFloat(),
                code = wmo(h.code.at(i, 0)).code
            )
        }
    }

    val days = if (d == null) emptyList() else {
        val n = d.time.size
        val mins = (0 until n).map { d.tmin.at(it, 0.0) }
        val maxs = (0 until n).map { d.tmax.at(it, 0.0) }
        val gmin = mins.minOrNull() ?: 0.0
        val span = maxOf(1.0, (maxs.maxOrNull() ?: 1.0) - gmin)
        (0 until n).map { i ->
            val day = d.time[i]
            val dw = wmo(d.code.at(i, 0))
            DayUi(
                label = if (i == 0) "HOY" else day.substring(8, 10) + "/" + day.substring(5, 7),
                weekday = if (i == 0) "HOY" else weekday(day),
                code = dw.code, desc = dw.label,
                tmin = mins[i].roundToInt(), tmax = maxs[i].roundToInt(),
                minF = ((mins[i] - gmin) / span).toFloat(), maxF = ((maxs[i] - gmin) / span).toFloat(),
                prob = d.precipMax.at(i, 0.0).roundToInt(), precip = d.precipSum.at(i, 0.0),
                uv = d.uvMax.at(i, 0.0).roundToInt(), wind = d.windMax.at(i, 0.0).roundToInt(),
                gust = d.gustMax.at(i, 0.0).roundToInt(), dir = dir(d.windDir.at(i, 0.0)),
                sunrise = d.sunrise.getOrNull(i)?.drop(11) ?: "--:--",
                sunset = d.sunset.getOrNull(i)?.drop(11) ?: "--:--"
            )
        }
    }

    val bf = beaufort(cur.windSpeed)
    val wind = WindUi(
        speed = cur.windSpeed.roundToInt(), dirDeg = cur.windDir.toFloat(), dir = dir(cur.windDir),
        gust = cur.gusts.roundToInt(), beaufort = bf, beaufortName = BEAUFORT_NAMES[bf],
        maxToday = d?.gustMax.orEmpty().at(0, cur.gusts).roundToInt()
    )

    val a = air?.current
    val lvl = aqiLevel(a?.aqi)
    val airUi = AirUi(
        aqi = a?.aqi?.roundToInt(), level = lvl, levelName = if (lvl < 0) "SIN DATOS" else AQI_NAMES[lvl],
        cells = listOf("PM2.5" to a?.pm25, "PM10" to a?.pm10, "O₃" to a?.ozone, "NO₂" to a?.no2)
            .map { (k, v) -> k to (v?.roundToInt()?.toString() ?: "—") }
    )

    val pollen = listOf(
        "GRAMÍNEAS" to a?.grass, "OLIVO" to a?.olive, "ABEDUL" to a?.birch,
        "ALISO" to a?.alder, "ARTEMISA" to a?.mugwort, "AMBROSÍA" to a?.ragweed
    ).map { (n, v) -> PollenRow(n, pollenIndex(v), pollenLevel(v)) }

    val sunMoon = if (d != null && d.sunrise.isNotEmpty() && d.sunset.isNotEmpty()) {
        val rise = toMin(d.sunrise[0])
        val set = toMin(d.sunset[0])
        val nowM = toMin(cur.time)
        val m = moon()
        SunMoonUi(
            sunrise = d.sunrise[0].drop(11), sunset = d.sunset[0].drop(11),
            daylight = hhmm(d.daylight.at(0, 0.0).toInt()),
            dayFrac = if (nowM in rise..set && set > rise) (nowM - rise).toFloat() / (set - rise) else null,
            moonName = m.name, moonIllum = m.illumination, moonDaysToFull = m.daysToFull, moonPhase = m.phase
        )
    } else null

    val rv = radar
    val radarUi = if (rv?.host != null && rv.radar != null && rv.radar.past.isNotEmpty())
        RadarUi(rv.host, rv.radar.past.takeLast(6).map { RadarFrame(it.time, it.path) }) else null

    val next24 = h?.let { hh ->
        val s = currentHourIndex(hh.time, nowIso)
        hh.code.drop(s).take(24).map { it ?: 0 }
    }.orEmpty()
    val alerts = computeAlerts(
        next24,
        d?.gustMax.orEmpty().at(0, 0.0),
        d?.uvMax.orEmpty().at(0, 0.0),
        d?.tmax.orEmpty().at(0, 0.0),
        d?.tmin.orEmpty().at(0, 0.0),
        d?.precipSum.orEmpty().at(0, 0.0)
    )

    return WeatherUi(
        now = now, hours = hours, days = days, wind = wind, air = airUi, pollen = pollen,
        sunMoon = sunMoon, radar = radarUi, alerts = alerts,
        rainIntensity = rainIntensity(cur.code, cur.precipitation)
    )
}
