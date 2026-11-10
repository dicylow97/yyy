package com.dicylow.launcher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private val settings = mutableStateOf(Settings())

    // Cambia cada vez que pulsas Inicio (onNewIntent)
    private val resetCounter = mutableIntStateOf(0)

    // Cambia cada vez que el launcher aparece, para releer la lista de apps
    private val appsVersion = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings.value = Settings.load(prefs)

        setContent {
            LauncherScreen(
                settings = settings.value,
                onSave = ::saveSettings,
                resetKey = resetCounter.intValue,
                appsKey = appsVersion.intValue
            )
        }
    }

    override fun onStart() {
        super.onStart()
        appsVersion.intValue++
    }

    override fun onStop() {
        super.onStop()
        // Al salir del launcher se libera la caché de iconos: no queda nada guardado en segundo plano
        IconCache.clear()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        IconCache.clear()
    }

    /** Solo cambia los ajustes activos si el disco confirmó el guardado. */
    private fun saveSettings(newSettings: Settings): Boolean {
        val ok = newSettings.save(prefs)
        if (ok) settings.value = newSettings
        return ok
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        resetCounter.intValue++
    }
}
