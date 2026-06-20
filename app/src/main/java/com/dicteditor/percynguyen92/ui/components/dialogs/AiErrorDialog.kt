@file:Suppress("PackageName")

package com.dicteditor.percynguyen92.ui.components.dialogs

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.dicteditor.percynguyen92.R
import com.dicteditor.percynguyen92.ui.components.HazeAlertDialog
import dev.chrisbanes.haze.HazeState

@Composable
fun AiErrorDialog(
    hazeState: HazeState,
    isAtpConnected: Boolean,
    connectionError: String?,
    onDismiss: () -> Unit
) {
    LaunchedEffect(isAtpConnected) {
        if (isAtpConnected) {
            onDismiss()
        }
    }
    
    if (isAtpConnected) {
        return
    }
    
    if (connectionError != null) {
        HazeAlertDialog(
            hazeState = hazeState,
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.dialog_ai_error_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(stringResource(R.string.dialog_ai_error_cause, connectionError), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.description_close), style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    } else {
        HazeAlertDialog(
            hazeState = hazeState,
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.dialog_ai_connecting_title), style = MaterialTheme.typography.titleLarge) },
            text = { Text(stringResource(R.string.dialog_ai_connecting_message), style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.description_close), style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}
