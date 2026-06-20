package com.dicteditor.percynguyen92.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.dicteditor.percynguyen92.R
import dev.chrisbanes.haze.HazeState

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun BulkSelectionBar(
    hazeState: HazeState,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onBulkDeleteClick: () -> Unit,
    onBulkExportClick: () -> Unit,
    onFabClick: () -> Unit
) {
    val isBulkMode = selectedCount > 0
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    // Animate color
    val targetBgColor = if (isBulkMode) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
    } else {
        Color.Transparent
    }
    val bgColorSpec = if (isBulkMode) {
        tween<Color>(durationMillis = 100)
    } else {
        tween<Color>(durationMillis = 100, delayMillis = 100)
    }
    val bgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = bgColorSpec,
        label = "bgColorAnimation"
    )

    // Animate width from 56.dp (FAB) to screenWidth - 32.dp (Bar)
    val targetWidth = if (isBulkMode) screenWidth - 32.dp else 56.dp
    val widthSpec = if (isBulkMode) {
        tween<Dp>(durationMillis = 100, delayMillis = 100)
    } else {
        tween<Dp>(durationMillis = 100)
    }
    val width by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = widthSpec,
        label = "widthAnimation"
    )

    // Make the card glassmorphic
    Box(
        modifier = Modifier
            .width(width)
            .height(56.dp)
            .hazeGlassmorphism(hazeState, cornerRadius = 12, tint = bgColor)
            .then(
                if (isBulkMode) Modifier else Modifier.clickable { onFabClick() }
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isBulkMode,
            transitionSpec = {
                if (targetState) {
                    fadeIn(animationSpec = tween(80, delayMillis = 150)) togetherWith 
                    fadeOut(animationSpec = tween(80))
                } else {
                    fadeIn(animationSpec = tween(80, delayMillis = 150)) togetherWith 
                    fadeOut(animationSpec = tween(80))
                }
            },
            label = "BulkSelectionBarAnimation"
        ) { targetIsBulkMode ->
            if (targetIsBulkMode) {
                // Bulk selection actions layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.label_selected_count, selectedCount),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onSelectAll,
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(stringResource(R.string.button_select_all), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                        }
                        TextButton(
                            onClick = onClearSelection,
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(stringResource(R.string.button_clear_selection), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                        }
                        IconButton(
                            onClick = onBulkExportClick,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.button_bulk_export), modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = onBulkDeleteClick,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.button_bulk_delete), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            } else {
                // FAB layout (plus icon)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.description_add_word),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
