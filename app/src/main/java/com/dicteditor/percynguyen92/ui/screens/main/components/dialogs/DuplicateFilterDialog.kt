package com.dicteditor.percynguyen92.ui.screens.main.components.dialogs

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
fun DuplicateFilterDialog(
    hazeState: HazeState,
    duplicateCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    HazeAlertDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_duplicate_title), style = MaterialTheme.typography.titleLarge) },
        text = { Text(stringResource(R.string.dialog_duplicate_message, duplicateCount), style = MaterialTheme.typography.bodySmall) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.dialog_duplicate_confirm), style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_duplicate_dismiss), style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
