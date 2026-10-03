package com.atmos.weather.ui.sections

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.atmos.weather.domain.*
import com.atmos.weather.ui.components.BracketPanel
import com.atmos.weather.ui.components.CodeChip
import com.atmos.weather.ui.components.SectionHeader
import com.atmos.weather.ui.components.SegBar
import com.atmos.weather.ui.theme.AtmosColors
import com.atmos.weather.ui.theme.AtmosType
import com.atmos.weather.ui.viewmodel.UiState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun SectionNow(s: UiState) {
    val cur = s.bundle?.forecast?.current
    val daily = s.bundle?.forecast?.daily
    BracketPanel {
        SectionHeader("01", "AHORA")
        if (cur == null || daily == null) {
            Text("// ENLAZANDO CON SATÉLITE", style = AtmosType.mono.copy(color = AtmosColors.Muted))
            return@BracketPanel
        }
        val w = wmo(cur.code)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${cur.temperature.roundToInt()}", style = AtmosType.tempBig.copy(color = AtmosColors.Text), fontSize = 96.sp)
            Text("°C", style = AtmosType.title.copy(color = AtmosColors.Accent), modifier = Modifier.padding(start = 4.dp, bottom = 24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.padding(bottom = 20.dp)) {
                CodeChip(w.code)
                Spacer(Modifier.height(4.dp))
                Text(w.label, style = AtmosType.title.copy(color = AtmosColors.Text))
                Text("↑${daily.tmax[0].roundToInt()}° ↓${daily.tmin[0].roundToInt()}°", style = AtmosType.mono.copy(color = AtmosColors.Muted))
            }
        }
        Spacer(Modifier.height(10.dp))
        val stats = listOf(
            "SENSACIÓN" to "${cur.apparent.roundToInt()}°",
            "HUMEDAD" to "${cur.humidity.roundToInt()}%",
            "PRESIÓN" to "${cur.pressure.roundToInt()} hPa",
            "ÍNDICE UV" to "${(cur.uv ?: 0.0).roundToInt()} · ${uvLabel(cur.uv ?: 0.0)}",
            "VISIBILIDAD" to "${((cur.visibility ?: 0.0) / 1000).roundToInt()} km",
            "P. ROCÍO" to "${cur.dew.roundToInt()}°"
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            stats.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (k, v) ->
                        Column(Modifier.weight(1f).border(1.dp, AtmosColors.Line).padding(8.dp)) {
                            Text(k, style = AtmosType.label.copy(color = AtmosColors.Muted))
                            Text(v, style = AtmosType.numMedium.copy(color = AtmosColors.Text))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHours(s: UiState, onRange: (Int) -> Unit) {
    val h = s.bundle?.forecast?.hourly
    BracketPanel {
        SectionHeader("02", "PRÓXIMAS HORAS", trailing = "${s.range}H")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(12, 24, 48).forEach { v ->
                val active = v == s.range
                Box(
                    Modifier
                        .background(if (active) AtmosColors.Accent else Color.Transparent)
                        .border(1.dp, AtmosColors.Line)
                        .clickable { onRange(v) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("${v}H", style = AtmosType.label.copy(color = if (active) AtmosColors.Bg else AtmosColors.Text))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (h == null) {
            Text("—", style = AtmosType.mono.copy(color = AtmosColors.Muted))
            return@BracketPanel
        }
        val n = min(s.range, h.temperature.size)
        val temps = h.temperature.take(n)
        val probs = h.precipProb.take(n).map { it ?: 0.0 }
        val mn = (temps.min() - 1)
        val mx = (temps.max() + 1)
        val scroll = rememberScrollState()
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .horizontalScroll(scroll)
        ) {
            val widthDp = (n * 26).dp
            Canvas(Modifier.width(widthDp).fillMaxHeight()) {
                val pw = size.width / n
                val top = 20f
                val bottom = size.height - 20f
                val range = (mx - mn).coerceAtLeast(0.1)
                fun yFor(t: Double) = (bottom - ((t - mn) / range) * (bottom - top)).toFloat()
                // probability bars
                probs.forEachIndexed { i, p ->
                    val barH = (p / 100.0 * 40).toFloat()
                    drawRect(
                        color = AtmosColors.Accent2.copy(alpha = 0.55f),
                        topLeft = Offset(i * pw + pw * 0.3f, bottom - barH),
                        size = androidx.compose.ui.geometry.Size(pw * 0.4f, barH)
                    )
                }
                // temperature line
                val path = Path()
                temps.forEachIndexed { i, t ->
                    val x = i * pw + pw / 2
                    val y = yFor(t)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = AtmosColors.Accent, style = Stroke(width = 2.dp.toPx()))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            h.time.take(n).forEachIndexed { i, t ->
                Column(
                    Modifier.width(44.dp).border(1.dp, AtmosColors.Line).padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(if (i == 0) "YA" else t.substring(11, 13), style = AtmosType.label.copy(color = AtmosColors.Muted))
                    Text("${probs[i].roundToInt()}%", style = AtmosType.mono.copy(color = AtmosColors.Accent2))
                    Text("${h.windSpeed[i].roundToInt()}", style = AtmosType.mono.copy(color = AtmosColors.Text))
                }
            }
        }
    }
}

@Composable
fun SectionDays(s: UiState, onToggle: (Int) -> Unit) {
    val d = s.bundle?.forecast?.daily ?: return
    BracketPanel {
        SectionHeader("03", "PRÓXIMOS 14 DÍAS")
        val gmin = d.tmin.min()
        val gmax = d.tmax.max()
        val span = max(1.0, gmax - gmin)
        d.time.forEachIndexed { i, day ->
            val w = wmo(d.code[i])
            Column(Modifier.fillMaxWidth().clickable { onToggle(i) }.padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (i == 0) "HOY" else day.substring(8, 10) + "/" + day.substring(5, 7),
                        style = AtmosType.title.copy(color = AtmosColors.Text), modifier = Modifier.width(60.dp))
                    CodeChip(w.code)
                    Spacer(Modifier.width(8.dp))
                    Text("${d.tmin[i].roundToInt()}°", style = AtmosType.mono.copy(color = AtmosColors.Accent2), modifier = Modifier.width(32.dp))
                    Box(
                        Modifier.weight(1f).height(8.dp).background(AtmosColors.Line.copy(alpha = 0.3f))
                    ) {
                        val startFrac = ((d.tmin[i] - gmin) / span).toFloat()
                        val widthFrac = max(0.04f, ((d.tmax[i] - d.tmin[i]) / span).toFloat())
                        Box(Modifier.fillMaxHeight().fillMaxWidth(widthFrac)
                            .offset(x = (startFrac * 200).dp)
                            .background(AtmosColors.Accent))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("${d.tmax[i].roundToInt()}°", style = AtmosType.mono.copy(color = AtmosColors.Accent))
                    Spacer(Modifier.width(8.dp))
                    Text("${(d.precipMax.getOrNull(i) ?: 0.0).roundToInt()}%", style = AtmosType.mono.copy(color = AtmosColors.Muted), modifier = Modifier.width(40.dp))
                }
                if (s.openDay == i) {
                    Column(Modifier.padding(start = 60.dp, top = 4.dp)) {
                        Text(w.label, style = AtmosType.body.copy(color = AtmosColors.Text))
                        Text("LLUVIA ${d.precipSum[i]} MM · UV ${(d.uvMax.getOrNull(i) ?: 0.0).roundToInt()}", style = AtmosType.label.copy(color = AtmosColors.Muted))
                        Text("VIENTO ${d.windMax[i].roundToInt()} KM/H · RACHAS ${d.gustMax[i].roundToInt()} ${dir(d.windDir[i])}", style = AtmosType.label.copy(color = AtmosColors.Muted))
                        Text("☀ ${d.sunrise[i].substring(11)} · ☾ ${d.sunset[i].substring(11)}", style = AtmosType.label.copy(color = AtmosColors.Muted))
                    }
                }
            }
        }
    }
}

@Composable
fun SectionWind(s: UiState) {
    val cur = s.bundle?.forecast?.current ?: return
    BracketPanel {
        SectionHeader("04", "VIENTO")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(140.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val r = min(cx, cy)
                // ticks
                for (i in 0 until 36) {
                    val a = (i * 10) * PI / 180
                    val len = if (i % 9 == 0) 10f else 5f
                    val x1 = cx + ((r - len) * sin(a)).toFloat()
                    val y1 = cy - ((r - len) * cos(a)).toFloat()
                    val x2 = cx + (r * sin(a)).toFloat()
                    val y2 = cy - (r * cos(a)).toFloat()
                    drawLine(if (i == 0) AtmosColors.Accent else AtmosColors.Line, Offset(x1, y1), Offset(x2, y2), 1.dp.toPx())
                }
                // needle (direction wind is going)
                rotate(cur.windDir.toFloat() + 180f, Offset(cx, cy)) {
                    drawLine(AtmosColors.Accent, Offset(cx, cy), Offset(cx, cy - r * 0.9f), 2.dp.toPx())
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${cur.windSpeed.roundToInt()} KM/H", style = AtmosType.numMedium.copy(color = AtmosColors.Text))
                Text("DIRECCIÓN · ${dir(cur.windDir)}", style = AtmosType.label.copy(color = AtmosColors.Muted))
                Text("RACHAS · ${cur.gusts.roundToInt()}", style = AtmosType.label.copy(color = AtmosColors.Warn))
                val bf = beaufort(cur.windSpeed)
                Text("$bf · ${BEAUFORT_NAMES[bf]}", style = AtmosType.title.copy(color = AtmosColors.Accent))
                SegBar(12, bf, modifier = Modifier.width(120.dp))
            }
        }
    }
}

@Composable
fun SectionAir(s: UiState) {
    val a = s.bundle?.air?.current
    BracketPanel {
        SectionHeader("05", "CALIDAD DEL AIRE")
        val aqi = a?.aqi
        val lvl = aqiLevel(aqi)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(aqi?.roundToInt()?.toString() ?: "N/D", style = AtmosType.tempBig.copy(color = AtmosColors.Accent2), fontSize = 54.sp)
            Spacer(Modifier.width(8.dp))
            Text(if (lvl < 0) "SIN DATOS" else AQI_NAMES[lvl], style = AtmosType.title.copy(color = AtmosColors.Text), modifier = Modifier.padding(bottom = 12.dp))
        }
        SegBar(6, max(0, lvl + 1), AtmosColors.Accent2)
        Spacer(Modifier.height(10.dp))
        val cells = listOf(
            "PM2.5" to a?.pm25,
            "PM10" to a?.pm10,
            "O₃" to a?.ozone,
            "NO₂" to a?.no2
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            cells.forEach { (k, v) ->
                Column(Modifier.weight(1f).border(1.dp, AtmosColors.Line).padding(6.dp)) {
                    Text(k, style = AtmosType.label.copy(color = AtmosColors.Muted))
                    Text(v?.roundToInt()?.toString() ?: "—", style = AtmosType.numMedium.copy(color = AtmosColors.Text))
                }
            }
        }
    }
}

@Composable
fun SectionPollen(s: UiState) {
    val a = s.bundle?.air?.current
    BracketPanel {
        SectionHeader("06", "POLEN")
        val rows = listOf(
            "Gramíneas" to a?.grass,
            "Olivo" to a?.olive,
            "Abedul" to a?.birch,
            "Aliso" to a?.alder,
            "Artemisa" to a?.mugwort,
            "Ambrosía" to a?.ragweed
        )
        rows.forEach { (name, v) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(name.uppercase(), style = AtmosType.label.copy(color = AtmosColors.Text), modifier = Modifier.width(90.dp))
                Box(Modifier.weight(1f).height(6.dp).background(AtmosColors.Line.copy(alpha = 0.3f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(pollenWidthPct(v) / 100f).background(AtmosColors.Accent))
                }
                Spacer(Modifier.width(8.dp))
                Text(pollenLevel(v), style = AtmosType.label.copy(color = AtmosColors.Accent2), modifier = Modifier.width(72.dp))
            }
        }
    }
}

@Composable
fun SectionSunMoon(s: UiState) {
    val d = s.bundle?.forecast?.daily ?: return
    val cur = s.bundle.forecast.current ?: return
    BracketPanel {
        SectionHeader("07", "SOL Y LUNA")
        val nowM = toMin(cur.time)
        val rise = toMin(d.sunrise[0])
        val set = toMin(d.sunset[0])
        val arc = sunArc(nowM, rise, set)
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.05f, h * 0.9f)
                quadraticBezierTo(w / 2, -h * 0.3f, w * 0.95f, h * 0.9f)
            }
            drawPath(path, AtmosColors.Line, style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
            val px = (arc.x / 300.0 * w).toFloat()
            val py = (arc.y / 100.0 * h * 0.9).toFloat()
            drawCircle(AtmosColors.Warn, radius = 8.dp.toPx(), center = Offset(px.coerceIn(10f, w - 10f), py.coerceIn(10f, h - 10f)))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("AMANECE", style = AtmosType.label.copy(color = AtmosColors.Muted)); Text(d.sunrise[0].substring(11), style = AtmosType.mono.copy(color = AtmosColors.Text)) }
            Column { Text("LUZ", style = AtmosType.label.copy(color = AtmosColors.Muted)); Text(hhmm((d.daylight.firstOrNull() ?: 0.0).toInt()), style = AtmosType.mono.copy(color = AtmosColors.Text)) }
            Column { Text("OCASO", style = AtmosType.label.copy(color = AtmosColors.Muted)); Text(d.sunset[0].substring(11), style = AtmosType.mono.copy(color = AtmosColors.Text)) }
        }
        Spacer(Modifier.height(10.dp))
        val m = moon()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(48.dp)) {
                val c = Offset(size.width / 2, size.height / 2)
                drawCircle(AtmosColors.Text, radius = size.minDimension / 2, center = c)
                drawCircle(AtmosColors.Panel, radius = size.minDimension / 2, center = Offset(c.x + m.shadowOffset / 56f * (size.minDimension / 2), c.y))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(m.name, style = AtmosType.title.copy(color = AtmosColors.Accent))
                Text("${m.illumination}% · ${m.daysToFull}d A LLENA", style = AtmosType.label.copy(color = AtmosColors.Muted))
            }
        }
    }
}

@Composable
fun SectionRadar(s: UiState) {
    val radar = s.bundle?.radar?.radar?.past?.lastOrNull()
    val host = s.bundle?.radar?.host
    val loc = s.location
    BracketPanel {
        SectionHeader("08", "RADAR")
        if (radar == null || host == null || loc == null) {
            Text("SIN DATOS DE RADAR", style = AtmosType.label.copy(color = AtmosColors.Muted))
            return@BracketPanel
        }
        val z = 6
        val xt = tileX(loc.lon, z)
        val yt = tileY(loc.lat, z)
        val tx = xt.toInt()
        val ty = yt.toInt()
        Box(Modifier.fillMaxWidth().height(210.dp).border(1.dp, AtmosColors.Line)) {
            for (i in -1..1) for (k in -1..1) {
                val leftFrac = (0.5 + (tx + i - xt) * (256.0 / 308.0 / 3)).toFloat()
                val topFrac = (0.5 + (ty + k - yt) * (256.0 / 210.0 / 3)).toFloat()
                AsyncImage(
                    model = "https://a.basemaps.cartocdn.com/dark_all/$z/${tx + i}/${ty + k}.png",
                    contentDescription = null,
                    modifier = Modifier.offset(x = (leftFrac * 100).dp, y = (topFrac * 100).dp).size(100.dp)
                )
            }
        }
        Text("ATRIBUCIÓN: RainViewer · © OpenStreetMap · © CARTO", style = AtmosType.label.copy(color = AtmosColors.Muted))
    }
}
