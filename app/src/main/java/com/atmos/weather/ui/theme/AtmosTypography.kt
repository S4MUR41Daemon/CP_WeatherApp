package com.atmos.weather.ui.theme

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
private val mono = GoogleFont("Share Tech Mono")

val ChakraPetch = FontFamily(
    Font(googleFont = chakra, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = chakra, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = chakra, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = chakra, fontProvider = provider, weight = FontWeight.Bold)
)

val ShareTechMono = FontFamily(
    Font(googleFont = mono, fontProvider = provider, weight = FontWeight.Normal)
)

object AtmosType {
    val city = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = 0.02.em)
    val tempBig = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold, fontSize = 108.sp, letterSpacing = (-0.05).em)
    val title = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.18.em)
    val body = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.Normal, fontSize = 13.sp)
    val numMedium = TextStyle(fontFamily = ChakraPetch, fontWeight = FontWeight.Medium, fontSize = 16.sp)
    val label = TextStyle(fontFamily = ShareTechMono, fontSize = 10.sp, letterSpacing = 0.12.em)
    val mono = TextStyle(fontFamily = ShareTechMono, fontSize = 12.sp)
    val monoBig = TextStyle(fontFamily = ShareTechMono, fontSize = 14.sp)
}
