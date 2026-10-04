package com.atmos.weather.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

data class GpsLocation(val lat: Double, val lon: Double, val name: String, val region: String)

class LocationProvider(private val context: Context) {

    fun hasPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    suspend fun getCurrent(): GpsLocation? {
        if (!hasPermission()) return null
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val req = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setMaxUpdateAgeMillis(15 * 60 * 1000)
                .build()
            val loc = client.getCurrentLocation(req, null).await() ?: return null
            val (name, region) = withContext(Dispatchers.IO) { reverseGeocode(loc.latitude, loc.longitude) }
            GpsLocation(loc.latitude, loc.longitude, name, region)
        } catch (_: Throwable) { null }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(lat: Double, lon: Double): Pair<String, String> {
        return try {
            val gc = Geocoder(context, Locale("es"))
            val list = gc.getFromLocation(lat, lon, 1).orEmpty()
            val a = list.firstOrNull() ?: return "Tu ubicación" to ""
            val name = a.locality ?: a.subAdminArea ?: a.adminArea ?: "Tu ubicación"
            val region = listOfNotNull(a.adminArea, a.countryName).joinToString(" · ")
            name to region
        } catch (_: Throwable) { "Tu ubicación" to "" }
    }
}
