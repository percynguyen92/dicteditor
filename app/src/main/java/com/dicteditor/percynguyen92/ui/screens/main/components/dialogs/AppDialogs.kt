package com.dicteditor.percynguyen92.ui.screens.main.components.dialogs

import android.content.Context
import androidx.compose.runtime.Composable
import com.dicteditor.percynguyen92.viewmodel.DictionaryViewModel
import dev.chrisbanes.haze.HazeState
import com.dicteditor.percynguyen92.utils.UpdateInfo

@Composable
fun AppDialogs(
    context: Context,
    viewModel: DictionaryViewModel,
    hazeState: HazeState,
    showCloseFileWarningDialog: Boolean,
    onDismissCloseFileWarning: () -> Unit,
    onConfirmCloseFile: (Boolean) -> Unit,
    showExitWarningDialog: Boolean,
    onDismissExitWarning: () -> Unit,
    onConfirmExit: (Boolean) -> Unit,
    showBulkDeleteConfirmDialog: Boolean,
    onDismissBulkDeleteConfirm: () -> Unit,
    selectedIds: Map<String, Boolean>,
    onConfirmBulkDelete: () -> Unit,
    showAiErrorDialog: Boolean,
    onDismissAiError: () -> Unit,
    isAtpConnected: Boolean,
    connectionError: String?,
    updateInfo: UpdateInfo?,
    onDismissUpdate: () -> Unit,
    onConfirmUpdate: () -> Unit,
    duplicateFoundCount: Int?,
    onDismissDuplicateFilter: () -> Unit,
    onConfirmDuplicateFilter: () -> Unit
) {
    if (updateInfo != null) {
        UpdateDialog(
            hazeState = hazeState,
            updateInfo = updateInfo,
            onDismiss = onDismissUpdate,
            onUpdate = {
                onConfirmUpdate()
                onDismissUpdate()
            }
        )
    }


    if (showCloseFileWarningDialog) {
        CloseFileWarningDialog(
            hazeState = hazeState,
            onDismiss = onDismissCloseFileWarning,
            onSaveAndClose = {
                onDismissCloseFileWarning()
                onConfirmCloseFile(true)
            },
            onDiscardAndClose = {
                onDismissCloseFileWarning()
                onConfirmCloseFile(false)
            }
        )
    }

    if (showExitWarningDialog) {
        ExitWarningDialog(
            hazeState = hazeState,
            onDismiss = onDismissExitWarning,
            onSaveAndExit = {
                onDismissExitWarning()
                onConfirmExit(true)
            },
            onDiscardAndExit = {
                onDismissExitWarning()
                onConfirmExit(false)
            }
        )
    }

    if (showBulkDeleteConfirmDialog) {
        BulkDeleteConfirmDialog(
            hazeState = hazeState,
            selectedCount = selectedIds.count { it.value },
            onDismiss = onDismissBulkDeleteConfirm,
            onConfirmDelete = {
                onConfirmBulkDelete()
                onDismissBulkDeleteConfirm()
            }
        )
    }

    if (showAiErrorDialog) {
        AiErrorDialog(
            hazeState = hazeState,
            isAtpConnected = isAtpConnected,
            connectionError = connectionError,
            onDismiss = onDismissAiError
        )
    }

    if (duplicateFoundCount != null && duplicateFoundCount > 0) {
        DuplicateFilterDialog(
            hazeState = hazeState,
            duplicateCount = duplicateFoundCount,
            onDismiss = onDismissDuplicateFilter,
            onConfirm = onConfirmDuplicateFilter
        )
    }
}
