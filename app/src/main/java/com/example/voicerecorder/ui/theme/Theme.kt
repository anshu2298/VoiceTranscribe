package com.example.voicerecorder.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF80CBC4),
    onPrimary = Color(0xFF00363A),
    primaryContainer = Color(0xFF004F54),
    onPrimaryContainer = Color(0xFF9CF0E1),
    secondary = Color(0xFFB2CCD1),
    background = Color(0xFF191C1D),
    surface = Color(0xFF191C1D),
    onBackground = Color(0xFFE1E3E3),
    onSurface = Color(0xFFE1E3E3),
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006970),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF0E1),
    onPrimaryContainer = Color(0xFF00201F),
    secondary = Color(0xFF4A6267),
    background = Color(0xFFFBFDFD),
    surface = Color(0xFFFBFDFD),
    onBackground = Color(0xFF191C1D),
    onSurface = Color(0xFF191C1D),
    error = Color(0xFFBA1A1A)
)

@Composable
fun VoiceTranscribeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
