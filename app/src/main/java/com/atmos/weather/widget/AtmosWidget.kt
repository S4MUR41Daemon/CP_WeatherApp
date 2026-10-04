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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val C_TEXT = ColorProvider(Color(0xFFF2EEDC))
private val C_YELLOW = ColorProvider(Color(0xFFFCEE0A))
private val C_RED = ColorProvider(Color(0xFFFF4D6D))
private val C_CYAN = ColorProvider(Color(0xFF02D7F2))
private val C_MUTED = ColorProvider(Color(0xFF8C887C))
private val LINE_RED = Color(0x80FF003C)

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
        provideContent { Content(ui, city, sync) }
    }

    @Composable
    private fun Content(ui: WeatherUi?, city: String, sync: String) {
        val size = LocalSize.current
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_frame))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (ui == null) {
                Text("ATMOS//OS", style = TextStyle(color = C_YELLOW, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                Text("SIN DATOS · ABRE LA APP", style = mono(C_MUTED, 10))
                return@Column
            }
            Header(ui, city, sync, compact = size.height < MEDIUM.height)
            if (size.height >= MEDIUM.height) {
                Spacer(GlanceModifier.defaultWeight())
                Divider()
                Spacer(GlanceModifier.height(6.dp))
                Hours(ui)
            }
            if (size.height >= LARGE.height && ui.days.size > 1) {
                Spacer(GlanceModifier.defaultWeight())
                Divider()
                Spacer(GlanceModifier.height(4.dp))
                Days(ui)
            }
        }
    }

    @Composable
    private fun Header(ui: WeatherUi, city: String, sync: String, compact: Boolean) {
        val n = ui.now
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${n.temp}°",
                style = TextStyle(color = C_TEXT, fontSize = if (compact) 34.sp else 46.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("[${n.code}] ${n.label}", style = mono(C_CYAN, 11), maxLines = 1)
                Text(city, style = TextStyle(color = C_YELLOW, fontSize = 16.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                if (!compact) {
                    Row {
                        Text("▲${n.hi}°", style = mono(C_RED, 11))
                        Spacer(GlanceModifier.width(6.dp))
                        Text("▼${n.lo}°", style = mono(C_CYAN, 11))
                        Spacer(GlanceModifier.width(6.dp))
                        Text("☂${ui.hours.firstOrNull()?.prob ?: 0}%", style = mono(C_MUTED, 11))
                    }
                }
            }
            if (!compact) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("ATMOS//OS", style = mono(C_RED, 9))
                    Text("SYNC $sync", style = mono(C_MUTED, 9))
                }
            }
        }
    }

    @Composable
    private fun Divider() {
        Box(GlanceModifier.fillMaxWidth().height(1.dp).background(LINE_RED)) {}
    }

    @Composable
    private fun Hours(ui: WeatherUi) {
        val hours = ui.hours.filterIndexed { i, _ -> i % 2 == 0 }.take(6)
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            hours.forEachIndexed { i, h ->
                Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(h.label, style = mono(if (i == 0) C_YELLOW else C_MUTED, 10, TextAlign.Center))
                    Text("${Math.round(h.temp)}°", style = TextStyle(color = C_TEXT, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
                    Text("${h.prob}%", style = mono(if (h.prob > 0) C_CYAN else C_MUTED, 10, TextAlign.Center))
                }
            }
        }
    }

    @Composable
    private fun Days(ui: WeatherUi) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            ui.days.drop(1).take(4).forEach { d ->
                Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(d.weekday, style = mono(C_TEXT, 11), modifier = GlanceModifier.width(40.dp))
                    Text(d.code, style = mono(C_CYAN, 11), modifier = GlanceModifier.defaultWeight())
                    Text("${d.prob}%", style = mono(C_MUTED, 11), modifier = GlanceModifier.width(40.dp))
                    Text("${d.tmin}°", style = mono(C_CYAN, 11), modifier = GlanceModifier.width(32.dp))
                    Text("${d.tmax}°", style = mono(C_RED, 11), modifier = GlanceModifier.width(32.dp))
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
