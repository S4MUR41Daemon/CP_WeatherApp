package com.atmos.weather.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Plantilla de color. Solo define roles; la UI nunca usa un color «fijo», así que cambiar de plantilla
 * cambia la estética sin tocar la lógica.
 *  - primary: nombre de ciudad, chips activos, línea de temperatura, acentos de marco.
 *  - secondary: marcos neón, aberración cromática, detalles de HUD.
 *  - data: datos secundarios (lluvia %, mínimas, códigos de estado).
 *  - warn: avisos meteorológicos y «sin red» (siempre un tono de alarma).
 */
@Immutable
data class Palette(
    val id: String,
    val name: String,
    val bg: Color,
    val panel: Color,
    val panelHi: Color,
    val primary: Color,
    val secondary: Color,
    val secondarySoft: Color,
    val data: Color,
    val warn: Color,
    val text: Color,
    val muted: Color,
    val dim: Color
)

object Palettes {
    /** Amarillo + rojo de Cyberpunk 2077. */
    val NIGHT_CITY = Palette(
        id = "night_city", name = "NIGHT CITY",
        bg = Color(0xFF060608), panel = Color(0xFF0E0C10), panelHi = Color(0xFF17141A),
        primary = Color(0xFFFCEE0A), secondary = Color(0xFFFF003C), secondarySoft = Color(0xFFFF4D6D),
        data = Color(0xFF02D7F2), warn = Color(0xFFFF003C),
        text = Color(0xFFF2EEDC), muted = Color(0xFF8C887C), dim = Color(0xFF4A4740)
    )

    /** Morado de la Unidad 01 con verde neón y naranja de alerta. */
    val EVA_001 = Palette(
        id = "eva001", name = "EVA001",
        bg = Color(0xFF07050C), panel = Color(0xFF100A1A), panelHi = Color(0xFF1A1229),
        primary = Color(0xFF6BFF3B), secondary = Color(0xFF9B4DFF), secondarySoft = Color(0xFFBE8CFF),
        data = Color(0xFFFF9A1F), warn = Color(0xFFFF3D3D),
        text = Color(0xFFEFE8FF), muted = Color(0xFF8E83A8), dim = Color(0xFF4A4060)
    )

    /** Cian eléctrico con magenta de neón. */
    val NOCHE_NEON = Palette(
        id = "noche_neon", name = "NOCHE DE NEÓN",
        bg = Color(0xFF03070C), panel = Color(0xFF09111A), panelHi = Color(0xFF101B27),
        primary = Color(0xFF00F0FF), secondary = Color(0xFFFF2BD6), secondarySoft = Color(0xFFFF73E6),
        data = Color(0xFF7C8CFF), warn = Color(0xFFFF2B5E),
        text = Color(0xFFE6FBFF), muted = Color(0xFF7E95A1), dim = Color(0xFF3A4A56)
    )

    /** Verde esmeralda sobre negro. */
    val GREEN_LANTERN = Palette(
        id = "green_lantern", name = "GREEN LANTERN",
        bg = Color(0xFF030805), panel = Color(0xFF08110B), panelHi = Color(0xFF0F1C13),
        primary = Color(0xFF4DFF5E), secondary = Color(0xFF00C853), secondarySoft = Color(0xFF6BFFA0),
        data = Color(0xFF00E5C0), warn = Color(0xFFFF3B3B),
        text = Color(0xFFE9FFEC), muted = Color(0xFF7FA088), dim = Color(0xFF3A5242)
    )

    val all = listOf(NIGHT_CITY, EVA_001, NOCHE_NEON, GREEN_LANTERN)

    fun byId(id: String?): Palette = all.firstOrNull { it.id == id } ?: NIGHT_CITY
}

/**
 * Acceso a la plantilla activa. `palette` es un State de Compose: cada lectura desde composición o
 * dibujo queda observada, así que cambiar de plantilla repinta la app al instante sin reiniciarla.
 */
object AtmosColors {
    var palette by mutableStateOf(Palettes.NIGHT_CITY)

    val Bg get() = palette.bg
    val Panel get() = palette.panel
    val PanelHi get() = palette.panelHi

    val Primary get() = palette.primary
    val Secondary get() = palette.secondary
    val SecondarySoft get() = palette.secondarySoft
    val Data get() = palette.data
    val Warn get() = palette.warn

    val Text get() = palette.text
    val Muted get() = palette.muted
    val Dim get() = palette.dim

    val LinePrimary get() = palette.primary.copy(alpha = 0.25f)
    val LineSecondary get() = palette.secondary.copy(alpha = 0.35f)
    val Track get() = palette.primary.copy(alpha = 0.12f)
    val Scanline = Color(0x1A000000)
}
