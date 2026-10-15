package io.github.mohuddle.overhead.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = SkyDeep,
    onPrimary = Limestone,
    primaryContainer = SkyMist,
    onPrimaryContainer = SkyInk,
    secondary = CopperDeep,
    onSecondary = Ink,
    secondaryContainer = CopperMist,
    onSecondaryContainer = CopperInk,
    background = Limestone,
    onBackground = Ink,
    surface = Limestone,
    onSurface = Ink,
    surfaceVariant = Travertine,
    onSurfaceVariant = ConcreteDeep,
    surfaceTint = Color.Transparent,
    error = Error,
)

private val DarkColors = darkColorScheme(
    primary = SkyBright,
    onPrimary = SkyInk,
    primaryContainer = SkyDeep,
    onPrimaryContainer = SkyMist,
    secondary = Copper,
    onSecondary = Ink,
    secondaryContainer = CopperInk,
    onSecondaryContainer = CopperMist,
    background = Ink,
    onBackground = Limestone,
    surface = Graphite,
    onSurface = Limestone,
    surfaceVariant = Slate,
    onSurfaceVariant = Concrete,
    surfaceTint = Color.Transparent,
    error = Error,
)

@Composable
fun OverheadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
