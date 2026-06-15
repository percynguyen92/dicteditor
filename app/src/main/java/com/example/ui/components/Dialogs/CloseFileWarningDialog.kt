package com.example.ui.components.Dialogs

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun CloseFileWarningDialog(
    onDismiss: () -> Unit,
    onSaveAndClose: () -> Unit,
    onDiscardAndClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thay đổi chưa lưu!") },
        text = { Text("Bạn có thay đổi chưa lưu. Hãy lưu trước khi đóng file hoặc bỏ qua.") },
        confirmButton = {
            Button(onClick = onSaveAndClose) {
                Text("Lưu & Đóng")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscardAndClose) {
                    Text("Bỏ qua", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text("Hủy")
                }
            }
        }
    )
}
