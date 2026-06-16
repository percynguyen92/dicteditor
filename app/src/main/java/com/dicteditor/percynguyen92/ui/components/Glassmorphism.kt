package com.dicteditor.percynguyen92.ui.components

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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dicteditor.percynguyen92.ui.theme.DarkColors
import com.dicteditor.percynguyen92.ui.theme.LightColors
import com.dicteditor.percynguyen92.ui.theme.DarkBackgroundBrush
import com.dicteditor.percynguyen92.ui.theme.LightBackgroundBrush
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun Modifier.appBackground(isDarkTheme: Boolean = true): Modifier {
    val brush = if (isDarkTheme) DarkBackgroundBrush else LightBackgroundBrush
    return this.background(brush)
}


@Composable
fun Modifier.hazeGlassmorphism(
    state: HazeState,
    cornerRadius: Int = 12,
    isDarkTheme: Boolean = true,
    blurRadius: Dp = 12.dp,
    tint: Color = Color.Unspecified,
    shape: Shape? = null
): Modifier {
    val surfaceColor = if (tint != Color.Unspecified) tint else (if (isDarkTheme) DarkColors.GlassSurface else LightColors.GlassSurface)
    val borderColor = if (isDarkTheme) DarkColors.GlassBorder else LightColors.GlassBorder
    val actualShape = shape ?: RoundedCornerShape(cornerRadius.dp)

    return shadow(
        elevation = 8.dp,
        shape = actualShape,
        clip = false,
        ambientColor = Color.Black.copy(alpha = 0.1f),
        spotColor = Color.Black.copy(alpha = 0.1f)
    )
        .clip(actualShape)
        .hazeEffect(
            state = state,
            style = HazeStyle(
                tint = HazeTint(surfaceColor),
                blurRadius = blurRadius,
                noiseFactor = 0.15f
            )
        )
        .border(
            width = 1.dp,
            color = borderColor,
            shape = actualShape
        )
}

@Composable
fun Modifier.staticGlassmorphism(
    cornerRadius: Int = 12,
    isDarkTheme: Boolean = true
): Modifier {
    val surfaceColor = if (isDarkTheme) DarkColors.GlassSurface else LightColors.GlassSurface
    val borderColor = if (isDarkTheme) DarkColors.GlassBorder else LightColors.GlassBorder

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
    val surfaceColor = if (isDarkTheme) DarkColors.GlassSurface else LightColors.GlassSurface
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = surfaceColor,
        unfocusedContainerColor = surfaceColor,
        focusedBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.White,
        unfocusedBorderColor = if (isDarkTheme) DarkColors.GlassBorder else LightColors.GlassBorder,
        focusedTextColor = if (isDarkTheme) Color.White else Color.Black,
        unfocusedTextColor = if (isDarkTheme) Color.White else Color.Black,
        cursorColor = if (isDarkTheme) Color.White else Color.Black,
        focusedPlaceholderColor = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f),
        unfocusedPlaceholderColor = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
    )
}
