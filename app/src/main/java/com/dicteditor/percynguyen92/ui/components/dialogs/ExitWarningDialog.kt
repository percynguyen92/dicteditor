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
fun ExitWarningDialog(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onSaveAndExit: () -> Unit,
    onDiscardAndExit: () -> Unit
) {
    HazeAlertDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_unsaved_changes_title), style = MaterialTheme.typography.titleLarge) },
        text = { Text(stringResource(R.string.dialog_exit_message), style = MaterialTheme.typography.bodySmall) },
        confirmButton = {
            Button(onClick = onSaveAndExit) {
                Text(stringResource(R.string.dialog_save_and_exit), style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscardAndExit) {
                    Text(stringResource(R.string.dialog_discard_changes), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    )
}
