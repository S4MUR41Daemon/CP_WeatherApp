package com.atmos.weather.ui.components

import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
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
import androidx.compose.runtime.withFrameNanos
import kotlin.random.Random

private const val GLYPHS = "0123456789°%+-<>/:"

private class Drop(var x: Float, var y: Float, var n: Int, val chars: CharArray)

@OptIn(ExperimentalTextApi::class)
@Composable
fun DigitalRain(
    rainIntensity: Double,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val ctx = LocalContext.current
    val powerSave = remember(ctx) {
        val pm = ctx.getSystemService(android.content.Context.POWER_SERVICE) as? PowerManager
        pm?.isPowerSaveMode == true
    }
    val animScale = remember(ctx) {
        try { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE) } catch (_: Throwable) { 1f }
    }
    val active = enabled && !powerSave && animScale > 0f

    val measurer = rememberTextMeasurer()
    val style = TextStyle(fontFamily = ShareTechMono, fontSize = 11.sp)
    val density = LocalDensity.current
    val colStepPx = with(density) { 13.dp.toPx() }
    val glyphH = with(density) { 12.dp.toPx() }

    val drops = remember { mutableListOf<Drop>() }
    var tick by remember { mutableIntStateOf(0) }
    var size by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    LaunchedEffect(active, rainIntensity, lifecycle) {
        if (!active) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (isActive) {
                withFrameNanos { t ->
                    if (t - last < 50_000_000L) return@withFrameNanos
                    last = t
                    val target = if (rainIntensity > 0) (18 + 22 * rainIntensity).toInt() else 9
                    val speed = if (rainIntensity > 0) (4f + 3f * rainIntensity).toFloat() else 0.6f
                    val w = size.width
                    val h = size.height
                    if (w <= 0 || h <= 0) return@withFrameNanos
                    val maxCol = (w / colStepPx).toInt().coerceAtLeast(1)
                    while (drops.size < target) {
                        drops += Drop(
                            x = Random.nextInt(maxCol) * colStepPx + 3f,
                            y = -Random.nextFloat() * 80f,
                            n = 3 + Random.nextInt(6),
                            chars = CharArray(9) { GLYPHS[Random.nextInt(GLYPHS.length)] }
                        )
                    }
                    while (drops.size > target) drops.removeAt(drops.lastIndex)
                    for (d in drops) {
                        d.y += speed * (0.6f + d.n / 10f)
                        if (Random.nextFloat() < 0.06f) {
                            d.chars[Random.nextInt(9)] = GLYPHS[Random.nextInt(GLYPHS.length)]
                        }
                        if (d.y - d.n * glyphH > h) {
                            d.x = Random.nextInt(maxCol) * colStepPx + 3f
                            d.y = -Random.nextFloat() * 80f
                            d.n = 3 + Random.nextInt(6)
                        }
                    }
                    tick++
                }
            }
        }
    }

    Canvas(modifier) {
        size = this.size
        if (!active) return@Canvas
        tick.hashCode() // read tick to trigger recomposition
        val alpha = if (rainIntensity > 0) 1f else 0.6f
        for (d in drops) {
            for (k in 0 until d.n) {
                val a = alpha * if (k == 0) 0.6f else 0.3f * (1f - k / d.n.toFloat())
                val color = if (k == 0) AtmosColors.Accent2 else AtmosColors.Accent
                drawTextSafe(measurer, d.chars[k].toString(), style.copy(color = color.copy(alpha = a)), Offset(d.x, d.y - k * glyphH))
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawTextSafe(m: TextMeasurer, s: String, style: TextStyle, offset: Offset) {
    val r = m.measure(s, style)
    drawText(r, topLeft = offset)
}
