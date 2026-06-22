package com.dicteditor.percynguyen92.ui.screens.main.components

import com.dicteditor.percynguyen92.ui.components.showCustomSnackbar
import android.content.Context
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.dicteditor.percynguyen92.aitranslateportal.AiPortalConnectionManager
import com.dicteditor.percynguyen92.viewmodel.DictionaryViewModel
import com.dicteditor.percynguyen92.utils.UiText

@Composable
fun AppSideEffects(
    context: Context,
    openedFileUri: Uri?,
    viewModel: DictionaryViewModel,
    atpConnectionManager: AiPortalConnectionManager,
    selectedIds: MutableMap<String, Boolean>,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(Unit) {
        atpConnectionManager.bindService()
    }

    DisposableEffect(Unit) {
        onDispose {
            atpConnectionManager.unbindService()
        }
    }

    LaunchedEffect(openedFileUri) {
        selectedIds.clear()
    }

    // Collect SharedFlow events for side effects (Toasts -> Snackbars)
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            snackbarHostState.showCustomSnackbar(
                scope = this,
                message = event.message.asString(context),
                type = event.type
            )
        }
    }
}

