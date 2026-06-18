package com.dicteditor.percynguyen92.ui.components.dialogs

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.dicteditor.percynguyen92.ui.components.HazeAlertDialog
import dev.chrisbanes.haze.HazeState

@Composable
fun CloseFileWarningDialog(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onSaveAndClose: () -> Unit,
    onDiscardAndClose: () -> Unit
) {
    HazeAlertDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        title = { Text("Thay đổi chưa lưu!", style = MaterialTheme.typography.titleLarge) },
        text = { Text("Bạn có thay đổi chưa lưu. Hãy lưu trước khi đóng file hoặc bỏ qua.", style = MaterialTheme.typography.bodySmall) },
        confirmButton = {
            Button(onClick = onSaveAndClose) {
                Text("Lưu & Đóng", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscardAndClose) {
                    Text("Bỏ qua", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onDismiss) {
                    Text("Hủy", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    )
}
