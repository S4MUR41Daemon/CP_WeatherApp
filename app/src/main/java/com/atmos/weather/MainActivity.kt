package com.atmos.weather

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.atmos.weather.ui.screen.AtmosScreen
import com.atmos.weather.ui.viewmodel.AtmosViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Atmos)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        // Misma instancia que obtiene viewModel() dentro de AtmosScreen (mismo ViewModelStore).
        val vm = ViewModelProvider(this, AtmosViewModel.Factory(application))[AtmosViewModel::class.java]

        val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
            vm.onLocationPermission(res.values.any { it })
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }

        setContent { AtmosScreen(vm) }
    }
}
