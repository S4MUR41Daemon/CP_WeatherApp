package com.atmos.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.atmos.weather.ui.theme.AtmosColors
import com.atmos.weather.ui.theme.AtmosType
import kotlin.random.Random

/** Polígono con las esquinas superior-izquierda e inferior-derecha cortadas (estilo HUD de CP2077). */
private fun chamferPath(w: Float, h: Float, cut: Float, inset: Float): Path = Path().apply {
    moveTo(cut + inset, inset)
    lineTo(w - inset, inset)
    lineTo(w - inset, h - cut - inset)
    lineTo(w - cut - inset, h - inset)
    lineTo(inset, h - inset)
    lineTo(inset, cut + inset)
    close()
}

/**
 * Marco neón: relleno + 3 trazos concéntricos con alfa decreciente que simulan el resplandor.
 * Todo el trabajo geométrico va en drawWithCache, así que al hacer scroll solo se reproducen
 * cuatro drawPath ya construidos (BlurMaskFilter sería mucho más caro y no va en GPU < API 28).
 */
fun Modifier.neonFrame(
    color: Color,
    cut: Dp = 16.dp,
    fill: Color = AtmosColors.Panel,
    accent: Color = color
): Modifier = drawWithCache {
    val c = cut.toPx()
    val core = 1.5.dp.toPx()
    val path = chamferPath(size.width, size.height, c, core / 2)
    val glowWide = Stroke(width = 9.dp.toPx())
    val glowMid = Stroke(width = 4.dp.toPx())
    val coreStroke = Stroke(width = core)
    val notchW = 44.dp.toPx()
    val notchH = 4.dp.toPx()
    val tick = 10.dp.toPx()
    val glowA = color.copy(alpha = 0.07f)
    val glowB = color.copy(alpha = 0.22f)
    onDrawBehind {
        drawPath(path, fill)
        drawPath(path, glowA, style = glowWide)
        drawPath(path, glowB, style = glowMid)
        drawPath(path, color, style = coreStroke)
        // Muesca sólida en el borde superior y diagonal de acento en la esquina cortada.
        drawRect(accent, Offset(size.width - notchW - c, 0f), Size(notchW, notchH))
        drawLine(accent, Offset(0f, c + tick), Offset(c + tick, 0f), 2.dp.toPx())
        // Marcas de calibración en la esquina inferior izquierda.
        for (k in 0..2) {
            val x = 8.dp.toPx() + k * 6.dp.toPx()
            drawLine(accent.copy(alpha = 0.6f), Offset(x, size.height - 6.dp.toPx()), Offset(x, size.height - 2.dp.toPx()), 1.dp.toPx())
        }
    }
}

@Composable
fun NeonPanel(
    number: String,
    title: String,
    modifier: Modifier = Modifier,
    color: Color = AtmosColors.Red,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .neonFrame(color, accent = if (color == AtmosColors.Red) AtmosColors.Yellow else AtmosColors.Red)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 18.dp)
    ) {
        SectionHeader(number, title, color, trailing)
        content()
    }
}

@Composable
fun SectionHeader(number: String, title: String, color: Color = AtmosColors.Red, trailing: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Box(
            Modifier
                .clip(CutCornerShape(topStart = 6.dp))
                .background(AtmosColors.Yellow)
                .padding(start = 8.dp, end = 6.dp, top = 1.dp, bottom = 1.dp)
        ) {
            Text(number, style = AtmosType.label.copy(color = AtmosColors.Bg))
        }
        Spacer(Modifier.width(6.dp))
        Text("//", style = AtmosType.label.copy(color = color))
        Spacer(Modifier.width(6.dp))
        Text(title, style = AtmosType.title.copy(color = AtmosColors.Text), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(
            Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                .height(8.dp)
                .drawBehind {
                    val y = size.height / 2
                    drawLine(color.copy(alpha = 0.45f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                    drawRect(color, Offset(size.width - 6.dp.toPx(), 0f), Size(6.dp.toPx(), size.height))
                }
        )
        if (trailing != null) {
            Text(trailing, style = AtmosType.label.copy(color = AtmosColors.RedSoft))
        }
    }
}

@Composable
fun CodeChip(code: String, color: Color = AtmosColors.Cyan, modifier: Modifier = Modifier) {
    Box(
        modifier
            .drawWithCache {
                val p = chamferPath(size.width, size.height, 5.dp.toPx(), 0.5.dp.toPx())
                val st = Stroke(1.dp.toPx())
                val bg = color.copy(alpha = 0.1f)
                onDrawBehind { drawPath(p, bg); drawPath(p, color, style = st) }
            }
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(code, style = AtmosType.label.copy(color = color))
    }
}

/** Botón biselado. `filled` = amarillo sólido (acción primaria). */
@Composable
fun CyberButton(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    color: Color = AtmosColors.Yellow,
    onClick: () -> Unit
) {
    val shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)
    Box(
        modifier
            .height(42.dp)
            .clip(shape)
            .then(if (filled) Modifier.background(color) else Modifier.neonFrame(color, cut = 10.dp, fill = AtmosColors.Bg))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = AtmosType.button.copy(color = if (filled) AtmosColors.Bg else color))
    }
}

@Composable
fun SegBar(
    total: Int,
    filled: Int,
    color: Color = AtmosColors.Yellow,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    segmentColors: List<Color>? = null
) {
    Canvas(modifier.fillMaxWidth().height(height)) {
        val gap = 2.dp.toPx()
        val w = (size.width - gap * (total - 1)) / total
        val skew = size.height * 0.6f
        for (i in 0 until total) {
            val x = i * (w + gap)
            val p = Path().apply {
                moveTo(x + skew, 0f); lineTo(x + w, 0f); lineTo(x + w - skew, size.height); lineTo(x, size.height); close()
            }
            val c = segmentColors?.getOrNull(i) ?: color
            drawPath(p, if (i < filled) c else c.copy(alpha = 0.14f))
        }
    }
}

/** Celda de dato con barra de acento lateral (menos ruido visual que un borde completo). */
@Composable
fun StatCell(label: String, value: String, modifier: Modifier = Modifier, accent: Color = AtmosColors.Yellow) {
    Column(
        modifier
            .background(AtmosColors.PanelHi)
            .drawBehind { drawRect(accent, Offset.Zero, Size(2.dp.toPx(), size.height)) }
            .padding(start = 10.dp, end = 6.dp, top = 7.dp, bottom = 7.dp)
    ) {
        Text(label, style = AtmosType.label.copy(color = AtmosColors.Muted), maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(value, style = AtmosType.numMedium.copy(color = AtmosColors.Text), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Scanlines CRT: un único drawRect con shader en mosaico, en vez de cientos de drawLine por frame. */
@Composable
fun ScanlineOverlay(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val brush = remember(density) {
        val step = with(density) { 3.dp.roundToPx() }.coerceAtLeast(3)
        val line = with(density) { 1.dp.roundToPx() }.coerceAtLeast(1)
        val bmp = ImageBitmap(1, step)
        val canvas = androidx.compose.ui.graphics.Canvas(bmp)
        canvas.drawRect(0f, 0f, 1f, line.toFloat(), Paint().apply { color = AtmosColors.Scanline })
        ShaderBrush(ImageShader(bmp, TileMode.Repeated, TileMode.Repeated))
    }
    Box(modifier.drawBehind { drawRect(brush) })
}

/** Glitch de 380 ms al sincronizar: franjas desplazadas en rojo/cian. Solo se compone mientras dura. */
@Composable
fun GlitchOverlay(seed: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val rnd = Random(seed)
        repeat(6) {
            val y = rnd.nextFloat() * size.height
            val h = 3.dp.toPx() + rnd.nextFloat() * 18.dp.toPx()
            val x = (rnd.nextFloat() - 0.5f) * 60.dp.toPx()
            val c = if (rnd.nextBoolean()) AtmosColors.Red else AtmosColors.Cyan
            drawRect(c.copy(alpha = 0.35f), Offset(x, y), Size(size.width, h), blendMode = BlendMode.Screen)
        }
        drawRect(AtmosColors.Yellow.copy(alpha = 0.06f))
    }
}

/** Banda de franjas diagonales tipo «zona de peligro». */
fun Modifier.hazardStripes(color: Color, step: Dp = 10.dp): Modifier = drawWithCache {
    val s = step.toPx()
    val sw = s * 0.45f
    onDrawBehind {
        clipRect {
            var x = -size.height
            while (x < size.width) {
                drawLine(color, Offset(x, size.height), Offset(x + size.height, 0f), sw)
                x += s
            }
        }
    }
}
