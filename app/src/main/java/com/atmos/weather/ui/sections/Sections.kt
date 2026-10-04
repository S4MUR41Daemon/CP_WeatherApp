package com.atmos.weather.ui.sections

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atmos.weather.domain.*
import com.atmos.weather.ui.components.*
import com.atmos.weather.ui.theme.AtmosColors
import com.atmos.weather.ui.theme.AtmosType
import com.atmos.weather.ui.theme.ChromaShift
import com.atmos.weather.ui.theme.ShareTechMono
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ───────────────────────────── 01 AHORA ─────────────────────────────

@Composable
fun SectionNow(now: NowUi) {
    NeonPanel("01", "AHORA", color = AtmosColors.Primary, trailing = if (now.isDay) "DÍA" else "NOCHE") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    "${now.temp}",
                    style = AtmosType.tempBig.copy(color = AtmosColors.Text, shadow = ChromaShift),
                    maxLines = 1
                )
                Text("°C", style = AtmosType.title.copy(color = AtmosColors.Primary), modifier = Modifier.padding(top = 18.dp, start = 2.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                CodeChip(now.code)
                Spacer(Modifier.height(6.dp))
                Text(
                    now.label, style = AtmosType.title.copy(color = AtmosColors.Text, textAlign = TextAlign.End),
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    Text("▲${now.hi}°", style = AtmosType.monoBig.copy(color = AtmosColors.SecondarySoft))
                    Spacer(Modifier.width(8.dp))
                    Text("▼${now.lo}°", style = AtmosType.monoBig.copy(color = AtmosColors.Data))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        val stats = listOf(
            "SENSACIÓN" to "${now.feels}°",
            "HUMEDAD" to "${now.humidity}%",
            "PRESIÓN" to "${now.pressure} hPa",
            "ÍNDICE UV" to "${now.uv} · ${now.uvLabel}",
            "VISIBILIDAD" to "${now.visKm} km",
            "P. ROCÍO" to "${now.dew}°"
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            stats.chunked(3).forEachIndexed { r, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEachIndexed { c, (k, v) ->
                        StatCell(k, v, Modifier.weight(1f), accent = if ((r + c) % 2 == 0) AtmosColors.Primary else AtmosColors.Secondary)
                    }
                }
            }
        }
    }
}

// ───────────────────────────── 02 HORAS ─────────────────────────────

@Composable
private fun RangeChips(range: Int, onRange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(12, 24, 48).forEach { v ->
            val on = v == range
            val shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp)
            Box(
                Modifier
                    .clip(shape)
                    .background(if (on) AtmosColors.Primary else Color.Transparent)
                    .border(1.dp, if (on) AtmosColors.Primary else AtmosColors.LineSecondary, shape)
                    .clickable { onRange(v) }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text("${v}H", style = AtmosType.label.copy(color = if (on) AtmosColors.Bg else AtmosColors.Text))
            }
        }
    }
}

@Composable
fun SectionHours(hours: List<HourUi>, range: Int, onRange: (Int) -> Unit) {
    NeonPanel("02", "PRÓXIMAS HORAS", trailing = "${range}H") {
        RangeChips(range, onRange)
        Spacer(Modifier.height(12.dp))
        if (hours.isEmpty()) {
            Text("—", style = AtmosType.mono.copy(color = AtmosColors.Muted)); return@NeonPanel
        }
        val data = remember(hours, range) { hours.take(range) }
        HourlyChart(data)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Legend(AtmosColors.Primary, "TEMP °C")
            Legend(AtmosColors.Data, "% LLUVIA")
            Legend(AtmosColors.Muted, "VIENTO KM/H")
        }
    }
}

@Composable
private fun Legend(c: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(c))
        Spacer(Modifier.width(5.dp))
        Text(label, style = AtmosType.label.copy(color = AtmosColors.Muted))
    }
}

/**
 * Toda la gráfica (línea, relleno, barras de lluvia y las cuatro filas de etiquetas) se dibuja en un
 * único nodo. Los textos se miden en drawWithCache: una vez por cambio de datos, no por frame.
 */
@Composable
private fun HourlyChart(data: List<HourUi>) {
    val measurer = rememberTextMeasurer()
    val colW = 46.dp
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxWidth().horizontalScroll(scroll)) {
        Spacer(
            Modifier
                .width(colW * data.size)
                .height(196.dp)
                .drawWithCache {
                    val n = data.size
                    val pw = size.width / n
                    val dp = density
                    val lineTop = 26f * dp
                    val lineBottom = 92f * dp
                    val barsBottom = 140f * dp
                    val barsMax = 34f * dp
                    val mn = data.minOf { it.temp } - 0.5
                    val mx = data.maxOf { it.temp } + 0.5
                    val rng = (mx - mn).coerceAtLeast(1.0)
                    fun yFor(t: Double) = (lineBottom - ((t - mn) / rng) * (lineBottom - lineTop)).toFloat()
                    val pts = data.mapIndexed { i, h -> Offset(i * pw + pw / 2, yFor(h.temp)) }

                    val line = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
                    val area = Path().apply {
                        addPath(line)
                        lineTo(pts.last().x, lineBottom + 6f * dp)
                        lineTo(pts.first().x, lineBottom + 6f * dp)
                        close()
                    }
                    val areaBrush = Brush.verticalGradient(
                        listOf(AtmosColors.Primary.copy(alpha = 0.28f), Color.Transparent),
                        startY = lineTop, endY = lineBottom + 6f * dp
                    )
                    val sTemp = TextStyle(fontFamily = ShareTechMono, fontSize = 11.sp, color = AtmosColors.Text)
                    val sProb = TextStyle(fontFamily = ShareTechMono, fontSize = 10.sp, color = AtmosColors.Data)
                    val sProbOff = sProb.copy(color = AtmosColors.Dim)
                    val sWind = TextStyle(fontFamily = ShareTechMono, fontSize = 10.sp, color = AtmosColors.Muted)
                    val sHour = TextStyle(fontFamily = ShareTechMono, fontSize = 11.sp, color = AtmosColors.Muted)
                    val sNow = sHour.copy(color = AtmosColors.Primary)
                    val tTemp = data.map { measurer.measure("${Math.round(it.temp)}°", sTemp) }
                    val tProb = data.map { measurer.measure("${it.prob}%", if (it.prob > 0) sProb else sProbOff) }
                    val tWind = data.map { measurer.measure("${it.wind}", sWind) }
                    val tHour = data.mapIndexed { i, h -> measurer.measure(h.label, if (i == 0) sNow else sHour) }
                    val glow = Stroke(width = 6f * dp, cap = StrokeCap.Round)
                    val core = Stroke(width = 2f * dp, cap = StrokeCap.Round)
                    val dash = PathEffect.dashPathEffect(floatArrayOf(4f * dp, 4f * dp))

                    onDrawBehind {
                        // Separadores de medianoche en rojo.
                        data.forEachIndexed { i, h ->
                            if (h.label == "00" && i > 0) {
                                val x = i * pw
                                drawLine(AtmosColors.LineSecondary, Offset(x, 0f), Offset(x, size.height), 1f * dp, pathEffect = dash)
                            }
                        }
                        drawPath(area, areaBrush)
                        drawPath(line, AtmosColors.Primary.copy(alpha = 0.22f), style = glow)
                        drawPath(line, AtmosColors.Primary, style = core)
                        pts.forEachIndexed { i, p ->
                            val c = if (i == 0) AtmosColors.Secondary else AtmosColors.Primary
                            val r = if (i == 0) 4.5f * dp else 2.5f * dp
                            drawRect(c, Offset(p.x - r, p.y - r), Size(r * 2, r * 2))
                            val t = tTemp[i]
                            drawText(t, topLeft = Offset(p.x - t.size.width / 2f, p.y - t.size.height - 6f * dp))
                        }
                        data.forEachIndexed { i, h ->
                            val cx = i * pw + pw / 2
                            val bh = (h.prob / 100f) * barsMax
                            drawRect(AtmosColors.Data.copy(alpha = 0.12f), Offset(cx - pw * 0.22f, barsBottom - barsMax), Size(pw * 0.44f, barsMax))
                            if (bh > 0f) drawRect(AtmosColors.Data.copy(alpha = 0.75f), Offset(cx - pw * 0.22f, barsBottom - bh), Size(pw * 0.44f, bh))
                            val tp = tProb[i]
                            drawText(tp, topLeft = Offset(cx - tp.size.width / 2f, barsBottom + 3f * dp))
                            val tw = tWind[i]
                            drawText(tw, topLeft = Offset(cx - tw.size.width / 2f, barsBottom + 19f * dp))
                            val th = tHour[i]
                            drawText(th, topLeft = Offset(cx - th.size.width / 2f, barsBottom + 36f * dp))
                        }
                    }
                }
        )
    }
}

// ───────────────────────────── 03 14 DÍAS ─────────────────────────────

private val TEMP_GRADIENT get() = listOf(AtmosColors.Data, AtmosColors.Primary, AtmosColors.Secondary)

@Composable
fun SectionDays(days: List<DayUi>, openDay: Int, onToggle: (Int) -> Unit) {
    if (days.isEmpty()) return
    NeonPanel("03", "PRÓXIMOS ${days.size} DÍAS") {
        days.forEachIndexed { i, d ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(AtmosColors.LinePrimary.copy(alpha = 0.12f)))
            DayRow(d, open = openDay == i, onClick = { onToggle(i) })
        }
    }
}

@Composable
private fun DayRow(d: DayUi, open: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(44.dp)) {
                Text(d.weekday, style = AtmosType.title.copy(color = if (d.label == "HOY") AtmosColors.Primary else AtmosColors.Text, fontSize = 14.sp))
                if (d.label != "HOY") Text(d.label, style = AtmosType.label.copy(color = AtmosColors.Muted))
            }
            Box(Modifier.width(54.dp)) { CodeChip(d.code) }
            Text(
                "${d.tmin}°", style = AtmosType.mono.copy(color = AtmosColors.Data, textAlign = TextAlign.End),
                modifier = Modifier.width(30.dp)
            )
            Canvas(Modifier.weight(1f).height(8.dp).padding(horizontal = 8.dp)) {
                val w = size.width
                drawRect(AtmosColors.Track, size = size)
                val brush = Brush.horizontalGradient(TEMP_GRADIENT, startX = 0f, endX = w)
                val x0 = d.minF * w
                val x1 = maxOf(d.maxF * w, x0 + 4.dp.toPx())
                drawRect(brush, Offset(x0, 0f), Size(x1 - x0, size.height))
            }
            Text(
                "${d.tmax}°", style = AtmosType.mono.copy(color = AtmosColors.SecondarySoft),
                modifier = Modifier.width(30.dp)
            )
            Text(
                "${d.prob}%",
                style = AtmosType.mono.copy(color = if (d.prob >= 30) AtmosColors.Data else AtmosColors.Muted, textAlign = TextAlign.End),
                modifier = Modifier.width(38.dp)
            )
        }
        if (open) {
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(AtmosColors.PanelHi)
                    .drawBehind { drawRect(AtmosColors.Secondary, Offset.Zero, Size(2.dp.toPx(), size.height)) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(d.desc, style = AtmosType.title.copy(color = AtmosColors.Primary))
                Text("LLUVIA ${"%.1f".format(d.precip)} MM · PROB ${d.prob}% · UV ${d.uv}", style = AtmosType.label.copy(color = AtmosColors.Text))
                Text("VIENTO ${d.wind} KM/H ${d.dir} · RACHAS ${d.gust}", style = AtmosType.label.copy(color = AtmosColors.Text))
                Text("AMANECE ${d.sunrise} · OCASO ${d.sunset}", style = AtmosType.label.copy(color = AtmosColors.Muted))
            }
        }
    }
}

// ───────────────────────────── 04 VIENTO ─────────────────────────────

@Composable
fun SectionWind(wind: WindUi) {
    NeonPanel("04", "VIENTO", trailing = "BEAUFORT ${wind.beaufort}") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Compass(wind, Modifier.size(150.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${wind.speed}", style = AtmosType.numLarge.copy(color = AtmosColors.Text, shadow = ChromaShift))
                    Spacer(Modifier.width(4.dp))
                    Text("KM/H", style = AtmosType.label.copy(color = AtmosColors.Primary), modifier = Modifier.padding(bottom = 4.dp))
                }
                Text("DIRECCIÓN · ${wind.dir} ${wind.dirDeg.toInt()}°", style = AtmosType.label.copy(color = AtmosColors.Muted))
                Text("RACHAS · ${wind.gust} KM/H", style = AtmosType.label.copy(color = AtmosColors.Secondary))
                Text("MÁX. HOY · ${wind.maxToday} KM/H", style = AtmosType.label.copy(color = AtmosColors.Muted))
                Text(wind.beaufortName, style = AtmosType.title.copy(color = AtmosColors.Primary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                SegBar(12, wind.beaufort.coerceAtLeast(1), AtmosColors.Primary, segmentColors = BEAUFORT_COLORS)
            }
        }
    }
}

private val BEAUFORT_COLORS get() = List(12) { i ->
    when {
        i < 4 -> AtmosColors.Data
        i < 8 -> AtmosColors.Primary
        else -> AtmosColors.Secondary
    }
}

@Composable
private fun Compass(wind: WindUi, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    Spacer(
        modifier.drawWithCache {
            val cx = size.width / 2
            val cy = size.height / 2
            val r = min(cx, cy) - 2.dp.toPx()
            val sCard = TextStyle(fontFamily = ShareTechMono, fontSize = 11.sp, color = AtmosColors.Muted)
            val cards = listOf("N", "E", "S", "O").map {
                measurer.measure(it, if (it == "N") sCard.copy(color = AtmosColors.Secondary) else sCard)
            }
            val ring = Stroke(1.dp.toPx())
            val arrow = Path().apply {
                moveTo(cx, cy - r * 0.78f)
                lineTo(cx + 7.dp.toPx(), cy - r * 0.52f)
                lineTo(cx + 2.dp.toPx(), cy - r * 0.55f)
                lineTo(cx + 2.dp.toPx(), cy + r * 0.55f)
                lineTo(cx - 2.dp.toPx(), cy + r * 0.55f)
                lineTo(cx - 2.dp.toPx(), cy - r * 0.55f)
                lineTo(cx - 7.dp.toPx(), cy - r * 0.52f)
                close()
            }
            onDrawBehind {
                drawCircle(AtmosColors.Secondary.copy(alpha = 0.08f), r, Offset(cx, cy))
                drawCircle(AtmosColors.LineSecondary, r, Offset(cx, cy), style = ring)
                drawCircle(AtmosColors.LinePrimary, r * 0.62f, Offset(cx, cy), style = ring)
                for (i in 0 until 72) {
                    val a = i * 5 * PI / 180
                    val major = i % 18 == 0
                    val len = if (major) 10.dp.toPx() else if (i % 2 == 0) 5.dp.toPx() else 2.5f.dp.toPx()
                    val s = sin(a).toFloat()
                    val c = cos(a).toFloat()
                    drawLine(
                        if (major) AtmosColors.Primary else AtmosColors.LinePrimary,
                        Offset(cx + (r - len) * s, cy - (r - len) * c), Offset(cx + r * s, cy - r * c),
                        if (major) 2.dp.toPx() else 1.dp.toPx()
                    )
                }
                val lr = r - 20.dp.toPx()
                cards.forEachIndexed { i, t ->
                    val a = i * PI / 2
                    val x = cx + (lr * sin(a)).toFloat() - t.size.width / 2f
                    val y = cy - (lr * cos(a)).toFloat() - t.size.height / 2f
                    drawText(t, topLeft = Offset(x, y))
                }
                // La flecha apunta hacia donde VA el viento (procedencia + 180°).
                rotate(wind.dirDeg + 180f, Offset(cx, cy)) {
                    drawPath(arrow, AtmosColors.Primary.copy(alpha = 0.25f), style = Stroke(5.dp.toPx()))
                    drawPath(arrow, AtmosColors.Primary)
                }
                drawCircle(AtmosColors.Secondary, 4.dp.toPx(), Offset(cx, cy))
            }
        }
    )
}

// ───────────────────────────── 05 AIRE ─────────────────────────────

private val AQI_COLORS get() = listOf(AtmosColors.Data, AtmosColors.Data, AtmosColors.Primary, AtmosColors.Primary, AtmosColors.Secondary, AtmosColors.Secondary)

@Composable
fun SectionAir(air: AirUi) {
    NeonPanel("05", "CALIDAD DEL AIRE", trailing = "EU-AQI") {
        val color = if (air.level < 0) AtmosColors.Muted else AQI_COLORS[air.level]
        Row(verticalAlignment = Alignment.Bottom) {
            Text(air.aqi?.toString() ?: "N/D", style = AtmosType.tempBig.copy(color = color, fontSize = 64.sp, lineHeight = 60.sp, shadow = ChromaShift))
            Spacer(Modifier.width(12.dp))
            Text(air.levelName, style = AtmosType.title.copy(color = AtmosColors.Text), modifier = Modifier.padding(bottom = 10.dp))
        }
        Spacer(Modifier.height(8.dp))
        SegBar(6, air.level + 1, segmentColors = AQI_COLORS, height = 8.dp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            air.cells.forEachIndexed { i, (k, v) ->
                StatCell(k, v, Modifier.weight(1f), accent = if (i % 2 == 0) AtmosColors.Data else AtmosColors.Primary)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("µg/m³", style = AtmosType.label.copy(color = AtmosColors.Dim))
    }
}

// ───────────────────────────── 06 POLEN ─────────────────────────────

@Composable
fun SectionPollen(rows: List<PollenRow>) {
    NeonPanel("06", "POLEN") {
        when {
            rows.all { it.level < 0 } -> {
                Text("// SIN DATOS DE POLEN PARA ESTA ZONA (SOLO EUROPA)", style = AtmosType.mono.copy(color = AtmosColors.Muted))
                return@NeonPanel
            }
            rows.all { it.level <= 0 } -> {
                Text("// AIRE LIMPIO · SIN POLEN SIGNIFICATIVO", style = AtmosType.mono.copy(color = AtmosColors.Data))
                Spacer(Modifier.height(10.dp))
            }
        }
        rows.forEach { p ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(p.name, style = AtmosType.label.copy(color = AtmosColors.Text), modifier = Modifier.width(92.dp))
                SegBar(4, p.level.coerceAtLeast(0), modifier = Modifier.weight(1f), segmentColors = POLLEN_COLORS)
                Spacer(Modifier.width(10.dp))
                val c = when {
                    p.level < 0 -> AtmosColors.Dim
                    p.level == 0 -> AtmosColors.Muted
                    p.level == 1 -> AtmosColors.Data
                    p.level == 2 -> AtmosColors.Primary
                    else -> AtmosColors.Secondary
                }
                Text(p.label, style = AtmosType.label.copy(color = c), modifier = Modifier.width(64.dp))
            }
        }
    }
}

private val POLLEN_COLORS get() = listOf(AtmosColors.Data, AtmosColors.Primary, AtmosColors.Secondary, AtmosColors.Secondary)

// ───────────────────────────── 07 SOL Y LUNA ─────────────────────────────

@Composable
fun SectionSunMoon(sm: SunMoonUi?) {
    if (sm == null) return
    NeonPanel("07", "SOL Y LUNA", color = AtmosColors.Primary, trailing = if (sm.dayFrac != null) "${(sm.dayFrac * 100).toInt()}% DÍA" else "NOCHE") {
        SunArc(sm.dayFrac, Modifier.fillMaxWidth().height(110.dp))
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LabeledValue("AMANECE", sm.sunrise, Alignment.Start)
            LabeledValue("LUZ", sm.daylight, Alignment.CenterHorizontally)
            LabeledValue("OCASO", sm.sunset, Alignment.End)
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(AtmosColors.LinePrimary.copy(alpha = 0.15f)))
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Moon(sm.moonPhase, Modifier.size(56.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(sm.moonName, style = AtmosType.title.copy(color = AtmosColors.Primary))
                Text("ILUMINACIÓN ${sm.moonIllum}% · LLENA EN ${sm.moonDaysToFull} D", style = AtmosType.label.copy(color = AtmosColors.Muted))
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String, align: Alignment.Horizontal) {
    Column(horizontalAlignment = align) {
        Text(label, style = AtmosType.label.copy(color = AtmosColors.Muted))
        Text(value, style = AtmosType.numMedium.copy(color = AtmosColors.Text))
    }
}

@Composable
private fun SunArc(dayFrac: Float?, modifier: Modifier) {
    Spacer(
        modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val pad = 14.dp.toPx()
            val base = h - 8.dp.toPx()
            val p0 = Offset(pad, base)
            val p1 = Offset(w / 2, -h * 0.55f)
            val p2 = Offset(w - pad, base)
            fun at(t: Float): Offset {
                val u = 1 - t
                return Offset(u * u * p0.x + 2 * u * t * p1.x + t * t * p2.x, u * u * p0.y + 2 * u * t * p1.y + t * t * p2.y)
            }
            val full = Path().apply { moveTo(p0.x, p0.y); quadraticBezierTo(p1.x, p1.y, p2.x, p2.y) }
            val done = Path().apply {
                moveTo(p0.x, p0.y)
                val f = dayFrac ?: 0f
                val steps = 40
                for (i in 1..steps) { val p = at(f * i / steps); lineTo(p.x, p.y) }
            }
            val sun = dayFrac?.let { at(it) }
            val dash = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())))
            val solid = Stroke(2.dp.toPx())
            val glow = Stroke(7.dp.toPx())
            onDrawBehind {
                drawLine(AtmosColors.LineSecondary, Offset(0f, base), Offset(w, base), 1.dp.toPx())
                drawPath(full, AtmosColors.LinePrimary, style = dash)
                if (sun != null) {
                    drawPath(done, AtmosColors.Primary.copy(alpha = 0.2f), style = glow)
                    drawPath(done, AtmosColors.Primary, style = solid)
                    drawCircle(AtmosColors.Secondary.copy(alpha = 0.18f), 16.dp.toPx(), sun)
                    drawCircle(AtmosColors.Secondary.copy(alpha = 0.35f), 11.dp.toPx(), sun)
                    drawCircle(AtmosColors.Secondary, 7.dp.toPx(), sun)
                    drawCircle(AtmosColors.Primary, 3.dp.toPx(), sun)
                }
                drawRect(AtmosColors.Primary, Offset(p0.x - 3.dp.toPx(), base - 3.dp.toPx()), Size(6.dp.toPx(), 6.dp.toPx()))
                drawRect(AtmosColors.Secondary, Offset(p2.x - 3.dp.toPx(), base - 3.dp.toPx()), Size(6.dp.toPx(), 6.dp.toPx()))
            }
        }
    )
}

/** Luna con terminador elíptico real (antes: un círculo desplazado que no respetaba la fase). */
@Composable
private fun Moon(phase: Double, modifier: Modifier) {
    Spacer(
        modifier.drawWithCache {
            val r = size.minDimension / 2 - 3.dp.toPx()
            val c = Offset(size.width / 2, size.height / 2)
            val circle = Rect(c.x - r, c.y - r, c.x + r, c.y + r)
            val rx = (r * abs(cos(2 * PI * phase))).toFloat()
            val ell = Rect(c.x - rx, c.y - r, c.x + rx, c.y + r)
            val lit = Path().apply {
                when {
                    phase < 0.5 -> { // creciente: lado derecho iluminado
                        arcTo(circle, -90f, 180f, true)
                        arcTo(ell, 90f, if (phase < 0.25) -180f else 180f, false)
                    }
                    else -> { // menguante: lado izquierdo iluminado
                        arcTo(circle, 90f, 180f, true)
                        arcTo(ell, -90f, if (phase > 0.75) -180f else 180f, false)
                    }
                }
                close()
            }
            onDrawBehind {
                drawCircle(AtmosColors.Primary.copy(alpha = 0.12f), r + 3.dp.toPx(), c)
                drawCircle(AtmosColors.PanelHi, r, c)
                drawPath(lit, AtmosColors.Text)
                drawCircle(AtmosColors.LinePrimary, r, c, style = Stroke(1.dp.toPx()))
            }
        }
    )
}

// ───────────────────────────── 08 RADAR ─────────────────────────────

@Composable
fun SectionRadar(radar: RadarUi?, loc: com.atmos.weather.data.cache.SavedCity?) {
    NeonPanel("08", "RADAR DE LLUVIA", trailing = "LIVE") {
        if (radar == null || loc == null) {
            Text("// SIN SEÑAL DE RADAR", style = AtmosType.mono.copy(color = AtmosColors.Muted))
            return@NeonPanel
        }
        RadarMap(radar, loc.lat, loc.lon)
        Spacer(Modifier.height(6.dp))
        Text("RADAR: RainViewer · MAPA: © OpenStreetMap", style = AtmosType.label.copy(color = AtmosColors.Dim))
    }
}
