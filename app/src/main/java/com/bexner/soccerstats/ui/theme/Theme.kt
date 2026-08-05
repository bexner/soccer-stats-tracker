package com.bexner.soccerstats.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PitchGreen = Color(0xFF2E7D32)
private val PitchGreenDark = Color(0xFF1B5E20)
private val PitchGreenLight = Color(0xFF81C784)
private val Whistle = Color(0xFFFFB300)

private val LightColors = lightColorScheme(
    primary = PitchGreen,
    onPrimary = Color.White,
    primaryContainer = PitchGreenLight,
    onPrimaryContainer = Color(0xFF0B2E10),
    secondary = Whistle,
    onSecondary = Color(0xFF201600)
)

private val DarkColors = darkColorScheme(
    primary = PitchGreenLight,
    onPrimary = Color(0xFF0B2E10),
    primaryContainer = PitchGreenDark,
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Whistle,
    onSecondary = Color(0xFF201600)
)

@Composable
fun SoccerStatsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
