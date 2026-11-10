package com.dicylow.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun LauncherScreen(
    settings: Settings,
    onChange: (Settings) -> Unit,
    resetKey: Int,
    appsKey: Int
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val accent = Color(settings.theme.accent)
    val uiFont = settings.uiFont.toFamily()

    var query by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Cada vez que pulsas Inicio, el launcher vuelve limpio
    LaunchedEffect(resetKey) {
        query = ""
        showSettings = false
        focusManager.clearFocus()
    }

    val apps by produceState<List<AppInfo>>(initialValue = emptyList(), key1 = appsKey) {
        value = withContext(Dispatchers.IO) { loadApps(context) }
    }

    // El reloj se actualiza cada segundo solo si hace falta (segundos o estilo terminal)
    val now by produceState(
        initialValue = LocalDateTime.now(),
        key1 = settings.showSeconds,
        key2 = settings.clockStyle
    ) {
        val interval = if (settings.showSeconds || settings.clockStyle == ClockStyle.TERMINAL) 1_000L else 15_000L
        while (true) {
            value = LocalDateTime.now()
            delay(interval)
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
        keyboard?.hide()
        context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) }
    }

    // Gesto/botón atrás: primero cierra ajustes, luego el teclado, luego borra la búsqueda.
    BackHandler(enabled = showSettings || focused || query.isNotEmpty()) {
        when {
            showSettings -> { showSettings = false }
            focused -> {
                focusManager.clearFocus()
                keyboard?.hide()
            }
            else -> { query = "" }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent,
            background = BgColor,
            surface = SurfaceColor,
            onSurface = TextColor,
            onBackground = TextColor
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Sin fondo propio: se ve el fondo del sistema (windowShowWallpaper)
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = settings.dim)))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp)
            ) {
                if (settings.searchPos == SearchPos.TOP) {
                    SearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        prompt = settings.prompt,
                        accent = accent,
                        font = uiFont,
                        onFocus = { focused = it },
                        onGo = { results.firstOrNull()?.let { launch(it) } },
                        onSettings = { showSettings = true }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (query.isBlank()) {
                    val interaction = remember { MutableInteractionSource() }
                    val align = when (settings.clockPos) {
                        ClockPos.TOP -> Alignment.TopCenter
                        ClockPos.CENTER -> Alignment.Center
                        ClockPos.BOTTOM -> Alignment.BottomCenter
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clickable(interactionSource = interaction, indication = null) {
                                focusManager.clearFocus()
                                keyboard?.hide()
                            },
                        contentAlignment = align
                    ) {
                        ClockView(settings = settings, now = now, accent = accent, uiFont = uiFont)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (results.isEmpty()) {
                            item {
                                Text("sin resultados", color = MutedColor, fontFamily = uiFont, modifier = Modifier.padding(12.dp))
                            }
                        }
                        items(results, key = { it.packageName }) { app ->
                            AppRow(app = app, font = uiFont, onClick = { launch(app) })
                        }
                    }
                }

                if (settings.searchPos == SearchPos.BOTTOM) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        prompt = settings.prompt,
                        accent = accent,
                        font = uiFont,
                        onFocus = { focused = it },
                        onGo = { results.firstOrNull()?.let { launch(it) } },
                        onSettings = { showSettings = true }
                    )
                }
            }

            if (showSettings) {
                SettingsScreen(
                    settings = settings,
                    onChange = onChange,
                    onClose = { showSettings = false }
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    prompt: String,
    accent: Color,
    font: FontFamily,
    onFocus: (Boolean) -> Unit,
    onGo: () -> Unit,
    onSettings: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).onFocusChanged { onFocus(it.isFocused) },
            singleLine = true,
            placeholder = { Text("$prompt buscar app", color = MutedColor, fontFamily = font) },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onGo() }),
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextColor,
                unfocusedTextColor = TextColor,
                focusedContainerColor = SurfaceColor,
                unfocusedContainerColor = SurfaceColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = accent
            )
        )
        TextButton(onClick = onSettings) {
            Text("⚙", color = accent, fontSize = 22.sp)
        }
    }
}

@Composable
private fun ClockView(settings: Settings, now: LocalDateTime, accent: Color, uiFont: FontFamily) {
    val pattern = buildString {
        append(if (settings.is24h) "HH:mm" else "h:mm")
        if (settings.showSeconds) append(":ss")
        if (!settings.is24h) append(" a")
    }
    val time = now.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
    val date = now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.forLanguageTag("es")))
    val clockFont = settings.clockFont.toFamily()
    val weight = when {
        settings.clockBold -> FontWeight.Bold
        settings.clockStyle == ClockStyle.MINIMAL -> FontWeight.Light
        else -> FontWeight.Normal
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when (settings.clockStyle) {
            ClockStyle.TERMINAL -> {
                Text("$ date", color = MutedColor, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                Row {
                    Text(
                        text = time,
                        color = accent,
                        fontSize = settings.clockSize.sp,
                        fontFamily = clockFont,
                        fontWeight = weight,
                        maxLines = 1
                    )
                    // Cursor parpadeante
                    Text(
                        text = if (now.second % 2 == 0) "▌" else " ",
                        color = accent,
                        fontSize = settings.clockSize.sp,
                        fontFamily = clockFont,
                        maxLines = 1
                    )
                }
                if (settings.showDate) {
                    Text("# $date", color = MutedColor, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                }
            }
            ClockStyle.BIG, ClockStyle.MINIMAL -> {
                Text(
                    text = time,
                    color = if (settings.clockStyle == ClockStyle.BIG) accent else TextColor,
                    fontSize = settings.clockSize.sp,
                    fontFamily = clockFont,
                    fontWeight = weight,
                    maxLines = 1
                )
                if (settings.showDate) {
                    Text(date, color = MutedColor, fontSize = 16.sp, fontFamily = uiFont)
                }
            }
        }
    }
}

@Composable
fun AppRow(app: AppInfo, font: FontFamily, onClick: () -> Unit) {
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
            fontFamily = font,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AppIcon(packageName: String) {
    val context = LocalContext.current
    val icon by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, key1 = packageName) {
        value = withContext(Dispatchers.IO) { IconCache.get(context, packageName) }
    }
    Box(modifier = Modifier.size(32.dp)) {
        icon?.let {
            Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}
