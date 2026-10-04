package com.atmos.weather.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.atmos.weather.AtmosApp
import com.atmos.weather.MainActivity
import com.atmos.weather.R
import com.atmos.weather.domain.WeatherUi
import com.atmos.weather.domain.toUi
import com.atmos.weather.ui.theme.Palette
import com.atmos.weather.ui.theme.Palettes
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Colores del widget derivados de la plantilla activa (Glance no ve el State de Compose de la app). */
private class WidgetColors(p: Palette) {
    val text = ColorProvider(p.text)
    val primary = ColorProvider(p.primary)
    val secondary = ColorProvider(p.secondarySoft)
    val data = ColorProvider(p.data)
    val muted = ColorProvider(p.muted)
    val line = p.secondary.copy(alpha = 0.5f)
    val frame = when (p.id) {
        Palettes.EVA_001.id -> R.drawable.widget_frame_eva001
        Palettes.NOCHE_NEON.id -> R.drawable.widget_frame_noche_neon
        Palettes.GREEN_LANTERN.id -> R.drawable.widget_frame_green_lantern
        else -> R.drawable.widget_frame_night_city
    }
}

private val SMALL = DpSize(160.dp, 60.dp)
private val MEDIUM = DpSize(250.dp, 110.dp)
private val LARGE = DpSize(250.dp, 220.dp)

object WidgetUpdater {
    suspend fun update(context: Context) {
        try { AtmosWidget().updateAll(context) } catch (_: Throwable) {}
    }
}

class AtmosWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as AtmosApp
        val cached = app.container.repository.loadCached()
        val ui = cached?.bundle?.toUi()
        val city = cached?.location?.name?.uppercase() ?: "ATMOS//OS"
        val sync = cached?.bundle?.fetchedAt?.takeIf { it > 0 }
            ?.let { SimpleDateFormat("HH:mm", Locale("es")).format(Date(it)) } ?: "--:--"
        val colors = WidgetColors(Palettes.byId(app.container.prefs.themeId.first()))
        provideContent { Content(ui, city, sync, colors) }
    }

    @Composable
    private fun Content(ui: WeatherUi?, city: String, sync: String, c: WidgetColors) {
        val size = LocalSize.current
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(c.frame))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (ui == null) {
                Text("ATMOS//OS", style = TextStyle(color = c.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                Text("SIN DATOS · ABRE LA APP", style = mono(c.muted, 10))
                return@Column
            }
            Header(ui, city, sync, compact = size.height < MEDIUM.height, c = c)
            if (size.height >= MEDIUM.height) {
                Spacer(GlanceModifier.defaultWeight())
                Divider(c)
                Spacer(GlanceModifier.height(6.dp))
                Hours(ui, c)
            }
            if (size.height >= LARGE.height && ui.days.size > 1) {
                Spacer(GlanceModifier.defaultWeight())
                Divider(c)
                Spacer(GlanceModifier.height(4.dp))
                Days(ui, c)
            }
        }
    }

    @Composable
    private fun Header(ui: WeatherUi, city: String, sync: String, compact: Boolean, c: WidgetColors) {
        val n = ui.now
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${n.temp}°",
                style = TextStyle(color = c.text, fontSize = if (compact) 34.sp else 46.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("[${n.code}] ${n.label}", style = mono(c.data, 11), maxLines = 1)
                Text(city, style = TextStyle(color = c.primary, fontSize = 16.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                if (!compact) {
                    Row {
                        Text("▲${n.hi}°", style = mono(c.secondary, 11))
                        Spacer(GlanceModifier.width(6.dp))
                        Text("▼${n.lo}°", style = mono(c.data, 11))
                        Spacer(GlanceModifier.width(6.dp))
                        Text("☂${ui.hours.firstOrNull()?.prob ?: 0}%", style = mono(c.muted, 11))
                    }
                }
            }
            if (!compact) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("ATMOS//OS", style = mono(c.secondary, 9))
                    Text("SYNC $sync", style = mono(c.muted, 9))
                }
            }
        }
    }

    @Composable
    private fun Divider(c: WidgetColors) {
        Box(GlanceModifier.fillMaxWidth().height(1.dp).background(c.line)) {}
    }

    @Composable
    private fun Hours(ui: WeatherUi, c: WidgetColors) {
        val hours = ui.hours.filterIndexed { i, _ -> i % 2 == 0 }.take(6)
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            hours.forEachIndexed { i, h ->
                Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(h.label, style = mono(if (i == 0) c.primary else c.muted, 10, TextAlign.Center))
                    Text("${Math.round(h.temp)}°", style = TextStyle(color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
                    Text("${h.prob}%", style = mono(if (h.prob > 0) c.data else c.muted, 10, TextAlign.Center))
                }
            }
        }
    }

    @Composable
    private fun Days(ui: WeatherUi, c: WidgetColors) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            ui.days.drop(1).take(4).forEach { d ->
                Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(d.weekday, style = mono(c.text, 11), modifier = GlanceModifier.width(40.dp))
                    Text(d.code, style = mono(c.data, 11), modifier = GlanceModifier.defaultWeight())
                    Text("${d.prob}%", style = mono(c.muted, 11), modifier = GlanceModifier.width(40.dp))
                    Text("${d.tmin}°", style = mono(c.data, 11), modifier = GlanceModifier.width(32.dp))
                    Text("${d.tmax}°", style = mono(c.secondary, 11), modifier = GlanceModifier.width(32.dp))
                }
            }
        }
    }

    private fun mono(c: ColorProvider, size: Int, align: TextAlign = TextAlign.Start) =
        TextStyle(color = c, fontSize = size.sp, fontFamily = FontFamily.Monospace, textAlign = align)
}

class AtmosWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AtmosWidget()
}
