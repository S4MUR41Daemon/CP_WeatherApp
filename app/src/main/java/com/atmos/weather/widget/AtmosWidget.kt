package com.atmos.weather.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import androidx.compose.ui.graphics.Color
import com.atmos.weather.AtmosApp
import com.atmos.weather.MainActivity
import com.atmos.weather.domain.wmo
import kotlin.math.roundToInt

class AtmosWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as AtmosApp
        val cached = app.container.repository.loadCached()
        provideContent { Content(cached) }
    }

    @Composable
    private fun Content(cached: com.atmos.weather.data.repository.CachedBundle?) {
        val bg = ColorProvider(Color(0xDD08080A))
        val accent = ColorProvider(Color(0xFFFCEE0A))
        val accent2 = ColorProvider(Color(0xFF00E5FF))
        val textCol = ColorProvider(Color(0xFFF4F1E1))
        val muted = ColorProvider(Color(0xFF9B9888))
        val cur = cached?.bundle?.forecast?.current
        val daily = cached?.bundle?.forecast?.daily
        val city = cached?.location?.name?.uppercase() ?: "ATMOS//OS"

        Column(
            modifier = GlanceModifier.fillMaxSize().background(bg).padding(10.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                Text(cur?.temperature?.roundToInt()?.toString() ?: "--", style = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, color = textCol))
                Text("°C", style = TextStyle(fontSize = 14.sp, color = accent))
                Spacer(GlanceModifier.width(10.dp))
                Column {
                    val code = cur?.let { wmo(it.code).code } ?: "---"
                    Text(code, style = TextStyle(fontSize = 11.sp, color = accent2))
                    Text(city, style = TextStyle(fontSize = 12.sp, color = accent, fontWeight = FontWeight.Bold))
                    if (daily != null) {
                        Text("↑${daily.tmax[0].roundToInt()}° ↓${daily.tmin[0].roundToInt()}°", style = TextStyle(fontSize = 10.sp, color = muted))
                    }
                }
            }
            Spacer(GlanceModifier.height(6.dp))
            val hourly = cached?.bundle?.forecast?.hourly
            if (hourly != null) {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    for (i in 0 until minOf(6, hourly.time.size)) {
                        Column(modifier = GlanceModifier.padding(horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (i == 0) "YA" else hourly.time[i].substring(11, 13), style = TextStyle(fontSize = 9.sp, color = muted))
                            Text("${hourly.temperature[i].roundToInt()}°", style = TextStyle(fontSize = 11.sp, color = textCol))
                            Text("${(hourly.precipProb.getOrNull(i) ?: 0.0).roundToInt()}%", style = TextStyle(fontSize = 9.sp, color = accent2))
                        }
                    }
                }
            }
        }
    }
}

class AtmosWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AtmosWidget()
}
