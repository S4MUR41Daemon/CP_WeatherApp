package com.atmos.weather

import android.app.Application
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.atmos.weather.data.api.OpenMeteoClient
import com.atmos.weather.data.api.buildHttpClient
import com.atmos.weather.data.cache.PrefsStore
import com.atmos.weather.data.cache.WeatherCache
import com.atmos.weather.data.location.LocationProvider
import com.atmos.weather.data.repository.WeatherRepository
import com.atmos.weather.work.RefreshScheduler

class AtmosApp : Application(), Configuration.Provider, ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        RefreshScheduler.schedule(this, 60)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    /** Coil reutiliza el OkHttpClient (User-Agent propio para OSM) y cachea teselas en disco. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(container.http)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.15).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("tiles")).maxSizeBytes(40L * 1024 * 1024).build() }
        .respectCacheHeaders(false)
        .crossfade(false)
        .build()
}

class AppContainer(app: Application) {
    val http = buildHttpClient(app.cacheDir)
    val client = OpenMeteoClient(http)
    val cache = WeatherCache(app)
    val prefs = PrefsStore(app)
    val location = LocationProvider(app)
    val repository = WeatherRepository(client, cache)
}
