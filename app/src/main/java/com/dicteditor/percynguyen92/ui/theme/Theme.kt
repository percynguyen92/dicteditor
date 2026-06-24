package com.dicteditor.percynguyen92.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext

import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DarkColors.Primary,
    secondary = DarkColors.PrimaryVariant,
    background = DarkColors.GradientStart,
    surface = DarkColors.GlassSurface,
    onPrimary = DarkColors.TextPrimary,
    onSecondary = DarkColors.TextSecondary,
    onBackground = DarkColors.TextPrimary,
    onSurface = DarkColors.TextPrimary,
    error = DarkColors.Error,
    primaryContainer = DarkColors.PrimaryContainer,
    onPrimaryContainer = DarkColors.OnPrimaryContainer,
    errorContainer = DarkColors.ErrorContainer,
    onErrorContainer = DarkColors.OnErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = LightColors.Primary,
    secondary = LightColors.PrimaryVariant,
    background = LightColors.GradientStart,
    surface = LightColors.GlassSurface,
    onPrimary = LightColors.TextPrimary,
    onSecondary = LightColors.TextSecondary,
    onBackground = LightColors.TextPrimary,
    onSurface = LightColors.TextPrimary,
    error = LightColors.Error,
    primaryContainer = LightColors.PrimaryContainer,
    onPrimaryContainer = LightColors.OnPrimaryContainer,
    errorContainer = LightColors.ErrorContainer,
    onErrorContainer = LightColors.OnErrorContainer
)

val DarkBackgroundBrush = Brush.linearGradient(
    colors = listOf(DarkColors.GradientStart, DarkColors.GradientMiddle, DarkColors.GradientEnd)
)

val LightBackgroundBrush = Brush.linearGradient(
    colors = listOf(LightColors.GradientStart, LightColors.GradientEnd)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Force dark theme as the app is dark-only
    val darkTheme = true
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
    } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

