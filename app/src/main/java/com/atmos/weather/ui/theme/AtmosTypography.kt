package com.atmos.weather.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.atmos.weather.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val chakra = GoogleFont("Chakra Petch")
private val rajdhani = GoogleFont("Rajdhani")
private val mono = GoogleFont("Share Tech Mono")

val ChakraPetch = FontFamily(
    Font(googleFont = chakra, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = chakra, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = chakra, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = chakra, fontProvider = provider, weight = FontWeight.Bold)
)

/** Rajdhani: condensada y angulosa, muy cercana a la tipografía de los menús de Cyberpunk 2077. */
val Rajdhani = FontFamily(
    Font(googleFont = rajdhani, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = rajdhani, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = rajdhani, fontProvider = provider, weight = FontWeight.Bold)
)

val ShareTechMono = FontFamily(
    Font(googleFont = mono, fontProvider = provider, weight = FontWeight.Normal)
)

/** Sombra desplazada en el color secundario = aberración cromática barata (sin blur, sin capas extra). */
val ChromaShift: Shadow get() = Shadow(color = AtmosColors.Secondary, offset = Offset(4f, 0f), blurRadius = 0f)

object AtmosType {
    val city = TextStyle(fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = 0.04.em, lineHeight = 40.sp)
    val tempBig = TextStyle(fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 112.sp, letterSpacing = (-0.03).em, lineHeight = 104.sp)
    val title = TextStyle(fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 17.sp, letterSpacing = 0.16.em)
    val button = TextStyle(fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.2.em)
    val body = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.Normal, fontSize = 13.sp)
    val numMedium = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    val numLarge = TextStyle(fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 30.sp)
    val label = TextStyle(fontFamily = ShareTechMono, fontSize = 10.sp, letterSpacing = 0.12.em)
    val mono = TextStyle(fontFamily = ShareTechMono, fontSize = 12.sp)
    val monoBig = TextStyle(fontFamily = ShareTechMono, fontSize = 14.sp)
}
