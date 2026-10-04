package com.atmos.weather.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atmos.weather.ui.components.NeonPanel
import com.atmos.weather.ui.components.neonFrame
import com.atmos.weather.ui.theme.AtmosColors
import com.atmos.weather.ui.theme.AtmosType
import com.atmos.weather.ui.theme.Palette
import com.atmos.weather.ui.theme.Palettes

@Composable
fun SectionSettings(themeId: String, onTheme: (String) -> Unit, rainEnabled: Boolean, onRain: (Boolean) -> Unit) {
    NeonPanel("09", "AJUSTES") {
        Text("PLANTILLA DE COLOR", style = AtmosType.label.copy(color = AtmosColors.Muted))
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Palettes.all.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { p ->
                        ThemeCard(p, selected = p.id == themeId, Modifier.weight(1f)) { onTheme(p.id) }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LLUVIA DIGITAL", style = AtmosType.label.copy(color = AtmosColors.Muted), modifier = Modifier.weight(1f))
            val shape = CutCornerShape(topStart = 5.dp, bottomEnd = 5.dp)
            Box(
                Modifier
                    .clip(shape)
                    .background(if (rainEnabled) AtmosColors.Primary else AtmosColors.Panel)
                    .border(1.dp, if (rainEnabled) AtmosColors.Primary else AtmosColors.LineSecondary, shape)
                    .clickable { onRain(!rainEnabled) }
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(if (rainEnabled) "ON" else "OFF", style = AtmosType.label.copy(color = if (rainEnabled) AtmosColors.Bg else AtmosColors.Text))
            }
        }
    }
}

/** Vista previa pintada con los colores de SU plantilla, no con los de la activa. */
@Composable
private fun ThemeCard(p: Palette, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .neonFrame(
                color = if (selected) p.primary else p.secondary.copy(alpha = 0.6f),
                cut = 12.dp, fill = p.panel, accent = p.secondary
            )
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Text(p.name, style = AtmosType.title.copy(color = p.primary, fontSize = 14.sp, shadow = androidx.compose.ui.graphics.Shadow(p.secondary, androidx.compose.ui.geometry.Offset(3f, 0f), 0f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(p.primary, p.secondary, p.data, p.text).forEach { c ->
                Box(Modifier.size(width = 18.dp, height = 8.dp).background(c))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (selected) "▶ ACTIVA" else "SELECCIONAR",
            style = AtmosType.label.copy(color = if (selected) p.primary else p.muted)
        )
    }
}
