package com.atmos.weather.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class Wmo(val label: String, val code: String)

fun wmo(c: Int): Wmo = when {
    c == 0 -> Wmo("DESPEJADO", "CLR")
    c <= 2 -> Wmo("POCO NUBOSO", "PCL")
    c == 3 -> Wmo("CUBIERTO", "OVC")
    c <= 48 -> Wmo("NIEBLA", "FOG")
    c <= 57 -> Wmo("LLOVIZNA", "DZL")
    c <= 67 -> Wmo("LLUVIA", "RAIN")
    c <= 77 -> Wmo("NIEVE", "SNOW")
    c <= 82 -> Wmo("CHUBASCOS", "SHWR")
    c <= 86 -> Wmo("NIEVE", "SNOW")
    else -> Wmo("TORMENTA", "TSTM")
}

private val DIRS = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
fun dir(d: Double): String {
    val norm = ((d % 360) + 360) % 360
    return DIRS[((norm / 45).roundToInt()) % 8]
}

val BEAUFORT_NAMES = listOf(
    "CALMA", "VENTOLINA", "FLOJITO", "FLOJO", "BONANCIBLE", "FRESQUITO",
    "FRESCO", "FRESCACHÓN", "TEMPORAL", "TEMPORAL FUERTE", "TEMPORAL DURO",
    "T. MUY DURO", "HURACÁN"
)

private val BEAUFORT_LIMITS = intArrayOf(1, 6, 12, 20, 29, 39, 50, 62, 75, 89, 103, 118)
fun beaufort(ws: Double): Int {
    val idx = BEAUFORT_LIMITS.indexOfFirst { ws < it }
    return if (idx < 0) 12 else idx
}

fun aqiLevel(aqi: Double?): Int {
    if (aqi == null) return -1
    return min(5, (aqi / 20).toInt())
}
val AQI_NAMES = listOf("BUENO", "RAZONABLE", "MODERADO", "MALO", "MUY MALO", "EXTREMO")

fun pollenLevel(v: Double?): String = when {
    v == null -> "N/D"
    v < 1 -> "NULO"
    v < 20 -> "BAJO"
    v < 50 -> "MEDIO"
    v < 150 -> "ALTO"
    else -> "MUY ALTO"
}

/** Nivel de polen 0..4 (NULO..MUY ALTO), -1 si no hay dato. */
fun pollenIndex(v: Double?): Int = when {
    v == null -> -1
    v < 1 -> 0
    v < 20 -> 1
    v < 50 -> 2
    v < 150 -> 3
    else -> 4
}

fun pollenWidthPct(v: Double?): Int =
    if (v == null) 0 else min(100.0, v / 150.0 * 100.0).toInt()

fun uvLabel(uv: Double): String = when {
    uv < 3 -> "BAJO"
    uv < 6 -> "MOD."
    uv < 8 -> "ALTO"
    uv < 11 -> "M.ALTO"
    else -> "EXTR."
}

data class MoonInfo(val name: String, val illumination: Int, val daysToFull: Int, val shadowOffset: Int, val phase: Double = 0.0)

fun moon(nowMs: Long = System.currentTimeMillis()): MoonInfo {
    val syn = 29.530588853
    val base = java.util.GregorianCalendar(2000, 0, 6, 18, 14).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }.timeInMillis
    val days = (nowMs - base) / 86_400_000.0
    val mp = (((days % syn) + syn) % syn) / syn
    val illum = (1 - cos(2 * PI * mp)) / 2
    val name = when {
        mp < 0.03 || mp >= 0.97 -> "LUNA NUEVA"
        mp < 0.22 -> "CRECIENTE"
        mp < 0.28 -> "CUARTO CRECIENTE"
        mp < 0.47 -> "GIBOSA CRECIENTE"
        mp < 0.53 -> "LUNA LLENA"
        mp < 0.72 -> "GIBOSA MENGUANTE"
        mp < 0.78 -> "CUARTO MENGUANTE"
        else -> "MENGUANTE"
    }
    val toFull = (((0.5 - mp + 1) % 1) * syn).roundToInt()
    val shadow = ((1 - illum) * 56 * (if (mp < 0.5) 1 else -1)).roundToInt()
    return MoonInfo(name, (illum * 100).roundToInt(), toFull, shadow, mp)
}

data class SunArc(val x: Double, val y: Double)
/** Posición normalizada 0..1 del sol, trazada sobre un bezier cuadrático de (14,92)-(150,-40)-(286,92). */
fun sunArc(nowMinutes: Int, riseMinutes: Int, setMinutes: Int): SunArc {
    val span = max(1, setMinutes - riseMinutes)
    val f = max(0.0, min(1.0, (nowMinutes - riseMinutes).toDouble() / span))
    val x = (1 - f) * (1 - f) * 14 + 2 * (1 - f) * f * 150 + f * f * 286
    val y = (1 - f) * (1 - f) * 92 + 2 * (1 - f) * f * -40 + f * f * 92
    return SunArc(x, y)
}

fun toMin(iso: String): Int =
    if (iso.length < 16) 0 else iso.substring(11, 13).toInt() * 60 + iso.substring(14, 16).toInt()

fun hhmm(seconds: Int): String {
    val h = seconds / 3600
    val m = ((seconds % 3600) / 60.0).roundToInt()
    return "${h}h ${m.toString().padStart(2, '0')}m"
}

/** Avisos locales según los umbrales de `renderVals` del prototipo. */
fun computeAlerts(
    next24hCodes: List<Int>,
    gustMaxKmh: Double,
    uvMax: Double,
    tMax: Double,
    tMin: Double,
    precipSumMm: Double
): List<String> {
    val list = mutableListOf<String>()
    if (next24hCodes.any { it >= 95 }) list += "TORMENTA PREVISTA · PRÓX. 24 H"
    if (gustMaxKmh >= 60) list += "RACHAS FUERTES · ${gustMaxKmh.roundToInt()} KM/H"
    if (uvMax >= 8) list += "ÍNDICE UV MUY ALTO · ${uvMax.roundToInt()}"
    if (tMax >= 38) list += "CALOR EXTREMO · ${tMax.roundToInt()}°"
    if (tMin <= -2) list += "HELADA · ${tMin.roundToInt()}°"
    if (precipSumMm >= 20) list += "LLUVIA INTENSA · ${precipSumMm.roundToInt()} MM"
    return list
}

/** Intensidad de lluvia (0..1) a partir del código y mm de precipitación. */
fun rainIntensity(code: Int, precipMm: Double): Double {
    val isWet = (code in 51..67) || (code in 80..82) || code >= 95
    return if (isWet) min(1.0, 0.4 + precipMm / 4.0) else 0.0
}

fun tileX(lon: Double, z: Int): Double {
    val n = (1 shl z).toDouble()
    return (lon + 180.0) / 360.0 * n
}
fun tileY(lat: Double, z: Int): Double {
    val n = (1 shl z).toDouble()
    val latr = lat * PI / 180.0
    return (1 - kotlin.math.ln(kotlin.math.tan(latr) + 1 / kotlin.math.cos(latr)) / PI) / 2 * n
}
