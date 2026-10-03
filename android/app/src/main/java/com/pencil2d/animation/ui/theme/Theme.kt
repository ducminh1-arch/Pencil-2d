package com.pencil2d.animation.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PencilYellowPrimary,
    onPrimary = PencilTextPrimary,
    primaryContainer = PencilYellowLight,
    onPrimaryContainer = PencilTextPrimary,
    secondary = PencilAccent,
    onSecondary = PencilSurface,
    background = PencilBackground,
    onBackground = PencilTextPrimary,
    surface = PencilSurface,
    onSurface = PencilTextPrimary,
    surfaceVariant = PencilBackground,
    onSurfaceVariant = PencilTextSecondary,
    outline = PencilBorder
)

@Composable
fun Pencil2DTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
