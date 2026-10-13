package com.atmos.weather.share

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.atmos.weather.AtmosApp
import com.atmos.weather.domain.toUi
import com.atmos.weather.ui.theme.AtmosColors
import com.atmos.weather.ui.theme.Palettes
import com.atmos.weather.widget.WidgetUpdater
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Puente con el launcher ATMOS//OS. Solo responde a apps firmadas con la misma clave
 * (permiso `signature`), así que ninguna otra app puede leer la ubicación ni cambiar el tema.
 *
 * Métodos de [call]:
 *  - `getWeather` → `json` (ver [SharedWeather]) y `ts` (último sync, ms).
 *  - `getTheme`   → `theme` (id de plantilla).
 *  - `setTheme`   → arg = id; cambia la plantilla de ATMOS y repinta su widget.
 *  - `requestRefresh` → si la caché tiene más de 30 min, encola un sync en segundo plano.
 *
 * Tras cada sync o cambio de tema se hace notifyChange([URI]) para que el launcher se refresque
 * sin sondear.
 */
class AtmosDataProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val ctx = context ?: return null
        if (ctx.checkCallingOrSelfPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Requiere $PERMISSION")
        }
        val app = ctx.applicationContext as AtmosApp
        return when (method) {
            "getWeather" -> runBlocking {
                val cached = app.container.repository.loadCached() ?: return@runBlocking Bundle()
                val ui = cached.bundle.toUi() ?: return@runBlocking Bundle()
                val loc = cached.location
                val shared = SharedWeather(
                    city = loc.name, region = loc.region, gps = loc.gps, lat = loc.lat, lon = loc.lon,
                    temp = ui.now.temp, code = ui.now.code, label = ui.now.label,
                    hi = ui.now.hi, lo = ui.now.lo, isDay = ui.now.isDay,
                    feels = ui.now.feels, humidity = ui.now.humidity, pressure = ui.now.pressure,
                    uv = ui.now.uv, uvLabel = ui.now.uvLabel, visKm = ui.now.visKm, dew = ui.now.dew,
                    hours = ui.hours.take(24).map { SharedHour(it.label, it.temp, it.prob) },
                    days = ui.days.take(7).map { SharedDay(it.weekday, it.code, it.tmin, it.tmax, it.prob) },
                    alerts = ui.alerts,
                    fetchedAt = cached.bundle.fetchedAt
                )
                Bundle().apply {
                    putString("json", json.encodeToString(shared))
                    putLong("ts", cached.bundle.fetchedAt)
                }
            }
            "requestRefresh" -> {
                // El launcher lo llama al encenderse la pantalla de inicio: solo si la caché tiene > 30 min.
                val ts = runBlocking { app.container.repository.loadCached()?.bundle?.fetchedAt } ?: 0L
                val stale = System.currentTimeMillis() - ts > 30 * 60 * 1000L
                if (stale) com.atmos.weather.work.RefreshScheduler.refreshNow(ctx)
                Bundle().apply { putBoolean("queued", stale) }
            }
            "getTheme" -> Bundle().apply {
                putString("theme", runBlocking { app.container.prefs.themeId.first() } ?: Palettes.NIGHT_CITY.id)
            }
            "setTheme" -> {
                val p = Palettes.byId(arg)
                AtmosColors.palette = p
                runBlocking {
                    app.container.prefs.setThemeId(p.id)
                    WidgetUpdater.update(ctx)
                }
                Bundle().apply { putString("theme", p.id) }
            }
            else -> null
        }
    }

    // Sin tablas: toda la API va por call().
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?): Int = 0

    companion object {
        const val AUTHORITY = "com.atmos.weather.data"
        const val PERMISSION = "com.atmos.weather.permission.SHARE"
        val URI: Uri = Uri.parse("content://$AUTHORITY")
        private val json = Json { encodeDefaults = true }

        fun notifyChanged(ctx: Context) {
            try { ctx.contentResolver.notifyChange(URI, null) } catch (_: Throwable) {}
        }
    }
}

@Serializable
data class SharedHour(val h: String, val t: Double, val p: Int)

@Serializable
data class SharedDay(val d: String, val c: String, val lo: Int, val hi: Int, val p: Int)

@Serializable
data class SharedWeather(
    val city: String,
    val region: String,
    val gps: Boolean,
    val lat: Double,
    val lon: Double,
    val temp: Int,
    val code: String,
    val label: String,
    val hi: Int,
    val lo: Int,
    val isDay: Boolean,
    val feels: Int,
    val humidity: Int,
    val pressure: Int,
    val uv: Int,
    val uvLabel: String,
    val visKm: Int,
    val dew: Int,
    val hours: List<SharedHour>,
    val days: List<SharedDay>,
    val alerts: List<String>,
    val fetchedAt: Long
)
