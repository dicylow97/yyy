package com.dicylow.launcher

import android.app.WallpaperManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

const val PREFS = "launcher_prefs"

// Colores fijos de la interfaz. El acento cambia según el tema elegido.
val BgColor = Color(0xFF0B0B0F)
val SurfaceColor = Color(0xFF14141B)
val CardColor = Color(0xFF1C1C26)
val TextColor = Color(0xFFE5E7EB)
val MutedColor = Color(0xFF6B7280)

enum class ClockStyle { BIG, TERMINAL, MINIMAL }

enum class ClockPos { TOP, CENTER, BOTTOM }

enum class SearchPos { TOP, BOTTOM }

enum class FontChoice {
    MONO, SERIF, SANS, CURSIVE;

    fun toFamily(): FontFamily = when (this) {
        MONO -> FontFamily.Monospace
        SERIF -> FontFamily.Serif
        SANS -> FontFamily.SansSerif
        CURSIVE -> FontFamily.Cursive
    }
}

enum class ThemeChoice(val label: String, val accent: Long) {
    MATRIX("Matrix", 0xFF7CFF9AL),
    AMBAR("Ámbar", 0xFFFFB347L),
    CIAN("Cian", 0xFF5EEAD4L),
    MAGENTA("Magenta", 0xFFF472B6L),
    BLANCO("Blanco", 0xFFE5E7EBL)
}

data class Settings(
    val clockStyle: ClockStyle = ClockStyle.BIG,
    val clockPos: ClockPos = ClockPos.CENTER,
    val clockSize: Int = 72,
    val clockFont: FontChoice = FontChoice.MONO,
    val clockBold: Boolean = false,
    val showSeconds: Boolean = false,
    val showDate: Boolean = true,
    val is24h: Boolean = true,
    val searchPos: SearchPos = SearchPos.TOP,
    val prompt: String = ">",
    val uiFont: FontChoice = FontChoice.MONO,
    val theme: ThemeChoice = ThemeChoice.MATRIX,
    val dim: Float = 0.4f
) {
    fun save(sp: SharedPreferences) {
        sp.edit()
            .putString("clock_style", clockStyle.name)
            .putString("clock_pos", clockPos.name)
            .putInt("clock_size", clockSize)
            .putString("clock_font", clockFont.name)
            .putBoolean("clock_bold", clockBold)
            .putBoolean("show_seconds", showSeconds)
            .putBoolean("show_date", showDate)
            .putBoolean("is24h", is24h)
            .putString("search_pos", searchPos.name)
            .putString("prompt", prompt)
            .putString("ui_font", uiFont.name)
            .putString("theme", theme.name)
            .putFloat("dim", dim)
            .apply()
    }

    companion object {
        fun load(sp: SharedPreferences): Settings {
            val d = Settings()
            return Settings(
                clockStyle = sp.enumOr("clock_style", d.clockStyle),
                clockPos = sp.enumOr("clock_pos", d.clockPos),
                clockSize = sp.getInt("clock_size", d.clockSize),
                clockFont = sp.enumOr("clock_font", d.clockFont),
                clockBold = sp.getBoolean("clock_bold", d.clockBold),
                showSeconds = sp.getBoolean("show_seconds", d.showSeconds),
                showDate = sp.getBoolean("show_date", d.showDate),
                is24h = sp.getBoolean("is24h", d.is24h),
                searchPos = sp.enumOr("search_pos", d.searchPos),
                prompt = sp.getString("prompt", d.prompt) ?: d.prompt,
                uiFont = sp.enumOr("ui_font", d.uiFont),
                theme = sp.enumOr("theme", d.theme),
                dim = sp.getFloat("dim", d.dim)
            )
        }
    }
}

private inline fun <reified T : Enum<T>> SharedPreferences.enumOr(key: String, default: T): T {
    val name = getString(key, null) ?: return default
    return enumValues<T>().firstOrNull { it.name == name } ?: default
}

/**
 * Pone la imagen como fondo del SISTEMA. Así el launcher y la vista de apps
 * abiertas usan el mismo fondo, en vez del fondo del launcher anterior.
 */
fun applyWallpaper(context: Context, uri: Uri): Boolean {
    return try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        var sample = 1
        while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return false

        WallpaperManager.getInstance(context).setBitmap(bitmap)
        bitmap.recycle()
        true
    } catch (e: Exception) {
        false
    }
}

/** Devuelve el fondo del sistema al que trae el teléfono. */
fun clearWallpaper(context: Context): Boolean {
    return try {
        WallpaperManager.getInstance(context).clear()
        true
    } catch (e: Exception) {
        false
    }
}
