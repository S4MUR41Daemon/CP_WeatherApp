package com.atmos.weather.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atmos.weather.data.cache.SavedCity
import com.atmos.weather.ui.components.*
import com.atmos.weather.ui.sections.*
import com.atmos.weather.ui.theme.AtmosColors
import com.atmos.weather.ui.theme.AtmosType
import com.atmos.weather.ui.viewmodel.AtmosViewModel
import com.atmos.weather.domain.rainIntensity
import kotlinx.coroutines.launch

private val SECTIONS = listOf(
    "ahora" to "AHORA",
    "horas" to "HORAS",
    "dias" to "14 DÍAS",
    "viento" to "VIENTO",
    "aire" to "AIRE",
    "polen" to "POLEN",
    "sol" to "SOL/LUNA",
    "radar" to "RADAR"
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AtmosScreen() {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
    val vm: AtmosViewModel = viewModel(factory = AtmosViewModel.Factory(app))
    val s by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val b = s.bundle
    val cur = b?.forecast?.current
    val intensity = if (cur != null) rainIntensity(cur.code, cur.precipitation) else 0.0

    Box(Modifier.fillMaxSize().background(AtmosColors.Bg)) {
        DigitalRain(
            rainIntensity = intensity,
            enabled = s.rainEnabled,
            modifier = Modifier.fillMaxSize()
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp)
        ) {
            item { HeaderBlock(s, onSync = { vm.refresh() }, onCity = { vm.openSheet() }) }
            stickyHeader { StickyNav(SECTIONS) { id ->
                val index = SECTIONS.indexOfFirst { it.first == id } + 2 // offset header + nav
                scope.launch { listState.animateScrollToItem(index.coerceAtLeast(0)) }
            } }
            item { AlertsBand(s) }
            item { Box(Modifier.fillMaxWidth()) { SectionNow(s) } }
            item { Box(Modifier.fillMaxWidth()) { SectionHours(s, onRange = { vm.setRange(it) }) } }
            item { Box(Modifier.fillMaxWidth()) { SectionDays(s, onToggle = { vm.toggleDay(it) }) } }
            item { Box(Modifier.fillMaxWidth()) { SectionWind(s) } }
            item { Box(Modifier.fillMaxWidth()) { SectionAir(s) } }
            item { Box(Modifier.fillMaxWidth()) { SectionPollen(s) } }
            item { Box(Modifier.fillMaxWidth()) { SectionSunMoon(s) } }
            item { Box(Modifier.fillMaxWidth()) { SectionRadar(s) } }
            item { Footer(s) }
        }
        ScanlineOverlay(Modifier.fillMaxSize())

        if (s.sheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { vm.closeSheet() },
                containerColor = AtmosColors.Panel,
                dragHandle = null
            ) {
                LocationSheet(s, vm)
            }
        }
    }
}

@Composable
private fun HeaderBlock(s: com.atmos.weather.ui.viewmodel.UiState, onSync: () -> Unit, onCity: () -> Unit) {
    val loc = s.location
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(AtmosColors.Accent2))
            Spacer(Modifier.width(6.dp))
            val gpsLbl = when {
                loc == null -> "BUSCANDO SEÑAL"
                loc.gps -> "GPS · FIJADO"
                loc.manual -> "UBICACIÓN MANUAL"
                else -> "SIN GPS · POR DEFECTO"
            }
            Text(gpsLbl, style = AtmosType.label.copy(color = AtmosColors.Muted))
            Spacer(Modifier.weight(1f))
            if (loc != null) {
                val coords = "${"%.2f".format(kotlin.math.abs(loc.lat))}°${if (loc.lat >= 0) "N" else "S"} ${"%.2f".format(kotlin.math.abs(loc.lon))}°${if (loc.lon >= 0) "E" else "O"}"
                Text(coords, style = AtmosType.label.copy(color = AtmosColors.Muted))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            loc?.name?.uppercase() ?: "LOCALIZANDO…",
            style = AtmosType.city.copy(color = AtmosColors.Accent)
        )
        if (loc != null && loc.region.isNotEmpty()) {
            Text(loc.region.uppercase(), style = AtmosType.label.copy(color = AtmosColors.Muted))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .height(40.dp)
                    .border(1.dp, AtmosColors.Line)
                    .clickable { onCity() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) { Text("CIUDAD", style = AtmosType.title.copy(color = AtmosColors.Text)) }
            Box(
                Modifier
                    .height(40.dp)
                    .background(AtmosColors.Accent)
                    .clickable { onSync() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) { Text("SYNC", style = AtmosType.title.copy(color = AtmosColors.Bg)) }
        }
    }
}

@Composable
private fun StickyNav(sections: List<Pair<String, String>>, onPick: (String) -> Unit) {
    val scroll = rememberScrollState()
    Row(
        Modifier
            .fillMaxWidth()
            .background(AtmosColors.Bg)
            .horizontalScroll(scroll)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        sections.forEach { (id, label) ->
            Box(
                Modifier
                    .border(1.dp, AtmosColors.Line)
                    .clickable { onPick(id) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) { Text(label, style = AtmosType.label.copy(color = AtmosColors.Text)) }
        }
    }
}

@Composable
private fun AlertsBand(s: com.atmos.weather.ui.viewmodel.UiState) {
    val b = s.bundle ?: return
    val d = b.forecast?.daily ?: return
    val h = b.forecast.hourly ?: return
    val next24 = h.code.take(24)
    val alerts = com.atmos.weather.domain.computeAlerts(
        next24,
        d.gustMax.firstOrNull() ?: 0.0,
        d.uvMax.firstOrNull() ?: 0.0,
        d.tmax.firstOrNull() ?: 0.0,
        d.tmin.firstOrNull() ?: 0.0,
        d.precipSum.firstOrNull() ?: 0.0
    )
    if (alerts.isEmpty()) {
        Text("SIN AVISOS ACTIVOS", style = AtmosType.label.copy(color = AtmosColors.Muted))
    } else {
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, AtmosColors.Warn)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            alerts.forEach { Text(it, style = AtmosType.title.copy(color = AtmosColors.Warn)) }
        }
    }
}

@Composable
private fun Footer(s: com.atmos.weather.ui.viewmodel.UiState) {
    val label = if (s.online) "DATOS EN VIVO" else "SIN RED"
    val time = if (s.lastFetchTs > 0) java.text.SimpleDateFormat("HH:mm", java.util.Locale("es"))
        .format(java.util.Date(s.lastFetchTs)) else "--:--"
    Column(Modifier.padding(top = 24.dp, bottom = 48.dp)) {
        Text("ATMOS//OS · OPEN-METEO", style = AtmosType.label.copy(color = AtmosColors.Muted))
        Text("$label · CACHÉ $time", style = AtmosType.label.copy(color = AtmosColors.Muted))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSheet(s: com.atmos.weather.ui.viewmodel.UiState, vm: AtmosViewModel) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("UBICACIONES", style = AtmosType.title.copy(color = AtmosColors.Accent))
        BasicTextField(
            value = s.query,
            onValueChange = { vm.onQueryChanged(it) },
            textStyle = AtmosType.mono.copy(color = AtmosColors.Text),
            cursorBrush = SolidColor(AtmosColors.Accent),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().border(1.dp, AtmosColors.Line).padding(10.dp),
            decorationBox = { inner ->
                if (s.query.isEmpty()) Text("Buscar ciudad…", style = AtmosType.mono.copy(color = AtmosColors.Muted))
                inner()
            }
        )
        Row(
            Modifier.fillMaxWidth().background(AtmosColors.Accent).clickable { vm.useGps() }.padding(12.dp),
            horizontalArrangement = Arrangement.Center
        ) { Text("USAR MI UBICACIÓN (GPS)", style = AtmosType.title.copy(color = AtmosColors.Bg)) }

        if (s.results.isNotEmpty()) {
            s.results.forEach { r ->
                Row(
                    Modifier.fillMaxWidth().clickable { vm.pickGeo(r) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(r.name, style = AtmosType.numMedium.copy(color = AtmosColors.Text))
                        Text(listOfNotNull(r.admin1, r.country).joinToString(" · "), style = AtmosType.label.copy(color = AtmosColors.Muted))
                    }
                }
            }
        }
        if (s.saved.isNotEmpty()) {
            Text("CIUDADES GUARDADAS", style = AtmosType.title.copy(color = AtmosColors.Muted))
            s.saved.forEach { c ->
                Row(
                    Modifier.fillMaxWidth().clickable { vm.pickLocation(c) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = AtmosType.numMedium.copy(color = AtmosColors.Text))
                        Text(c.region, style = AtmosType.label.copy(color = AtmosColors.Muted))
                    }
                    Box(Modifier.clickable { vm.removeSaved(c) }.padding(8.dp)) {
                        Text("×", style = AtmosType.title.copy(color = AtmosColors.Warn))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
