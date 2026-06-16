package com.dicteditor.percynguyen92.ui.components.dialogs

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ExitWarningDialog(
    onDismiss: () -> Unit,
    onSaveAndExit: () -> Unit,
    onDiscardAndExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thay đổi chưa lưu!", style = MaterialTheme.typography.titleLarge) },
        text = { Text("Ứng dụng của bạn có thay đổi từ điển chưa lưu về file gốc. Hãy lưu trước khi rời khỏi hoặc bỏ qua.", style = MaterialTheme.typography.bodySmall) },
        confirmButton = {
            Button(onClick = onSaveAndExit) {
                Text("Lưu & Thoát", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscardAndExit) {
                    Text("Bỏ qua đổi thay", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onDismiss) {
                    Text("Hủy", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    )
}
