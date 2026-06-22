package com.dicteditor.percynguyen92.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import com.dicteditor.percynguyen92.viewmodel.SnackbarType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CustomSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    val type: SnackbarType = SnackbarType.INFO
) : SnackbarVisuals

fun SnackbarHostState.showCustomSnackbar(
    scope: CoroutineScope,
    message: String,
    type: SnackbarType
) {
    val durationMs = if (type == SnackbarType.ERROR) 4000L else 2000L
    scope.launch {
        val dismissJob = launch {
            delay(durationMs)
            currentSnackbarData?.dismiss()
        }
        showSnackbar(
            CustomSnackbarVisuals(
                message = message,
                type = type,
                duration = SnackbarDuration.Indefinite
            )
        )
        dismissJob.cancel()
    }
}
