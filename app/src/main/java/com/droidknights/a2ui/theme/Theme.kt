package com.droidknights.a2ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = Purple80,
        secondary = PurpleGrey80,
        tertiary = Pink80,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF1B64DA),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE8F3FF),
        onPrimaryContainer = Color(0xFF003366),
        secondary = Color(0xFF334155),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF2F4F6),
        onSecondaryContainer = Color(0xFF191F28),
        tertiary = Color(0xFF0F766E),
        tertiaryContainer = Color(0xFFE6FFFA),
        onTertiaryContainer = Color(0xFF134E4A),
        background = Color(0xFFF4F6FA),
        onBackground = Color(0xFF191F28),
        surface = Color.White,
        onSurface = Color(0xFF191F28),
        surfaceVariant = Color(0xFFF2F4F6),
        onSurfaceVariant = Color(0xFF4E5968),
    )

@Composable
fun DroidKnightsA2uiTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
