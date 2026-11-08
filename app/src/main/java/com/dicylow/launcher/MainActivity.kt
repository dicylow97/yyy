package com.dicylow.launcher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf

class MainActivity : ComponentActivity() {

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    // Cambia cada vez que el usuario pulsa "Inicio" y el launcher vuelve a primer plano
    private val resetCounter = mutableIntStateOf(0)

    // Cambia cada vez que el launcher se muestra, para recargar la lista de apps
    private val appsVersion = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LauncherScreen(
                prefs = prefs,
                resetKey = resetCounter.intValue,
                appsKey = appsVersion.intValue
            )
        }
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
