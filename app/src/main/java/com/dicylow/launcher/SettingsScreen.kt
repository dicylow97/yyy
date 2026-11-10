package com.dicylow.launcher

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(settings: Settings, onChange: (Settings) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val accent = Color(settings.theme.accent)
    val font = settings.uiFont.toFamily()
    var message by remember { mutableStateOf("") }

    val pickWallpaper = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            message = if (applyWallpaper(context, uri)) "Fondo aplicado" else "No se pudo aplicar ese fondo"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AJUSTES", color = accent, fontFamily = font, fontSize = 20.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) {
                Text("Cerrar", color = accent, fontFamily = font)
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            SectionTitle("RELOJ", accent, font)
            Label("Estilo", font)
            ChoiceRow(
                options = listOf(ClockStyle.BIG to "Grande", ClockStyle.TERMINAL to "Terminal", ClockStyle.MINIMAL to "Minimal"),
                selected = settings.clockStyle,
                accent = accent,
                font = font,
                onSelect = { onChange(settings.copy(clockStyle = it)) }
            )
            Label("Posición", font)
            ChoiceRow(
                options = listOf(ClockPos.TOP to "Arriba", ClockPos.CENTER to "Centro", ClockPos.BOTTOM to "Abajo"),
                selected = settings.clockPos,
                accent = accent,
                font = font,
                onSelect = { onChange(settings.copy(clockPos = it)) }
            )
            Label("Tamaño: ${settings.clockSize}", font)
            Slider(
                value = settings.clockSize.toFloat(),
                onValueChange = { onChange(settings.copy(clockSize = it.roundToInt())) },
                valueRange = 40f..120f,
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
            )
            Label("Tipografía del reloj", font)
            ChoiceRow(
                options = fontOptions(),
                selected = settings.clockFont,
                accent = accent,
                font = font,
                onSelect = { onChange(settings.copy(clockFont = it)) }
            )
            SwitchRow("Negrita", settings.clockBold, accent, font) { onChange(settings.copy(clockBold = it)) }
            SwitchRow("Mostrar segundos", settings.showSeconds, accent, font) { onChange(settings.copy(showSeconds = it)) }
            SwitchRow("Mostrar fecha", settings.showDate, accent, font) { onChange(settings.copy(showDate = it)) }
            SwitchRow("Formato 24 horas", settings.is24h, accent, font) { onChange(settings.copy(is24h = it)) }

            SectionTitle("BUSCADOR", accent, font)
            Label("Posición", font)
            ChoiceRow(
                options = listOf(SearchPos.TOP to "Arriba", SearchPos.BOTTOM to "Abajo"),
                selected = settings.searchPos,
                accent = accent,
                font = font,
                onSelect = { onChange(settings.copy(searchPos = it)) }
            )
            Label("Símbolo del prompt", font)
            ChoiceRow(
                options = listOf(">" to ">", "\$" to "\$", "❯" to "❯", "λ" to "λ"),
                selected = settings.prompt,
                accent = accent,
                font = font,
                onSelect = { onChange(settings.copy(prompt = it)) }
            )

            SectionTitle("INTERFAZ", accent, font)
            Label("Tipografía de la interfaz", font)
            ChoiceRow(
                options = fontOptions(),
                selected = settings.uiFont,
                accent = accent,
                font = font,
                onSelect = { onChange(settings.copy(uiFont = it)) }
            )
            Label("Tema de color", font)
            ChoiceRow(
                options = ThemeChoice.values().toList().map { it to it.label },
                selected = settings.theme,
                accent = accent,
                font = font,
                onSelect = { onChange(settings.copy(theme = it)) }
            )

            SectionTitle("FONDO", accent, font)
            Label("Oscurecer: ${(settings.dim * 100).roundToInt()}%", font)
            Slider(
                value = settings.dim,
                onValueChange = { onChange(settings.copy(dim = it)) },
                valueRange = 0f..0.9f,
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
            )
            Row(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedButton(onClick = { pickWallpaper.launch(arrayOf("image/*")) }) {
                    Text("Elegir fondo", color = accent, fontFamily = font)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = {
                    message = if (clearWallpaper(context)) "Fondo restaurado" else "No se pudo restaurar"
                }) {
                    Text("Quitar fondo", color = TextColor, fontFamily = font)
                }
            }
            if (message.isNotEmpty()) {
                Text(message, color = MutedColor, fontFamily = font, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }

            Text(
                "Launcher 1.1 · escribe para buscar, pulsa Atrás para cerrar el teclado",
                color = MutedColor,
                fontFamily = font,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 32.dp, bottom = 24.dp)
            )
        }
    }
}

private fun fontOptions(): List<Pair<FontChoice, String>> = listOf(
    FontChoice.MONO to "Mono",
    FontChoice.SERIF to "Serif",
    FontChoice.SANS to "Sans",
    FontChoice.CURSIVE to "Cursiva"
)

@Composable
private fun SectionTitle(text: String, accent: Color, font: FontFamily) {
    Text(
        text = text,
        color = accent,
        fontFamily = font,
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun Label(text: String, font: FontFamily) {
    Text(
        text = text,
        color = MutedColor,
        fontFamily = font,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, accent: Color, font: FontFamily, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextColor, fontFamily = font, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = BgColor)
        )
    }
}

@Composable
private fun <T> ChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    accent: Color,
    font: FontFamily,
    onSelect: (T) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        options.forEach { (value, label) ->
            val isOn = value == selected
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isOn) accent else CardColor)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(label, color = if (isOn) BgColor else TextColor, fontFamily = font, fontSize = 13.sp)
            }
        }
    }
}
