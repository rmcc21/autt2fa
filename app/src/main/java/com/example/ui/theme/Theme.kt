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

private val DarkColorScheme = darkColorScheme(
    primary = M3PurpleLightContainer,
    onPrimary = M3OnPurpleContainer,
    primaryContainer = M3PurplePrimary,
    onPrimaryContainer = Color.White,
    secondary = HighDensitySuccess,
    tertiary = HighDensityWarning,
    background = HighDensityBgDark,
    surface = HighDensitySurfaceDark,
    surfaceVariant = HighDensitySurfaceTonalDark,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = HighDensityBorderDark,
    outlineVariant = Color(0xFF49454F)
)

private val LightColorScheme = lightColorScheme(
    primary = M3PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = M3PurpleContainer,
    onPrimaryContainer = M3OnPurpleContainer,
    secondary = HighDensitySuccess,
    tertiary = HighDensityWarning,
    background = HighDensityBgLight,
    surface = HighDensitySurfaceLight,
    surfaceVariant = HighDensitySurfaceTonal,
    onBackground = HighDensityTextPrimary,
    onSurface = HighDensityTextPrimary,
    onSurfaceVariant = HighDensityTextVariant,
    outline = HighDensityBorderMedium,
    outlineVariant = HighDensityBorderLight,
    error = HighDensityError,
    errorContainer = HighDensityErrorBg,
    onErrorContainer = HighDensityError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our custom security theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
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
        content = content
    )
}
