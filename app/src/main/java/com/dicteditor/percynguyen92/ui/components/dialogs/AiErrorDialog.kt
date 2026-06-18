@file:Suppress("PackageName")

package com.dicteditor.percynguyen92.ui.components.dialogs

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.dicteditor.percynguyen92.ui.components.HazeAlertDialog
import dev.chrisbanes.haze.HazeState

@Composable
fun AiErrorDialog(
    hazeState: HazeState,
    isAtpConnected: Boolean,
    connectionError: String?,
    onDismiss: () -> Unit
) {
    LaunchedEffect(isAtpConnected) {
        if (isAtpConnected) {
            onDismiss()
        }
    }
    
    if (isAtpConnected) {
        return
    }
    
    if (connectionError != null) {
        HazeAlertDialog(
            hazeState = hazeState,
            onDismissRequest = onDismiss,
            title = { Text("Lỗi kết nối AI", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Nguyên nhân: $connectionError", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("Đóng", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    } else {
        HazeAlertDialog(
            hazeState = hazeState,
            onDismissRequest = onDismiss,
            title = { Text("Đang kết nối AI...", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Vui lòng đợi một chút hoặc kiểm tra xem ứng dụng AI Portal có đang chạy không.", style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("Đóng", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}
