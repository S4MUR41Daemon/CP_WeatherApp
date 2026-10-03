package com.atmos.weather.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.atmos.weather.AtmosApp
import com.atmos.weather.data.api.GeoResult
import com.atmos.weather.data.cache.SavedCity
import com.atmos.weather.data.repository.WeatherBundle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val bundle: WeatherBundle? = null,
    val location: SavedCity? = null,
    val loading: Boolean = true,
    val sheetOpen: Boolean = false,
    val query: String = "",
    val results: List<GeoResult> = emptyList(),
    val saved: List<SavedCity> = emptyList(),
    val range: Int = 12,
    val openDay: Int = -1,
    val glitch: Boolean = false,
    val rainEnabled: Boolean = true,
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

    init {
        viewModelScope.launch {
            container.prefs.rainEnabled.collect { r -> _state.update { it.copy(rainEnabled = r) } }
        }
        viewModelScope.launch {
            container.prefs.savedCities.collect { s -> _state.update { it.copy(saved = s) } }
        }
        viewModelScope.launch { initialLoad() }
    }

    private suspend fun initialLoad() {
        val cached = container.repository.loadCached()
        if (cached != null) {
            _state.update {
                it.copy(bundle = cached.bundle, location = cached.location, loading = false, lastFetchTs = cached.bundle.fetchedAt)
            }
            if (System.currentTimeMillis() - cached.bundle.fetchedAt < 30 * 60 * 1000L) {
                return
            }
            refresh(cached.location)
            return
        }
        val gps = container.location.getCurrent()
        val loc = gps?.let { SavedCity(it.name, it.lat, it.lon, it.region, gps = true, manual = false) } ?: DEFAULT_LOCATION
        _state.update { it.copy(location = loc) }
        refresh(loc)
    }

    fun refresh() { state.value.location?.let { refresh(it) } }
    private fun refresh(loc: SavedCity) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, glitch = true) }
            val b = container.repository.refresh(loc)
            val online = b.forecast?.current != null
            _state.update { it.copy(bundle = b, location = loc, loading = false, online = online, lastFetchTs = b.fetchedAt) }
            delay(380)
            _state.update { it.copy(glitch = false) }
        }
    }

    fun openSheet() { _state.update { it.copy(sheetOpen = true) } }
    fun closeSheet() { _state.update { it.copy(sheetOpen = false, query = "", results = emptyList()) } }
    fun setRange(r: Int) { _state.update { it.copy(range = r) } }
    fun toggleDay(i: Int) { _state.update { s -> s.copy(openDay = if (s.openDay == i) -1 else i) } }

    fun onQueryChanged(q: String) {
        _state.update { it.copy(query = q) }
        searchJob?.cancel()
        if (q.trim().length < 2) { _state.update { it.copy(results = emptyList()) }; return }
        searchJob = viewModelScope.launch {
            delay(350)
            val r = container.client.search(q)
            _state.update { it.copy(results = r) }
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
        _state.update { it.copy(sheetOpen = false, query = "", results = emptyList(), location = loc, bundle = null, loading = true) }
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
            val loc = g?.let { SavedCity(it.name, it.lat, it.lon, it.region, gps = true, manual = false) } ?: DEFAULT_LOCATION
            _state.update { it.copy(location = loc) }
            refresh(loc)
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
