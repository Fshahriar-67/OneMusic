package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = OnePlusRed,
    secondary = SpotifyGreen,
    tertiary = TextMutedGray,
    background = OnyxDarkBg,
    surface = ObsidianSurface,
    surfaceVariant = DarkCardSurface,
    onPrimary = TextPrimaryWhite,
    onSecondary = OnyxDarkBg,
    onBackground = TextPrimaryWhite,
    onSurface = TextPrimaryWhite,
    onSurfaceVariant = TextMutedGray
)

private val LightColorScheme = lightColorScheme(
    primary = OnePlusRed,
    secondary = SpotifyGreen,
    tertiary = TextMutedCoolCharcoal,
    background = ChalkLightBg,
    surface = PureWhiteSurface,
    surfaceVariant = PureWhiteSurface,
    onPrimary = TextPrimaryWhite,
    onSecondary = TextPrimaryCharcoal,
    onBackground = TextPrimaryCharcoal,
    onSurface = TextPrimaryCharcoal,
    onSurfaceVariant = TextMutedCoolCharcoal
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve original identity
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
