package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkGradientEnd
import com.example.ui.theme.DarkGradientMiddle
import com.example.ui.theme.DarkGradientStart
import com.example.ui.theme.GlassBorderDark
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.LightGradientEnd
import com.example.ui.theme.LightGradientStart
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun Modifier.appBackground(isDarkTheme: Boolean = true): Modifier {
    val brush = if (isDarkTheme) {
        Brush.linearGradient(
            colors = listOf(DarkGradientStart, DarkGradientMiddle, DarkGradientEnd)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(LightGradientStart, LightGradientEnd)
        )
    }
    return this.background(brush)
}


@Composable
fun Modifier.hazeGlassmorphism(
    state: HazeState,
    cornerRadius: Int = 16,
    isDarkTheme: Boolean = true
): Modifier {
    val surfaceColor = if (isDarkTheme) GlassSurfaceDark else GlassSurfaceLight
    val borderColor = if (isDarkTheme) GlassBorderDark else GlassBorderLight

    return shadow(
        elevation = 8.dp,
        shape = RoundedCornerShape(cornerRadius.dp),
        clip = false,
        ambientColor = Color.Black.copy(alpha = 0.1f),
        spotColor = Color.Black.copy(alpha = 0.1f)
    )
        .clip(RoundedCornerShape(cornerRadius.dp))
        .hazeEffect(
            state = state,
            style = HazeStyle(
                tint = HazeTint(surfaceColor),
                blurRadius = 24.dp,
                noiseFactor = 0.15f
            )
        )
        .border(
            width = 1.dp,
            color = borderColor,
            shape = RoundedCornerShape(cornerRadius.dp)
        )
}

@Composable
fun Modifier.staticGlassmorphism(
    cornerRadius: Int = 16,
    isDarkTheme: Boolean = true
): Modifier {
    val surfaceColor = if (isDarkTheme) GlassSurfaceDark else GlassSurfaceLight
    val borderColor = if (isDarkTheme) GlassBorderDark else GlassBorderLight

    return this
        .shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(cornerRadius.dp),
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.1f),
            spotColor = Color.Black.copy(alpha = 0.1f)
        )
        .clip(RoundedCornerShape(cornerRadius.dp))
        .background(surfaceColor)
        .border(
            width = 1.dp,
            color = borderColor,
            shape = RoundedCornerShape(cornerRadius.dp)
        )
}

@Composable
fun glassTextFieldColors(isDarkTheme: Boolean = true): TextFieldColors {
    val surfaceColor = if (isDarkTheme) GlassSurfaceDark else GlassSurfaceLight
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = surfaceColor,
        unfocusedContainerColor = surfaceColor,
        focusedBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.White,
        unfocusedBorderColor = if (isDarkTheme) GlassBorderDark else GlassBorderLight,
        focusedTextColor = if (isDarkTheme) Color.White else Color.Black,
        unfocusedTextColor = if (isDarkTheme) Color.White else Color.Black,
        cursorColor = if (isDarkTheme) Color.White else Color.Black,
        focusedPlaceholderColor = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f),
        unfocusedPlaceholderColor = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
    )
}
