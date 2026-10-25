package com.example.ui.theme

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

// Pure Monochrome Dark
private val DarkMonochromeColorScheme = darkColorScheme(
    primary = MonoWhite,
    onPrimary = MonoBlack,
    primaryContainer = MonoDarkElevated,
    onPrimaryContainer = MonoWhite,
    secondary = MonoLightGray,
    onSecondary = MonoBlack,
    secondaryContainer = MonoDarkElevated,
    onSecondaryContainer = MonoWhite,
    tertiary = MonoGray,
    onTertiary = MonoBlack,
    background = MonoBlack,
    onBackground = MonoWhite,
    surface = MonoDarkSurface,
    onSurface = MonoWhite,
    surfaceVariant = MonoDarkElevated,
    onSurfaceVariant = MonoLightGray,
    outline = MonoDarkBorder,
    outlineVariant = Color(0xFF282828)
)

// Pure Monochrome Light
private val LightMonochromeColorScheme = lightColorScheme(
    primary = MonoBlack,
    onPrimary = MonoWhite,
    primaryContainer = MonoLightElevated,
    onPrimaryContainer = MonoBlack,
    secondary = MonoDarkGray,
    onSecondary = MonoWhite,
    secondaryContainer = MonoLightElevated,
    onSecondaryContainer = MonoBlack,
    tertiary = MonoGray,
    onTertiary = MonoWhite,
    background = MonoWhite,
    onBackground = MonoBlack,
    surface = MonoLightSurface,
    onSurface = MonoBlack,
    surfaceVariant = MonoLightElevated,
    onSurfaceVariant = MonoDarkGray,
    outline = MonoLightBorder,
    outlineVariant = Color(0xFFE0E0E0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Themed to device wallpaper
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        // Device wallpaper themed dynamic colors (Android 12+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkMonochromeColorScheme
        else -> LightMonochromeColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
