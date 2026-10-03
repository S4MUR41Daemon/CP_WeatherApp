package com.atmos.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atmos.weather.ui.theme.AtmosColors
import com.atmos.weather.ui.theme.AtmosType

@Composable
fun BracketPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(AtmosColors.Panel)
            .drawBehind {
                val stroke = 2.dp.toPx()
                val arm = 12.dp.toPx()
                val c = AtmosColors.Accent
                // top-left
                drawLine(c, Offset(0f, 0f), Offset(arm, 0f), stroke)
                drawLine(c, Offset(0f, 0f), Offset(0f, arm), stroke)
                // bottom-right
                drawLine(c, Offset(size.width - arm, size.height), Offset(size.width, size.height), stroke)
                drawLine(c, Offset(size.width, size.height - arm), Offset(size.width, size.height), stroke)
            }
            .padding(14.dp),
        content = content
    )
}

@Composable
fun SectionHeader(number: String, title: String, trailing: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
    ) {
        Box(
            Modifier
                .background(AtmosColors.Accent)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(number, style = AtmosType.label.copy(color = AtmosColors.Bg))
        }
        Spacer(Modifier.width(8.dp))
        Text(title, style = AtmosType.title.copy(color = AtmosColors.Text))
        Spacer(Modifier.weight(1f).padding(horizontal = 8.dp)
            .drawBehind {
                drawLine(AtmosColors.Line, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.dp.toPx())
            }.height(1.dp))
        if (trailing != null) {
            Text(trailing, style = AtmosType.label.copy(color = AtmosColors.Muted))
        }
    }
}

@Composable
fun CodeChip(code: String, color: Color = AtmosColors.Accent2) {
    Box(
        Modifier
            .border(1.dp, color)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(code, style = AtmosType.label.copy(color = color))
    }
}

@Composable
fun SegBar(total: Int, filled: Int, color: Color = AtmosColors.Accent, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 0 until total) {
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(if (i < filled) color else color.copy(alpha = 0.18f))
            )
        }
    }
}

@Composable
fun ScanlineOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier.drawWithCache {
            onDrawBehind {
                val stroke = 1.dp.toPx()
                val step = 3.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawLine(AtmosColors.Scanline, Offset(0f, y), Offset(size.width, y), stroke)
                    y += step
                }
            }
        }
    )
}
