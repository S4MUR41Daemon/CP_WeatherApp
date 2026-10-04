package com.atmos.weather.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.atmos.weather.AtmosApp
import com.atmos.weather.data.api.GeoResult
import com.atmos.weather.data.cache.SavedCity
import com.atmos.weather.data.repository.WeatherBundle
import com.atmos.weather.domain.WeatherUi
import com.atmos.weather.domain.toUi
import com.atmos.weather.ui.theme.AtmosColors
import com.atmos.weather.ui.theme.Palettes
import com.atmos.weather.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class UiState(
    val weather: WeatherUi? = null,
    val location: SavedCity? = null,
    val loading: Boolean = true,
    val sheetOpen: Boolean = false,
    val query: String = "",
    val searching: Boolean = false,
    val results: List<GeoResult> = emptyList(),
    val saved: List<SavedCity> = emptyList(),
    val range: Int = 12,
    val openDay: Int = -1,
    /** != 0 mientras dura el glitch de sincronización. */
    val glitchSeed: Int = 0,
    val rainEnabled: Boolean = true,
    val themeId: String = Palettes.NIGHT_CITY.id,
    val online: Boolean = true,
    val lastFetchTs: Long = 0L
)

val DEFAULT_LOCATION = SavedCity(
    name = "Zaragoza", lat = 41.6488, lon = -0.8891,
    region = "Aragón · España", gps = false, manual = false
)

class AtmosViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as AtmosApp).container
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var searchJob: Job? = null
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            container.prefs.rainEnabled.collect { r -> _state.update { it.copy(rainEnabled = r) } }
        }
        viewModelScope.launch {
            container.prefs.themeId.collect { id ->
                val p = Palettes.byId(id)
                AtmosColors.palette = p
                _state.update { it.copy(themeId = p.id) }
            }
        }
        viewModelScope.launch {
            container.prefs.savedCities.collect { s -> _state.update { it.copy(saved = s) } }
        }
        viewModelScope.launch { initialLoad() }
    }

    private suspend fun initialLoad() {
        val cached = container.repository.loadCached()
        if (cached != null) {
            val ui = withContext(Dispatchers.Default) { cached.bundle.toUi() }
            _state.update {
                it.copy(weather = ui, location = cached.location, loading = false, lastFetchTs = cached.bundle.fetchedAt)
            }
            if (System.currentTimeMillis() - cached.bundle.fetchedAt > 30 * 60 * 1000L) refresh(cached.location)
            return
        }
        val gps = container.location.getCurrent()
        val loc = gps?.let { SavedCity(it.name, it.lat, it.lon, it.region, gps = true, manual = false) } ?: DEFAULT_LOCATION
        _state.update { it.copy(location = loc) }
        refresh(loc)
    }

    fun refresh() { state.value.location?.let { refresh(it) } }

    private fun refresh(loc: SavedCity) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, glitchSeed = (1..Int.MAX_VALUE).random()) }
            val b: WeatherBundle = container.repository.refresh(loc)
            val ui = withContext(Dispatchers.Default) { b.toUi() }
            val online = !b.fromMock
            _state.update {
                it.copy(
                    weather = ui ?: it.weather, location = loc, loading = false, online = online,
                    lastFetchTs = if (b.fetchedAt > 0) b.fetchedAt else it.lastFetchTs
                )
            }
            if (online) WidgetUpdater.update(getApplication())
            delay(380)
            _state.update { it.copy(glitchSeed = 0) }
        }
    }

    /** Llamado por la Activity cuando el usuario responde al diálogo de permisos. */
    fun onLocationPermission(granted: Boolean) {
        val loc = state.value.location
        if (granted) {
            if (loc == null || !loc.manual) useGps()
        } else if (loc == null || (!loc.manual && !loc.gps)) {
            openSheet()
        }
    }

    fun openSheet() { _state.update { it.copy(sheetOpen = true) } }
    fun closeSheet() { _state.update { it.copy(sheetOpen = false, query = "", results = emptyList(), searching = false) } }
    fun setRange(r: Int) { _state.update { it.copy(range = r) } }
    fun toggleDay(i: Int) { _state.update { s -> s.copy(openDay = if (s.openDay == i) -1 else i) } }

    fun onQueryChanged(q: String) {
        _state.update { it.copy(query = q) }
        searchJob?.cancel()
        if (q.trim().length < 2) { _state.update { it.copy(results = emptyList(), searching = false) }; return }
        searchJob = viewModelScope.launch {
            delay(350)
            _state.update { it.copy(searching = true) }
            val r = container.client.search(q)
            _state.update { it.copy(results = r, searching = false) }
        }
    }

    fun pickGeo(g: GeoResult) {
        val loc = SavedCity(g.name, g.latitude, g.longitude, listOfNotNull(g.admin1, g.country).joinToString(" · "), gps = false, manual = true)
        pickLocation(loc)
    }

    fun pickLocation(loc: SavedCity) {
        viewModelScope.launch {
            val newList = (listOf(loc) + _state.value.saved.filter { it.name != loc.name }).take(6)
            container.prefs.setSavedCities(newList)
        }
        _state.update { it.copy(sheetOpen = false, query = "", results = emptyList(), location = loc, weather = null, loading = true) }
        refresh(loc)
    }

    fun removeSaved(loc: SavedCity) {
        viewModelScope.launch {
            container.prefs.setSavedCities(_state.value.saved.filter { it.name != loc.name })
        }
    }

    fun useGps() {
        _state.update { it.copy(sheetOpen = false, loading = true) }
        viewModelScope.launch {
            val g = container.location.getCurrent()
            val loc = g?.let { SavedCity(it.name, it.lat, it.lon, it.region, gps = true, manual = false) }
                ?: _state.value.location ?: DEFAULT_LOCATION
            _state.update { it.copy(location = loc) }
            refresh(loc)
        }
    }

    /** Cambia la plantilla: la app se repinta al instante (State) y el widget en segundo plano. */
    fun setTheme(id: String) {
        AtmosColors.palette = Palettes.byId(id)
        _state.update { it.copy(themeId = id) }
        viewModelScope.launch {
            container.prefs.setThemeId(id)
            WidgetUpdater.update(getApplication())
        }
    }

    fun setRainEnabled(v: Boolean) {
        viewModelScope.launch { container.prefs.setRainEnabled(v) }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = AtmosViewModel(app) as T
    }
}
