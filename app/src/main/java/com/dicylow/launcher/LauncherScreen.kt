package com.dicylow.launcher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

const val PREFS = "launcher_prefs"
private const val KEY_WALLPAPER = "wallpaper"
private const val KEY_DIM = "dim"

private val BgColor = Color(0xFF0B0B0F)
private val SurfaceColor = Color(0xFF14141B)
private val AccentColor = Color(0xFF7CFF9A)
private val TextColor = Color(0xFFE5E7EB)
private val MutedColor = Color(0xFF6B7280)

@Composable
fun LauncherScreen(prefs: SharedPreferences, resetKey: Int, appsKey: Int) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var query by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var wallpaperUri by remember { mutableStateOf(prefs.getString(KEY_WALLPAPER, null)) }
    var dim by remember { mutableFloatStateOf(prefs.getFloat(KEY_DIM, 0.4f)) }

    // Al volver a la pantalla de inicio, el buscador se vacía
    LaunchedEffect(resetKey) { query = "" }

    // Se recarga la lista cuando se instala o desinstala una app (al volver al launcher)
    val apps by produceState<List<AppInfo>>(initialValue = emptyList(), key1 = appsKey) {
        value = withContext(Dispatchers.IO) { loadApps(context) }
    }

    val wallpaper by produceState<ImageBitmap?>(initialValue = null, key1 = wallpaperUri) {
        val uriText = wallpaperUri
        value = if (uriText == null) {
            null
        } else {
            withContext(Dispatchers.IO) { loadWallpaper(context, Uri.parse(uriText)) }
        }
    }

    val time by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(15_000)
        }
    }

    val pickWallpaper = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                // Algunos proveedores no dan permiso persistente; igual funciona mientras la app esté abierta
            }
            prefs.edit().putString(KEY_WALLPAPER, uri.toString()).apply()
            wallpaperUri = uri.toString()
        }
    }

    val results = remember(query, apps) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            emptyList<AppInfo>()
        } else {
            apps
                .filter { it.label.lowercase().contains(q) }
                .sortedWith(
                    compareBy<AppInfo>(
                        { !it.label.lowercase().startsWith(q) },
                        { it.label.lowercase() }
                    )
                )
        }
    }

    fun launch(app: AppInfo) {
        focusManager.clearFocus()
        context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AccentColor,
            background = BgColor,
            surface = SurfaceColor,
            onSurface = TextColor,
            onBackground = TextColor
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().background(BgColor)) {

            wallpaper?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                // Buscador arriba, estilo rofi/i3
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("> buscar app", color = MutedColor) },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = { results.firstOrNull()?.let { launch(it) } }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = TextColor,
                            unfocusedTextColor = TextColor,
                            focusedContainerColor = SurfaceColor,
                            unfocusedContainerColor = SurfaceColor,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = AccentColor
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = { showSettings = true }) {
                        Text("⚙", color = AccentColor, fontSize = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (query.isBlank()) {
                    // Sin búsqueda: solo el reloj en el centro, nada de cajón de apps
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = time.format(DateTimeFormatter.ofPattern("HH:mm")),
                                color = TextColor,
                                fontSize = 72.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = time.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.forLanguageTag("es"))),
                                color = MutedColor,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "escribe para buscar una app",
                                color = MutedColor,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    if (results.isEmpty()) {
                        Text(
                            text = "sin resultados",
                            color = MutedColor,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(results, key = { it.packageName }) { app ->
                            AppRow(app = app, onClick = { launch(app) })
                        }
                    }
                }
            }

            if (showSettings) {
                AlertDialog(
                    onDismissRequest = { showSettings = false },
                    containerColor = SurfaceColor,
                    title = { Text("Ajustes", color = TextColor) },
                    text = {
                        Column {
                            Text(
                                text = "Oscurecer fondo: ${(dim * 100).roundToInt()}%",
                                color = TextColor
                            )
                            Slider(
                                value = dim,
                                onValueChange = {
                                    dim = it
                                    prefs.edit().putFloat(KEY_DIM, it).apply()
                                },
                                valueRange = 0f..0.9f
                            )
                            TextButton(onClick = { pickWallpaper.launch(arrayOf("image/*")) }) {
                                Text("Elegir fondo de pantalla", color = AccentColor)
                            }
                            TextButton(onClick = {
                                prefs.edit().remove(KEY_WALLPAPER).apply()
                                wallpaperUri = null
                            }) {
                                Text("Quitar fondo", color = TextColor)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSettings = false }) {
                            Text("Cerrar", color = AccentColor)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AppRow(app: AppInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(packageName = app.packageName)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = app.label,
            color = TextColor,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AppIcon(packageName: String) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = null, key1 = packageName) {
        value = withContext(Dispatchers.IO) { IconCache.get(context, packageName) }
    }
    Box(modifier = Modifier.size(32.dp)) {
        icon?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
