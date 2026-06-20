package com.dicteditor.percynguyen92.ui.components.dialogs

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dicteditor.percynguyen92.R
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
        title = { Text(stringResource(R.string.dialog_unsaved_changes_title), style = MaterialTheme.typography.titleLarge) },
        text = { Text(stringResource(R.string.dialog_close_file_message), style = MaterialTheme.typography.bodySmall) },
        confirmButton = {
            Button(onClick = onSaveAndClose) {
                Text(stringResource(R.string.dialog_save_and_close), style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscardAndClose) {
                    Text(stringResource(R.string.dialog_discard), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    )
}
