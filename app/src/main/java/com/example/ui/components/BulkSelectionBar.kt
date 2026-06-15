package com.example.ui.components

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
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import com.example.ui.components.hazeGlassmorphism

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
        tween(durationMillis = 100)
    } else {
        tween(durationMillis = 100, delayMillis = 100)
    }
    val bgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = bgColorSpec,
        label = "bgColorAnimation"
    )

    // Animate width from 56.dp (FAB) to screenWidth - 32.dp (Bar)
    val targetWidth = if (isBulkMode) screenWidth - 32.dp else 56.dp
    val widthSpec = if (isBulkMode) {
        tween(durationMillis = 100, delayMillis = 100)
    } else {
        tween(durationMillis = 100)
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
            .hazeGlassmorphism(hazeState, cornerRadius = 16)
            .background(bgColor, shape = RoundedCornerShape(16.dp))
            .then(
                if (isBulkMode) Modifier else Modifier.clickable { onFabClick() }
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isBulkMode,
            transitionSpec = {
                if (targetState) {
                    fadeIn(animationSpec = tween(80, delayMillis = 120)) togetherWith 
                    fadeOut(animationSpec = tween(80))
                } else {
                    fadeIn(animationSpec = tween(80, delayMillis = 120)) togetherWith 
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
                        text = "Đã chọn $selectedCount",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onSelectAll,
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Tất cả", color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(
                            onClick = onClearSelection,
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Bỏ chọn", color = MaterialTheme.colorScheme.error)
                        }
                        IconButton(
                            onClick = onBulkExportClick,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Xuất", modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = onBulkDeleteClick,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa", modifier = Modifier.size(20.dp))
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
                        contentDescription = "Thêm từ mới",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
