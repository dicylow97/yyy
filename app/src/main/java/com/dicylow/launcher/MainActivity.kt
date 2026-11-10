package com.dicylow.launcher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private val settings = mutableStateOf(Settings())

    // Cambia cada vez que pulsas Inicio (onNewIntent)
    private val resetCounter = mutableIntStateOf(0)

    // Cambia cada vez que el launcher vuelve a primer plano, para recargar las apps
    private val appsVersion = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dibujamos detrás de la barra de estado y de navegación.
        // El padding lo gestiona Compose con WindowInsets.safeDrawing.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        settings.value = Settings.load(prefs)

        setContent {
            LauncherScreen(
                settings = settings.value,
                onChange = ::updateSettings,
                resetKey = resetCounter.intValue,
                appsKey = appsVersion.intValue
            )
        }
    }

    private fun updateSettings(newSettings: Settings) {
        settings.value = newSettings
        newSettings.save(prefs)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        resetCounter.intValue++
    }

    override fun onResume() {
        super.onResume()
        appsVersion.intValue++
    }
}
