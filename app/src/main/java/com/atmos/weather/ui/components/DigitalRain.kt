package com.atmos.weather.ui.components

import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.atmos.weather.ui.theme.AtmosColors
import com.atmos.weather.ui.theme.ShareTechMono
import kotlinx.coroutines.isActive
import kotlin.random.Random

private const val GLYPHS = "0123456789°%+-<>/:"
private const val TRAIL = 9

private class Drop(var x: Float, var y: Float, var n: Int, val chars: IntArray)

private class RainSim {
    val drops = ArrayList<Drop>(48)
    var w = 0f
    var h = 0f
}

/**
 * Lluvia de datos a 20 fps.
 *
 * Optimizaciones respecto a la versión anterior:
 *  - Los 18 glifos se miden UNA vez (antes: TextMeasurer.measure por glifo y por frame, ~300/frame).
 *  - El contador de frame solo se lee en la fase de dibujo → invalida el Canvas, no recompone nada.
 *  - El tamaño se guarda en un objeto plano (antes era un State escrito desde draw → bucle de recomposición).
 *  - Se pausa fuera de RESUMED, con ahorro de batería o con animaciones del sistema desactivadas.
 */
@Composable
fun DigitalRain(
    rainIntensity: Double,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val ctx = LocalContext.current
    val active = remember(ctx, enabled) {
        val pm = ctx.getSystemService(android.content.Context.POWER_SERVICE) as? PowerManager
        val scale = try {
            Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE)
        } catch (_: Throwable) { 1f }
        enabled && pm?.isPowerSaveMode != true && scale > 0f
    }
    if (!active) return

    val measurer = rememberTextMeasurer(cacheSize = 0)
    val glyphs = remember(measurer) {
        val style = TextStyle(fontFamily = ShareTechMono, fontSize = 11.sp)
        Array(GLYPHS.length) { measurer.measure(GLYPHS[it].toString(), style) }
    }
    val density = LocalDensity.current
    val colStep = with(density) { 14.dp.toPx() }
    val glyphH = with(density) { 12.dp.toPx() }

    val sim = remember { RainSim() }
    var frame by remember { mutableLongStateOf(0L) }

    LaunchedEffect(rainIntensity, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (isActive) {
                withFrameNanos { t ->
                    if (t - last >= 50_000_000L) {
                        last = t
                        step(sim, rainIntensity, colStep, glyphH)
                        frame++
                    }
                }
            }
        }
    }

    val wet = rainIntensity > 0
    val head = if (wet) AtmosColors.Cyan else AtmosColors.Yellow
    val tail = if (wet) AtmosColors.Cyan else AtmosColors.Red
    val base = if (wet) 0.9f else 0.55f

    Canvas(modifier) {
        sim.w = size.width
        sim.h = size.height
        if (frame < 0L) return@Canvas // lectura en fase de dibujo → solo redibuja
        for (d in sim.drops) {
            for (k in 0 until d.n) {
                val y = d.y - k * glyphH
                if (y < -glyphH || y > size.height) continue
                val a = base * if (k == 0) 0.85f else 0.45f * (1f - k / d.n.toFloat())
                drawText(glyphs[d.chars[k]], color = if (k == 0) head else tail, topLeft = Offset(d.x, y), alpha = a)
            }
        }
    }
}

private fun step(sim: RainSim, intensity: Double, colStep: Float, glyphH: Float) {
    val w = sim.w
    val h = sim.h
    if (w <= 0f || h <= 0f) return
    val target = if (intensity > 0) (14 + 18 * intensity).toInt() else 8
    val speed = if (intensity > 0) (4f + 3f * intensity.toFloat()) else 0.9f
    val maxCol = (w / colStep).toInt().coerceAtLeast(1)
    val drops = sim.drops
    while (drops.size < target) {
        drops += Drop(
            x = Random.nextInt(maxCol) * colStep + 3f,
            y = Random.nextFloat() * h,
            n = 3 + Random.nextInt(6),
            chars = IntArray(TRAIL) { Random.nextInt(GLYPHS.length) }
        )
    }
    while (drops.size > target) drops.removeAt(drops.lastIndex)
    for (d in drops) {
        d.y += speed * (0.6f + d.n / 10f)
        if (Random.nextFloat() < 0.06f) d.chars[Random.nextInt(TRAIL)] = Random.nextInt(GLYPHS.length)
        if (d.y - d.n * glyphH > h) {
            d.x = Random.nextInt(maxCol) * colStep + 3f
            d.y = -Random.nextFloat() * 80f
            d.n = 3 + Random.nextInt(6)
        }
    }
}
