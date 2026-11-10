package com.dicylow.launcher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
    onSave: (Settings) -> Boolean,
    resetKey: Int,
    appsKey: Int
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    var query by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }

    // Borrador: mientras editas ajustes trabajas sobre esta copia.
    // Solo pasa a ser real al pulsar GUARDAR. Descartar lo borra.
    var draft by remember { mutableStateOf<Settings?>(null) }
    val showSettings = draft != null
    val s = draft ?: settings

    val accent = Color(s.theme.accent)
    val surface = Color(s.theme.surface)
    val clockColor = if (s.clockColor == 0L) accent else Color(s.clockColor)
    val uiFont = s.uiFont.toFamily()

    LaunchedEffect(resetKey) {
        query = ""
        draft = null
        focusManager.clearFocus()
    }

    val apps by produceState<List<AppInfo>>(initialValue = emptyList(), key1 = appsKey) {
        value = withContext(Dispatchers.IO) { loadApps(context) }
    }

    val now by produceState(
        initialValue = LocalDateTime.now(),
        key1 = s.showSeconds,
        key2 = s.clockStyle
    ) {
        val interval = if (s.showSeconds || s.clockStyle == ClockStyle.TERMINAL) 1_000L else 15_000L
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

    // Atrás: cierra ajustes (sin guardar), luego el teclado, luego borra la búsqueda
    BackHandler(enabled = showSettings || focused || query.isNotEmpty()) {
        when {
            showSettings -> { draft = null }
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
            surface = surface,
            onSurface = TextColor,
            onBackground = TextColor
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Sin fondo propio: se ve el fondo del sistema (windowShowWallpaper)
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = s.dim)))

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                if (s.searchPos == SearchPos.TOP) {
                    SearchField(
                        query = query,
                        onQueryChange = { query = it },
                        prompt = s.prompt,
                        accent = accent,
                        surface = surface,
                        font = uiFont,
                        onFocus = { focused = it },
                        onGo = { results.firstOrNull()?.let { launch(it) } },
                        onSettings = { draft = settings }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (query.isBlank()) {
                    val interaction = remember { MutableInteractionSource() }
                    val align = when (s.clockPos) {
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
                        ClockView(s = s, now = now, clockColor = clockColor, uiFont = uiFont)
                    }
                } else {
                    // La lista ocupa el espacio libre y nunca tapa el buscador
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (results.isEmpty()) {
                            item {
                                Text(
                                    "sin resultados",
                                    color = MutedColor,
                                    fontFamily = uiFont,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        items(results, key = { it.packageName }) { app ->
                            AppRow(app = app, query = query, accent = accent, font = uiFont, onClick = { launch(app) })
                        }
                    }
                }

                if (s.searchPos == SearchPos.BOTTOM) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SearchField(
                        query = query,
                        onQueryChange = { query = it },
                        prompt = s.prompt,
                        accent = accent,
                        surface = surface,
                        font = uiFont,
                        onFocus = { focused = it },
                        onGo = { results.firstOrNull()?.let { launch(it) } },
                        onSettings = { draft = settings }
                    )
                }
            }

            if (showSettings) {
                SettingsScreen(
                    settings = s,
                    onChange = { draft = it },
                    hasChanges = draft != null && draft != settings,
                    onSave = {
                        val toSave = draft
                        if (toSave != null) {
                            if (onSave(toSave)) {
                                draft = null
                                Toast.makeText(context, "Ajustes guardados", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No se pudo guardar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onDiscard = { draft = null }
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    prompt: String,
    accent: Color,
    surface: Color,
    font: FontFamily,
    onFocus: (Boolean) -> Unit,
    onGo: () -> Unit,
    onSettings: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val cursorWidth = with(LocalDensity.current) { 11.dp.toPx() }

    // Parpadeo tipo terminal: solo corre mientras el buscador tiene el foco
    val blink by produceState(initialValue = true, key1 = isFocused) {
        value = true
        while (isFocused) {
            delay(530)
            value = !value
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surface)
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(prompt, color = accent, fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "buscar app",
                    color = MutedColor,
                    fontFamily = font,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        isFocused = it.isFocused
                        onFocus(it.isFocused)
                    }
                    .drawWithContent {
                        drawContent()
                        // Cursor de bloque, como las terminales antiguas
                        val rect = layout?.getCursorRect(query.length)
                        if (isFocused && blink && rect != null) {
                            drawRect(
                                color = accent,
                                topLeft = Offset(rect.left, rect.top),
                                size = Size(cursorWidth, rect.height)
                            )
                        }
                    },
                textStyle = TextStyle(
                    color = TextColor,
                    fontFamily = font,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                singleLine = true,
                cursorBrush = SolidColor(Color.Transparent),
                onTextLayout = { layout = it },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onGo() })
            )
        }

        TextButton(onClick = onSettings) {
            Text("⚙", color = accent, fontSize = 22.sp)
        }
    }
}

@Composable
private fun ClockView(s: Settings, now: LocalDateTime, clockColor: Color, uiFont: FontFamily) {
    val pattern = buildString {
        append(if (s.is24h) "HH:mm" else "h:mm")
        if (s.showSeconds) append(":ss")
        if (!s.is24h) append(" a")
    }
    val time = now.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
    val date = now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.forLanguageTag("es")))
    val clockFont = s.clockFont.toFamily()
    val weight = when {
        s.clockBold -> FontWeight.Bold
        s.clockStyle == ClockStyle.MINIMAL -> FontWeight.Light
        else -> FontWeight.Normal
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (s.clockStyle == ClockStyle.TERMINAL) {
            Text("$ date", color = MutedColor, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Row {
                Text(
                    text = time,
                    color = clockColor,
                    fontSize = s.clockSize.sp,
                    fontFamily = clockFont,
                    fontWeight = weight,
                    maxLines = 1
                )
                Text(
                    text = if (now.second % 2 == 0) "█" else " ",
                    color = clockColor,
                    fontSize = s.clockSize.sp,
                    fontFamily = clockFont,
                    maxLines = 1
                )
            }
            if (s.showDate) {
                Text("# $date", color = MutedColor, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            }
        } else {
            Text(
                text = time,
                color = clockColor,
                fontSize = s.clockSize.sp,
                fontFamily = clockFont,
                fontWeight = weight,
                maxLines = 1
            )
            if (s.showDate) {
                Text(date, color = MutedColor, fontSize = 16.sp, fontFamily = uiFont)
            }
        }
    }
}

/** Resalta en color la parte del nombre que coincide con lo escrito. */
private fun highlight(label: String, query: String, accent: Color): AnnotatedString {
    val q = query.trim()
    val idx = if (q.isEmpty()) -1 else label.indexOf(q, ignoreCase = true)
    return buildAnnotatedString {
        if (idx < 0) {
            append(label)
        } else {
            append(label.substring(0, idx))
            withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) {
                append(label.substring(idx, idx + q.length))
            }
            append(label.substring(idx + q.length))
        }
    }
}

@Composable
fun AppRow(app: AppInfo, query: String, accent: Color, font: FontFamily, onClick: () -> Unit) {
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
            text = highlight(app.label, query, accent),
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
