package com.atmos.weather.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.atmos.weather.data.cache.SavedCity
import com.atmos.weather.ui.components.*
import com.atmos.weather.ui.sections.*
import com.atmos.weather.ui.theme.AtmosColors
import com.atmos.weather.ui.theme.AtmosType
import com.atmos.weather.ui.theme.ChromaRed
import com.atmos.weather.ui.viewmodel.AtmosViewModel
import com.atmos.weather.ui.viewmodel.UiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val SECTIONS = listOf("AHORA", "HORAS", "14 DÍAS", "VIENTO", "AIRE", "POLEN", "SOL/LUNA", "RADAR")

/** Índice del primer panel de sección dentro del LazyColumn (cabecera, nav y avisos van antes). */
private const val FIRST_SECTION_INDEX = 3

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AtmosScreen(vm: AtmosViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val navOffsetPx = with(density) { 60.dp.roundToPx() }
    val w = s.weather

    val active by remember {
        derivedStateOf { (listState.firstVisibleItemIndex - FIRST_SECTION_INDEX + 1).coerceIn(0, SECTIONS.lastIndex) }
    }

    Box(Modifier.fillMaxSize().background(AtmosColors.Bg)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                start = 14.dp, end = 14.dp, top = 6.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
            )
        ) {
            item(key = "header", contentType = "header") {
                HeroHeader(
                    loc = s.location, loading = s.loading, rainIntensity = w?.rainIntensity ?: 0.0,
                    rainEnabled = s.rainEnabled, glitchSeed = s.glitchSeed,
                    onSync = vm::refresh, onCity = vm::openSheet
                )
            }
            stickyHeader(key = "nav", contentType = "nav") {
                SectionNav(active = active, enabled = w != null) { i ->
                    scope.launch { listState.animateScrollToItem(FIRST_SECTION_INDEX + i, -navOffsetPx) }
                }
            }
            item(key = "status", contentType = "status") { StatusBand(w?.alerts, s.online, s.loading) }

            if (w == null) {
                item(key = "loading") { LoadingPanel(s.loading) }
            } else {
                item(key = "now") { SectionNow(w.now) }
                item(key = "hours") { SectionHours(w.hours, s.range, vm::setRange) }
                item(key = "days") { SectionDays(w.days, s.openDay, vm::toggleDay) }
                item(key = "wind") { SectionWind(w.wind) }
                item(key = "air") { SectionAir(w.air) }
                item(key = "pollen") { SectionPollen(w.pollen) }
                item(key = "sun") { SectionSunMoon(w.sunMoon) }
                item(key = "radar") { SectionRadar(w.radar, s.location) }
            }
            item(key = "footer") { Footer(s.online, s.lastFetchTs, s.rainEnabled, vm::setRainEnabled) }
        }

        ScanlineOverlay(Modifier.fillMaxSize())

        if (s.sheetOpen) {
            ModalBottomSheet(
                onDismissRequest = vm::closeSheet,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = AtmosColors.Panel,
                scrimColor = AtmosColors.Bg.copy(alpha = 0.7f),
                shape = CutCornerShape(topStart = 22.dp),
                dragHandle = null
            ) {
                LocationSheet(s, vm)
            }
        }
    }
}

@Composable
private fun HeroHeader(
    loc: SavedCity?,
    loading: Boolean,
    rainIntensity: Double,
    rainEnabled: Boolean,
    glitchSeed: Int,
    onSync: () -> Unit,
    onCity: () -> Unit
) {
    Box(Modifier.fillMaxWidth()) {
        DigitalRain(rainIntensity = rainIntensity, enabled = rainEnabled, modifier = Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(CutCornerShape(topStart = 6.dp))
                        .background(AtmosColors.Red)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) { Text("ATMOS//OS", style = AtmosType.label.copy(color = AtmosColors.Bg)) }
                Spacer(Modifier.width(8.dp))
                val gpsLbl = when {
                    loc == null -> "BUSCANDO SEÑAL"
                    loc.gps -> "GPS · FIJADO"
                    loc.manual -> "UBICACIÓN MANUAL"
                    else -> "SIN GPS · POR DEFECTO"
                }
                Box(Modifier.size(6.dp).background(if (loc?.gps == true) AtmosColors.Cyan else AtmosColors.Yellow))
                Spacer(Modifier.width(6.dp))
                Text(gpsLbl, style = AtmosType.label.copy(color = AtmosColors.Muted))
                Spacer(Modifier.weight(1f))
                if (loc != null) {
                    val coords = "%.2f°%s %.2f°%s".format(
                        Locale.US, abs(loc.lat), if (loc.lat >= 0) "N" else "S", abs(loc.lon), if (loc.lon >= 0) "E" else "O"
                    )
                    Text(coords, style = AtmosType.label.copy(color = AtmosColors.Muted))
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                loc?.name?.uppercase() ?: "LOCALIZANDO…",
                style = AtmosType.city.copy(color = AtmosColors.Yellow, shadow = ChromaRed),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (loc != null && loc.region.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(loc.region.uppercase(), style = AtmosType.label.copy(color = AtmosColors.RedSoft))
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CyberButton("⌖ CIUDAD", color = AtmosColors.Red, onClick = onCity)
                CyberButton(if (loading) "SYNC ···" else "⟳ SYNC", filled = true, onClick = onSync)
            }
        }
        if (glitchSeed != 0) GlitchOverlay(glitchSeed, Modifier.matchParentSize())
    }
}

@Composable
private fun SectionNav(active: Int, enabled: Boolean, onPick: (Int) -> Unit) {
    val rowState = rememberLazyListState()
    LaunchedEffect(active) { rowState.animateScrollToItem((active - 1).coerceAtLeast(0)) }
    LazyRow(
        state = rowState,
        modifier = Modifier
            .fillMaxWidth()
            .background(AtmosColors.Bg)
            .drawBehind {
                drawLine(AtmosColors.LineR, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1.dp.toPx())
            }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(SECTIONS) { i, label ->
            val on = enabled && i == active
            val shape = CutCornerShape(topStart = 7.dp, bottomEnd = 7.dp)
            Box(
                Modifier
                    .clip(shape)
                    .background(if (on) AtmosColors.Yellow else AtmosColors.Panel)
                    .border(1.dp, if (on) AtmosColors.Yellow else AtmosColors.LineR, shape)
                    .clickable(enabled = enabled) { onPick(i) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(label, style = AtmosType.label.copy(color = if (on) AtmosColors.Bg else AtmosColors.Text))
            }
        }
    }
}

@Composable
private fun StatusBand(alerts: List<String>?, online: Boolean, loading: Boolean) {
    if (alerts.isNullOrEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(8.dp).background(if (online) AtmosColors.Cyan else AtmosColors.Red))
            Spacer(Modifier.width(8.dp))
            val txt = when {
                loading && alerts == null -> "ESTABLECIENDO ENLACE…"
                !online -> "SIN RED · MOSTRANDO CACHÉ"
                else -> "SISTEMA NOMINAL // SIN AVISOS ACTIVOS"
            }
            Text(txt, style = AtmosType.label.copy(color = if (online) AtmosColors.Muted else AtmosColors.RedSoft))
        }
        return
    }
    Column(Modifier.fillMaxWidth().neonFrame(AtmosColors.Red, cut = 12.dp, fill = AtmosColors.Red.copy(alpha = 0.08f))) {
        Box(Modifier.fillMaxWidth().height(8.dp).hazardStripes(AtmosColors.Red.copy(alpha = 0.7f)))
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("⚠ AVISO // NCPD WEATHER ALERT", style = AtmosType.label.copy(color = AtmosColors.Yellow))
            alerts.forEach { Text(it, style = AtmosType.title.copy(color = AtmosColors.Red)) }
        }
    }
}

@Composable
private fun LoadingPanel(loading: Boolean) {
    NeonPanel("00", "ENLACE", color = AtmosColors.Yellow) {
        Text(
            if (loading) "// ENLAZANDO CON SATÉLITE…" else "// SIN DATOS. PULSA SYNC O ELIGE CIUDAD",
            style = AtmosType.mono.copy(color = AtmosColors.Muted)
        )
        Spacer(Modifier.height(10.dp))
        SegBar(12, if (loading) 7 else 0, AtmosColors.Yellow)
    }
}

@Composable
private fun Footer(online: Boolean, lastFetchTs: Long, rainEnabled: Boolean, onRain: (Boolean) -> Unit) {
    val label = if (online) "DATOS EN VIVO" else "SIN RED"
    val time = if (lastFetchTs > 0) SimpleDateFormat("HH:mm", Locale("es")).format(Date(lastFetchTs)) else "--:--"
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LLUVIA DIGITAL", style = AtmosType.label.copy(color = AtmosColors.Muted))
            Spacer(Modifier.width(10.dp))
            val shape = CutCornerShape(topStart = 5.dp, bottomEnd = 5.dp)
            Box(
                Modifier
                    .clip(shape)
                    .background(if (rainEnabled) AtmosColors.Yellow else AtmosColors.Panel)
                    .border(1.dp, if (rainEnabled) AtmosColors.Yellow else AtmosColors.LineR, shape)
                    .clickable { onRain(!rainEnabled) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(if (rainEnabled) "ON" else "OFF", style = AtmosType.label.copy(color = if (rainEnabled) AtmosColors.Bg else AtmosColors.Text))
            }
        }
        Text("ATMOS//OS v2 · OPEN-METEO · RAINVIEWER · © OSM", style = AtmosType.label.copy(color = AtmosColors.Dim))
        Text("$label · SYNC $time", style = AtmosType.label.copy(color = AtmosColors.Muted))
    }
}

@Composable
private fun LocationSheet(s: UiState, vm: AtmosViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader("⌖", "UBICACIONES", AtmosColors.Red)
        BasicTextField(
            value = s.query,
            onValueChange = vm::onQueryChanged,
            textStyle = AtmosType.monoBig.copy(color = AtmosColors.Text),
            cursorBrush = SolidColor(AtmosColors.Yellow),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .neonFrame(AtmosColors.Yellow, cut = 10.dp, fill = AtmosColors.Bg)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            decorationBox = { inner ->
                Box {
                    if (s.query.isEmpty()) Text("> BUSCAR CIUDAD_", style = AtmosType.monoBig.copy(color = AtmosColors.Muted))
                    inner()
                }
            }
        )
        CyberButton("⌖ USAR MI UBICACIÓN (GPS)", Modifier.fillMaxWidth(), filled = true, onClick = vm::useGps)

        if (s.searching) Text("// CONSULTANDO RED…", style = AtmosType.label.copy(color = AtmosColors.Cyan))
        else if (s.query.trim().length >= 2 && s.results.isEmpty()) Text("SIN RESULTADOS", style = AtmosType.label.copy(color = AtmosColors.Muted))

        s.results.forEach { r ->
            CityRow(r.name, listOfNotNull(r.admin1, r.country).joinToString(" · "), onClick = { vm.pickGeo(r) })
        }
        if (s.saved.isNotEmpty()) {
            Text("CIUDADES GUARDADAS", style = AtmosType.label.copy(color = AtmosColors.RedSoft))
            s.saved.forEach { c ->
                CityRow(c.name, c.region, onClick = { vm.pickLocation(c) }, onRemove = { vm.removeSaved(c) })
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun CityRow(name: String, region: String, onClick: () -> Unit, onRemove: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(AtmosColors.PanelHi)
            .drawBehind { drawRect(AtmosColors.Yellow, Offset.Zero, androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height)) }
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(name.uppercase(), style = AtmosType.title.copy(color = AtmosColors.Text), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (region.isNotEmpty()) Text(region.uppercase(), style = AtmosType.label.copy(color = AtmosColors.Muted), maxLines = 1)
        }
        if (onRemove != null) {
            Box(Modifier.clickable(onClick = onRemove).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("✕", style = AtmosType.title.copy(color = AtmosColors.Red))
            }
        }
    }
}
