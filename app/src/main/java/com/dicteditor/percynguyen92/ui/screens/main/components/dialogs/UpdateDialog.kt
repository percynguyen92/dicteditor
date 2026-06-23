package com.dicteditor.percynguyen92.ui.screens.main.components.dialogs

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dicteditor.percynguyen92.R
import com.dicteditor.percynguyen92.ui.components.HazeAlertDialog
import com.dicteditor.percynguyen92.utils.UpdateInfo
import dev.chrisbanes.haze.HazeState

@Composable
fun UpdateDialog(
    hazeState: HazeState,
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    HazeAlertDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_update_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(
                    R.string.dialog_update_message,
                    updateInfo.latestVersion,
                    updateInfo.changelog
                ),
                style = MaterialTheme.typography.bodySmall
            )
        },
        confirmButton = {
            Button(onClick = onUpdate) {
                Text(
                    text = stringResource(R.string.dialog_update_button_update),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.dialog_update_button_later),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}
