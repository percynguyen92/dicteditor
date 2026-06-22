package com.dicteditor.percynguyen92.ui.screens.main.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.dicteditor.percynguyen92.R
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

    val emptyError = stringResource(R.string.dialog_batch_import_error_empty)

    HazeAlertDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_batch_import_title),
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
                    text = stringResource(R.string.dialog_batch_import_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = rawText,
                    onValueChange = {
                        rawText = it
                        if (it.isNotBlank()) errorText = null
                    },
                    placeholder = { Text(stringResource(R.string.dialog_batch_import_hint)) },
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
                        errorText = emptyError
                    } else {
                        onImport(rawText)
                    }
                }
            ) {
                Text(stringResource(R.string.dialog_import), style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel), style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
