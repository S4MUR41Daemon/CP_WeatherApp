package com.atmos.weather

import android.app.Application
import androidx.work.Configuration
import com.atmos.weather.data.api.OpenMeteoClient
import com.atmos.weather.data.cache.PrefsStore
import com.atmos.weather.data.cache.WeatherCache
import com.atmos.weather.data.location.LocationProvider
import com.atmos.weather.data.repository.WeatherRepository
import com.atmos.weather.work.RefreshScheduler

class AtmosApp : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        RefreshScheduler.schedule(this, 60)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}

class AppContainer(app: Application) {
    val client = OpenMeteoClient()
    val cache = WeatherCache(app)
    val prefs = PrefsStore(app)
    val location = LocationProvider(app)
    val repository = WeatherRepository(client, cache)
}
