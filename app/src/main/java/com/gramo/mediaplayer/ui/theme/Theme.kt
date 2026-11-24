package com.gramo.mediaplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppBackground = Color(0xFFF6F7FB)
val PlaceholderGray = Color(0xFFE3E5EC)
val SelectedPillBg = Color(0xFFDCE2F0)
val RoyalBlueAccent = Color(0xFF1E56D9)
val SoftBlueTrack = Color(0xFFC8D7F8)
val PlayCircleBg = Color(0xFFDEE2EC)
val DarkIconColor = Color(0xFF1F2937)
val MutedTextColor = Color(0xFF6B7280)
val DividerGray = Color(0xFFE5E7EB)

private val LightColors = lightColorScheme(
    primary = RoyalBlueAccent,
    onPrimary = Color.White,
    secondaryContainer = SelectedPillBg,
    onSecondaryContainer = DarkIconColor,
    background = AppBackground,
    surface = AppBackground,
    onBackground = DarkIconColor,
    onSurface = DarkIconColor,
    surfaceVariant = PlaceholderGray,
    onSurfaceVariant = MutedTextColor
)

@Composable
fun GramoMediaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
