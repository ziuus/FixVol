package com.fixvol.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryEmerald,
    onPrimary = LightSurface,
    secondary = SecondaryDark,
    background = LightBgBase,
    surface = LightSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun FixVolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
