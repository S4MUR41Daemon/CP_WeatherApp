package com.atmos.weather.ui.theme

import androidx.compose.ui.graphics.Color

/** Paleta «Night City»: amarillo y rojo neón de Cyberpunk 2077, cian como color terciario de datos. */
object AtmosColors {
    val Bg = Color(0xFF060608)
    val Panel = Color(0xFF0E0C10)
    val PanelHi = Color(0xFF17141A)

    val Yellow = Color(0xFFFCEE0A)
    val Red = Color(0xFFFF003C)
    val RedSoft = Color(0xFFFF4D6D)
    val Cyan = Color(0xFF02D7F2)

    val Text = Color(0xFFF2EEDC)
    val Muted = Color(0xFF8C887C)
    val Dim = Color(0xFF4A4740)

    val LineY = Color(0x40FCEE0A) // amarillo @ 25%
    val LineR = Color(0x59FF003C) // rojo @ 35%
    val Track = Color(0x1FFCEE0A) // pista de barras
    val Scanline = Color(0x1A000000)

    // Alias para no romper código antiguo.
    val Accent get() = Yellow
    val Accent2 get() = Cyan
    val Warn get() = Red
    val Line get() = LineY
}
