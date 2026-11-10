package com.dicylow.launcher

import android.app.WallpaperManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

const val PREFS = "launcher_prefs"

// Colores fijos de la interfaz
val BgColor = Color(0xFF0B0B0F)
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

/** Cada tema tiene un color de acento y un color de superficie (fondo del buscador). */
enum class ThemeChoice(val label: String, val accent: Long, val surface: Long) {
    MATRIX("Matrix", 0xFF7CFF9AL, 0xFF0D1A12L),
    AMBAR("Ámbar", 0xFFFFB347L, 0xFF1A1407L),
    CIAN("Cian", 0xFF5EEAD4L, 0xFF071A1CL),
    MAGENTA("Magenta", 0xFFF472B6L, 0xFF1C0A16L),
    VIOLETA("Violeta", 0xFFB388FFL, 0xFF140E22L),
    ROJO("Rojo", 0xFFFF6B6BL, 0xFF1C0C0CL),
    AZUL("Azul hielo", 0xFF7DD3FCL, 0xFF09161FL),
    NORD("Nord", 0xFF88C0D0L, 0xFF1B2129L),
    BLANCO("Blanco", 0xFFF3F4F6L, 0xFF16161DL)
}

/** Colores que puedes elegir para el reloj. 0 = usar el color del tema. */
val CLOCK_COLORS: List<Pair<Long, String>> = listOf(
    0L to "Tema",
    0xFFFFFFFFL to "Blanco",
    0xFF7CFF9AL to "Verde",
    0xFFFFB000L to "Ámbar",
    0xFFFF7A18L to "Naranja",
    0xFFFF5252L to "Rojo",
    0xFFFF5FA2L to "Rosa",
    0xFFB388FFL to "Violeta",
    0xFF7DD3FCL to "Azul",
    0xFF5EEAD4L to "Cian"
)

data class Settings(
    val clockStyle: ClockStyle = ClockStyle.BIG,
    val clockPos: ClockPos = ClockPos.CENTER,
    val clockSize: Int = 72,
    val clockFont: FontChoice = FontChoice.MONO,
    val clockColor: Long = 0L,
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
    /** commit() escribe en disco de forma síncrona: si devuelve true, el cambio quedó guardado. */
    fun save(sp: SharedPreferences): Boolean {
        return sp.edit()
            .putString("clock_style", clockStyle.name)
            .putString("clock_pos", clockPos.name)
            .putInt("clock_size", clockSize)
            .putString("clock_font", clockFont.name)
            .putLong("clock_color", clockColor)
            .putBoolean("clock_bold", clockBold)
            .putBoolean("show_seconds", showSeconds)
            .putBoolean("show_date", showDate)
            .putBoolean("is24h", is24h)
            .putString("search_pos", searchPos.name)
            .putString("prompt", prompt)
            .putString("ui_font", uiFont.name)
            .putString("theme", theme.name)
            .putFloat("dim", dim)
            .commit()
    }

    companion object {
        fun load(sp: SharedPreferences): Settings {
            val d = Settings()
            return Settings(
                clockStyle = sp.enumOr("clock_style", d.clockStyle),
                clockPos = sp.enumOr("clock_pos", d.clockPos),
                clockSize = sp.getInt("clock_size", d.clockSize),
                clockFont = sp.enumOr("clock_font", d.clockFont),
                clockColor = sp.getLong("clock_color", d.clockColor),
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

/** Pone la imagen como fondo del sistema, para que el launcher y la vista de apps usen el mismo. */
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

/** Devuelve el fondo original del teléfono. */
fun clearWallpaper(context: Context): Boolean {
    return try {
        WallpaperManager.getInstance(context).clear()
        true
    } catch (e: Exception) {
        false
    }
}
