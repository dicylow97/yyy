package com.dicylow.launcher

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppInfo(val label: String, val packageName: String)

/** Lista todas las apps que aparecen en el cajón del sistema, ordenadas por nombre. */
fun loadApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .distinctBy { it.activityInfo.packageName }
        .map { AppInfo(label = it.loadLabel(pm).toString(), packageName = it.activityInfo.packageName) }
        .sortedBy { it.label.lowercase() }
}

/** Carga el fondo reduciendo la resolución para no gastar memoria en el teléfono. */
fun loadWallpaper(context: Context, uri: Uri): ImageBitmap? {
    return try {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val maxSide = 1600
        var sample = 1
        while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

/** Caché de iconos pequeños para que el scroll sea fluido. */
object IconCache {
    private val cache = LruCache<String, ImageBitmap>(150)

    fun get(context: Context, packageName: String): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        return try {
            val bitmap = context.packageManager
                .getApplicationIcon(packageName)
                .toBitmap(96, 96)
                .asImageBitmap()
            cache.put(packageName, bitmap)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
