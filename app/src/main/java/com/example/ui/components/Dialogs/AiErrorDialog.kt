package com.example.ui.components.Dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun AiErrorDialog(
    isAtpConnected: Boolean,
    connectionError: String?,
    onDismiss: () -> Unit
) {
    if (isAtpConnected) {
        onDismiss()
        return
    }
    
    if (connectionError != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Lỗi kết nối AI") },
            text = { Text("Nguyên nhân: $connectionError") },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("Đóng")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Đang kết nối AI...") },
            text = { Text("Vui lòng đợi một chút hoặc kiểm tra xem ứng dụng AI Portal có đang chạy không.") },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("Đóng")
                }
            }
        )
    }
}
