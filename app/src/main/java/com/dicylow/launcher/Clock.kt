package com.dicylow.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

/**
 * Reloj. El tamaño se calcula con un ancho de referencia FIJO (5 u 8 caracteres),
 * no con la hora actual. Así el número nunca cambia de tamaño solo.
 */
@Composable
fun ClockView(s: Settings, now: LocalDateTime, clockColor: Color, uiFont: FontFamily) {
    val clockFont = s.clockFont.family()
    val surface = Color(s.theme.surface)
    val accent = Color(s.theme.accent)
    val terminal = s.clockStyle == ClockStyle.TERMINAL
    val capsule = s.clockStyle == ClockStyle.CAPSULA
    val stacked = s.clockStacked && !terminal

    val weight = when {
        s.clockBold || s.clockStyle == ClockStyle.SOMBRA -> FontWeight.Bold
        s.clockStyle == ClockStyle.MINIMAL -> FontWeight.Light
        else -> FontWeight.Normal
    }
    val shadow: Shadow? = when (s.clockStyle) {
        ClockStyle.NEON -> Shadow(color = clockColor, offset = Offset.Zero, blurRadius = 36f)
        ClockStyle.SOMBRA -> Shadow(color = Color.Black.copy(alpha = 0.75f), offset = Offset(6f, 8f), blurRadius = 4f)
        else -> null
    }
    val textStyle = TextStyle(shadow = shadow)

    val ampm = if (!s.is24h && s.showAmPm) (if (now.hour < 12) "AM" else "PM") else ""
    val date = now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.forLanguageTag("es")))
    val hm = now.format(DateTimeFormatter.ofPattern(if (s.is24h) "HH:mm" else "h:mm", Locale.US))
    val sec = if (s.showSeconds) now.format(DateTimeFormatter.ofPattern(":ss", Locale.US)) else ""
    val hours = now.format(DateTimeFormatter.ofPattern(if (s.is24h) "HH" else "h", Locale.US))
    val minutes = now.format(DateTimeFormatter.ofPattern("mm", Locale.US))

    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Espacio que ocupan el marco de la cápsula y el AM/PM al lado
        val reserve = (if (capsule) 56f else 0f) + (if (ampm.isEmpty()) 0f else 36f)
        // Caracteres de referencia fijos: 5 para HH:mm, 8 con segundos, 2 en dos líneas
        val chars = (if (stacked) 2 else if (s.showSeconds) 8 else 5) + (if (terminal) 1 else 0)
        val fit = (maxWidth.value - reserve) / (chars * 0.62f)
        val size = min(s.clockSize.toFloat(), fit).coerceAtLeast(24f).sp

        val content: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    terminal -> {
                        Text("$ date", color = MutedColor, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Row {
                            Text(
                                text = hm + sec,
                                color = clockColor,
                                fontSize = size,
                                fontFamily = clockFont,
                                fontWeight = weight,
                                maxLines = 1
                            )
                            // Cursor de bloque parpadeante
                            Text(
                                text = if (now.second % 2 == 0) "█" else " ",
                                color = clockColor,
                                fontSize = size,
                                fontFamily = clockFont,
                                maxLines = 1
                            )
                        }
                        if (s.showDate) {
                            Text("# $date", color = MutedColor, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    stacked -> {
                        Text(
                            text = hours,
                            color = clockColor,
                            fontSize = size,
                            fontFamily = clockFont,
                            fontWeight = weight,
                            maxLines = 1,
                            style = textStyle
                        )
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = minutes,
                                color = clockColor,
                                fontSize = size,
                                fontFamily = clockFont,
                                fontWeight = weight,
                                maxLines = 1,
                                style = textStyle
                            )
                            if (ampm.isNotEmpty()) {
                                Text(
                                    text = ampm,
                                    color = clockColor.copy(alpha = 0.85f),
                                    fontSize = 14.sp,
                                    fontFamily = uiFont,
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                                )
                            }
                        }
                        if (s.showDate) {
                            Text(date, color = MutedColor, fontSize = 16.sp, fontFamily = uiFont)
                        }
                    }
                    else -> {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = hm + sec,
                                color = clockColor,
                                fontSize = size,
                                fontFamily = clockFont,
                                fontWeight = weight,
                                maxLines = 1,
                                style = textStyle
                            )
                            if (ampm.isNotEmpty()) {
                                Text(
                                    text = ampm,
                                    color = clockColor.copy(alpha = 0.85f),
                                    fontSize = 14.sp,
                                    fontFamily = uiFont,
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                                )
                            }
                        }
                        if (s.showDate) {
                            Text(date, color = MutedColor, fontSize = 16.sp, fontFamily = uiFont)
                        }
                    }
                }
            }
        }

        if (capsule) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(surface)
                    .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                    .padding(horizontal = 22.dp, vertical = 14.dp)
            ) {
                content()
            }
        } else {
            content()
        }
    }
}
