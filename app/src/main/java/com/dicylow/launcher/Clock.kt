package com.dicylow.launcher

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

/**
 * Reloj. El tamaño nunca se sale de la pantalla: si el número es muy grande
 * se ajusta al ancho disponible. El AM/PM va pequeño al lado, no encima de la hora.
 */
@Composable
fun ClockView(s: Settings, now: LocalDateTime, clockColor: Color, uiFont: FontFamily) {
    val clockFont = s.clockFont.family()
    val weight = when {
        s.clockBold -> FontWeight.Bold
        s.clockStyle == ClockStyle.MINIMAL -> FontWeight.Light
        else -> FontWeight.Normal
    }
    val ampm = if (!s.is24h && s.showAmPm) (if (now.hour < 12) "AM" else "PM") else ""
    val ampmColor = clockColor.copy(alpha = 0.85f)
    val date = now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.forLanguageTag("es")))
    val stacked = s.clockStacked && s.clockStyle != ClockStyle.TERMINAL
    val hm = now.format(DateTimeFormatter.ofPattern(if (s.is24h) "HH:mm" else "h:mm", Locale.US))
    val sec = if (s.showSeconds) now.format(DateTimeFormatter.ofPattern(":ss", Locale.US)) else ""
    val hours = now.format(DateTimeFormatter.ofPattern(if (s.is24h) "HH" else "h", Locale.US))
    val minutes = now.format(DateTimeFormatter.ofPattern("mm", Locale.US))

    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Espacio que ocupa el AM/PM al lado de los números
        val reserve = if (ampm.isEmpty()) 0f else 36f
        // Cuántos caracteres van en la línea más ancha (los dígitos miden unos 0.62 em)
        val chars = if (stacked) 2 else hm.length + sec.length
        val fit = (maxWidth.value - reserve) / (chars * 0.62f)
        val size = min(s.clockSize.toFloat(), fit).coerceAtLeast(24f).sp

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                s.clockStyle == ClockStyle.TERMINAL -> {
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
                    // Hora arriba y minutos abajo, en grande
                    Text(
                        text = hours,
                        color = clockColor,
                        fontSize = size,
                        fontFamily = clockFont,
                        fontWeight = weight,
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = minutes,
                            color = clockColor,
                            fontSize = size,
                            fontFamily = clockFont,
                            fontWeight = weight,
                            maxLines = 1
                        )
                        if (ampm.isNotEmpty()) {
                            Text(
                                text = ampm,
                                color = ampmColor,
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
                            maxLines = 1
                        )
                        if (ampm.isNotEmpty()) {
                            Text(
                                text = ampm,
                                color = ampmColor,
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
}
