package com.atmos.weather.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.atmos.weather.domain.RadarUi
import com.atmos.weather.domain.tileX
import com.atmos.weather.domain.tileY
import com.atmos.weather.ui.components.CyberButton
import com.atmos.weather.ui.components.SegBar
import com.atmos.weather.ui.theme.AtmosColors
import com.atmos.weather.ui.theme.AtmosType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.floor

private const val ZOOM = 6
private val TILE: Dp = 150.dp
private val MAP_H: Dp = 250.dp

/**
 * Teselas OSM estándar (sin API key; CARTO ahora la exige) convertidas a un mapa nocturno con una
 * matriz de color: luminancia invertida y teñida con el color secundario de la plantilla activa.
 * Se aplica en GPU al dibujar.
 */
private fun nightMapFilter(tint: Color): ColorFilter {
    fun row(c: Float, bias: Float): FloatArray {
        val k = 0.18f + 0.5f * c
        return floatArrayOf(-0.299f * k, -0.587f * k, -0.114f * k, 0f, 255f * k + bias)
    }
    return ColorFilter.colorMatrix(
        ColorMatrix(row(tint.red, 6f) + row(tint.green, 4f) + row(tint.blue, 8f) + floatArrayOf(0f, 0f, 0f, 1f, 0f))
    )
}

private fun baseUrl(x: Int, y: Int) = "https://tile.openstreetmap.org/$ZOOM/$x/$y.png"
private fun radarUrl(host: String, path: String, x: Int, y: Int) = "$host$path/256/$ZOOM/$x/$y/2/1_1.png"

@Composable
fun RadarMap(radar: RadarUi, lat: Double, lon: Double) {
    val frames = radar.frames
    var idx by remember(radar) { mutableIntStateOf(frames.lastIndex) }
    var playing by remember(radar) { mutableStateOf(false) }
    val ctx = LocalContext.current

    val tint = AtmosColors.Secondary
    val mapFilter = remember(tint) { nightMapFilter(tint) }
    val xt = remember(lon) { tileX(lon, ZOOM) }
    val yt = remember(lat) { tileY(lat, ZOOM) }

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(MAP_H)
            .clipToBounds()
            .background(AtmosColors.Bg)
    ) {
        val w = maxWidth
        val tilesX = remember(w, xt) { tileRange(xt, w / TILE) }
        val tilesY = remember(yt) { tileRange(yt, MAP_H / TILE) }
        val n = 1 shl ZOOM

        // Prefetch de todos los frames al pulsar «play» para que la animación no parpadee.
        LaunchedEffect(playing) {
            if (!playing) return@LaunchedEffect
            val loader = ctx.imageLoader
            for (f in frames) for (x in tilesX) for (y in tilesY) {
                if (y !in 0 until n) continue
                loader.enqueue(ImageRequest.Builder(ctx).data(radarUrl(radar.host, f.path, Math.floorMod(x, n), y)).build())
            }
            while (true) {
                delay(650)
                idx = (idx + 1) % frames.size
            }
        }

        for (x in tilesX) for (y in tilesY) {
            if (y !in 0 until n) continue
            val wx = Math.floorMod(x, n)
            val ox = w / 2 + TILE * (x - xt).toFloat()
            val oy = MAP_H / 2 + TILE * (y - yt).toFloat()
            val tileMod = Modifier.offset(ox, oy).size(TILE)
            AsyncImage(
                model = baseUrl(wx, y), contentDescription = null, modifier = tileMod,
                contentScale = ContentScale.FillBounds, colorFilter = mapFilter
            )
            AsyncImage(
                model = radarUrl(radar.host, frames[idx].path, wx, y), contentDescription = null, modifier = tileMod,
                contentScale = ContentScale.FillBounds, alpha = 0.85f
            )
        }
        RadarHud(Modifier.matchParentSize())
        Text(
            fmtTime(frames[idx].time),
            style = AtmosType.monoBig.copy(color = AtmosColors.Primary),
            modifier = Modifier.align(Alignment.TopEnd).background(AtmosColors.Bg.copy(alpha = 0.75f)).padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        CyberButton(if (playing) "❚❚ PAUSA" else "▶ ANIMAR", color = AtmosColors.Primary, filled = !playing) { playing = !playing }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            SegBar(frames.size, idx + 1, AtmosColors.Data, height = 8.dp)
            Spacer(Modifier.height(4.dp))
            Text("−${(frames.size - 1 - idx) * 10} MIN", style = AtmosType.label.copy(color = AtmosColors.Muted))
        }
    }
}

private fun tileRange(center: Double, spanTiles: Float): IntRange {
    val half = spanTiles / 2.0
    return floor(center - half).toInt()..floor(center + half).toInt()
}

private fun fmtTime(epochSec: Long): String =
    SimpleDateFormat("HH:mm", Locale("es")).format(Date(epochSec * 1000))

/** Retícula, punto de ubicación y esquinas tipo visor. */
@Composable
private fun RadarHud(modifier: Modifier) {
    Spacer(
        modifier.drawWithCache {
            val c = Offset(size.width / 2, size.height / 2)
            val arm = 14.dp.toPx()
            val sw = 2.dp.toPx()
            val ring = Stroke(1.5.dp.toPx())
            onDrawBehind {
                drawCircle(AtmosColors.Secondary.copy(alpha = 0.25f), 26.dp.toPx(), c, style = ring)
                drawCircle(AtmosColors.Secondary, 5.dp.toPx(), c)
                drawLine(AtmosColors.Secondary, Offset(c.x - 40.dp.toPx(), c.y), Offset(c.x - 12.dp.toPx(), c.y), 1.dp.toPx())
                drawLine(AtmosColors.Secondary, Offset(c.x + 12.dp.toPx(), c.y), Offset(c.x + 40.dp.toPx(), c.y), 1.dp.toPx())
                drawLine(AtmosColors.Secondary, Offset(c.x, c.y - 40.dp.toPx()), Offset(c.x, c.y - 12.dp.toPx()), 1.dp.toPx())
                drawLine(AtmosColors.Secondary, Offset(c.x, c.y + 12.dp.toPx()), Offset(c.x, c.y + 40.dp.toPx()), 1.dp.toPx())
                val w = size.width; val h = size.height
                val y = AtmosColors.Primary
                drawRect(y, Offset(0f, 0f), Size(arm, sw)); drawRect(y, Offset(0f, 0f), Size(sw, arm))
                drawRect(y, Offset(w - arm, h - sw), Size(arm, sw)); drawRect(y, Offset(w - sw, h - arm), Size(sw, arm))
                drawRect(AtmosColors.LineSecondary, Offset.Zero, size, style = Stroke(1.dp.toPx()))
            }
        }
    )
}
