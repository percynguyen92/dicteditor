package com.dicteditor.percynguyen92.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dicteditor.percynguyen92.ui.components.glassTextFieldColors
import com.dicteditor.percynguyen92.ui.components.HazeAlertDialog
import dev.chrisbanes.haze.HazeState

@Composable
fun BatchImportDialog(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onImport: (rawText: String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    HazeAlertDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nhập từ hàng loạt (Batch)",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Dán các dòng từ điển có định dạng 'tiếng_Trung=nghĩa1/nghĩa2'. Mỗi từ nằm trên 1 dòng độc lập. Trùng lặp từ đã có sẽ được bỏ qua.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = rawText,
                    onValueChange = {
                        rawText = it
                        if (it.isNotBlank()) errorText = null
                    },
                    placeholder = { Text("E.g.\n龙=rồng/long\n火=lửa/hỏa\n水=nước/thủy") },
                    minLines = 6,
                    maxLines = 12,
                    isError = errorText != null,
                    supportingText = {
                        if (errorText != null) {
                            Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                        }
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = glassTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rawText.isBlank()) {
                        errorText = "Nội dung paste không được rỗng"
                    } else {
                        onImport(rawText)
                    }
                }
            ) {
                Text("Import", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
