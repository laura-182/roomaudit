package com.roomcheck.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.roomcheck.app.domain.Rating

val StatusRed = Color(0xFFE53935)
val StatusYellow = Color(0xFFF9A825)
val StatusGreen = Color(0xFF43A047)

val Rating.color: Color
    get() = when (this) {
        Rating.RED -> StatusRed
        Rating.YELLOW -> StatusYellow
        Rating.GREEN -> StatusGreen
    }

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    secondary = Color(0xFF546E7A),
    tertiary = Color(0xFF00897B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    secondary = Color(0xFFB0BEC5),
    tertiary = Color(0xFF4DB6AC),
)

@Composable
fun RoomCheckTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
