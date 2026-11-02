package com.seventyfiveperc.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = PureBlack,
    secondary = HotMagenta,
    onSecondary = Color.White,
    tertiary = LimeGreen,
    onTertiary = PureBlack,
    background = PureBlack,
    surface = DarkGrey,
    onBackground = OffWhite,
    onSurface = OffWhite,
    error = BloodRed,
    surfaceVariant = GlassWhite,
    onSurfaceVariant = OffWhite
)

private val LightColorScheme = lightColorScheme(
    primary = NeonCyan,
    onPrimary = PureBlack,
    secondary = HotMagenta,
    onSecondary = Color.White,
    tertiary = LimeGreen,
    onTertiary = PureBlack,
    background = OffWhite,
    surface = Color.White,
    onBackground = PureBlack,
    onSurface = PureBlack,
    error = BloodRed,
    surfaceVariant = Color.LightGray.copy(alpha = 0.2f),
    onSurfaceVariant = PureBlack
)

@Composable
fun AttendAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
