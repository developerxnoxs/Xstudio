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

private val StudioDarkColorScheme = darkColorScheme(
    primary = StudioGreen,
    onPrimary = Color(0xFF003919),
    primaryContainer = Color(0xFF005327),
    onPrimaryContainer = Color(0xFF6CFFA0),
    secondary = StudioBlue,
    onSecondary = Color(0xFF002F6C),
    secondaryContainer = Color(0xFF004699),
    onSecondaryContainer = Color(0xFFB0D2FF),
    tertiary = StudioCyan,
    background = StudioBackground,
    onBackground = Color(0xFFE3E2E6),
    surface = StudioSurface,
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = Color(0xFFC4C7C5),
    outline = StudioBorder,
    error = StudioRed
)

private val StudioLightColorScheme = lightColorScheme(
    primary = Color(0xFF006D38),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF6CFFA0),
    onPrimaryContainer = Color(0xFF00210C),
    secondary = StudioLightPrimary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E4FF),
    onSecondaryContainer = Color(0xFF001B3E),
    tertiary = Color(0xFF00677D),
    background = StudioLightBg,
    onBackground = Color(0xFF191C1D),
    surface = StudioLightSurface,
    onSurface = Color(0xFF191C1D),
    surfaceVariant = StudioLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF44474E),
    outline = StudioLightBorder,
    error = Color(0xFFBA1A1A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to IDE Dark Mode for best Android Studio feel
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> StudioDarkColorScheme
        else -> StudioLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun AndroidStudioTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MyApplicationTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}

