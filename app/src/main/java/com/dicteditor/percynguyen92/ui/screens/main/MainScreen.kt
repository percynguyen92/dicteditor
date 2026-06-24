package com.dicteditor.percynguyen92.ui.screens.main

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dicteditor.percynguyen92.ExportActivity
import com.dicteditor.percynguyen92.ExportSession
import com.dicteditor.percynguyen92.R
import com.dicteditor.percynguyen92.WordFormActivity
import com.dicteditor.percynguyen92.aitranslateportal.AiPortalConnectionManager
import com.dicteditor.percynguyen92.data.DictEntry
import com.dicteditor.percynguyen92.data.EntryOpResult
import com.dicteditor.percynguyen92.ui.screens.about.AboutScreen
import com.dicteditor.percynguyen92.ui.screens.main.components.AppSideEffects
import com.dicteditor.percynguyen92.ui.screens.main.components.AppTopBar
import com.dicteditor.percynguyen92.ui.screens.main.components.BulkSelectionBar
import com.dicteditor.percynguyen92.ui.screens.main.components.PaginationBar
import com.dicteditor.percynguyen92.ui.components.CustomSnackbarVisuals
import com.dicteditor.percynguyen92.ui.screens.main.components.MainContentArea
import com.dicteditor.percynguyen92.ui.components.appBackground
import androidx.paging.compose.collectAsLazyPagingItems
import com.dicteditor.percynguyen92.ui.screens.main.components.dialogs.AppDialogs
import com.dicteditor.percynguyen92.ui.components.hazeGlassmorphism
import com.dicteditor.percynguyen92.ui.components.showCustomSnackbar
import com.dicteditor.percynguyen92.ui.theme.DarkColors
import com.dicteditor.percynguyen92.ui.theme.LightColors
import com.dicteditor.percynguyen92.viewmodel.DictionaryViewModel
import com.dicteditor.percynguyen92.viewmodel.SnackbarType
import com.dicteditor.percynguyen92.utils.UiText
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import kotlin.math.ceil

@Composable
fun MainScreen(
    viewModel: DictionaryViewModel,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val hazeState = remember { HazeState() }

    // State flows from ViewModel
    val displayEntries = viewModel.entryPagingFlow.collectAsLazyPagingItems()
    val recentFiles by viewModel.recentFiles.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchUseRegex by viewModel.searchUseRegex.collectAsStateWithLifecycle()
    val searchMatchCase by viewModel.searchMatchCase.collectAsStateWithLifecycle()
    val highlightedIds by viewModel.highlightedIds.collectAsStateWithLifecycle()
    val totalWords by viewModel.totalWords.collectAsStateWithLifecycle()
    val filteredCount by viewModel.filteredEntriesCount.collectAsStateWithLifecycle()
    val openedFileUri by viewModel.openedFileUri.collectAsStateWithLifecycle()
    val statusMessageUiText by viewModel.statusMessage.collectAsStateWithLifecycle()
    val defaultStatus = if (openedFileUri == null) stringResource(R.string.status_pick_file) else stringResource(R.string.status_ready)
    val statusMessage = statusMessageUiText?.asString(context) ?: defaultStatus

    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val fileLoadError by viewModel.fileLoadError.collectAsStateWithLifecycle()
    val searchError by viewModel.searchError.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.checkForUpdates(com.dicteditor.percynguyen92.BuildConfig.VERSION_NAME)
    }
    
    // Dialog state controllers
    var showBatchImportDialog by remember { mutableStateOf(false) }
    var showExitWarningDialog by remember { mutableStateOf(false) }
    var showCloseFileWarningDialog by remember { mutableStateOf(false) }
    var showBulkDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAiErrorDialog by remember { mutableStateOf(false) }
    var showAboutScreen by remember { mutableStateOf(false) }

    // Replace mode state
    var isReplaceMode by remember { mutableStateOf(false) }
    var replaceQuery by remember { mutableStateOf("") }

    // Edit entry state
    var editEntryTarget by remember { mutableStateOf<DictEntry?>(null) }

    // Bulk selection state
    val selectedIds = remember { mutableStateMapOf<String, Boolean>() }
    val selectedCount = selectedIds.count { it.value }
    val isBulkMode = selectedCount > 0
    val coroutineScope = rememberCoroutineScope()
    
    val atpConnectionManager = remember { AiPortalConnectionManager(context) }
    val isAtpConnected by atpConnectionManager.isConnected.collectAsStateWithLifecycle()
    val connectionError by atpConnectionManager.connectionError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    val snackbarUpdatedWord = stringResource(R.string.snackbar_updated_word)
    val snackbarAddedWord = stringResource(R.string.snackbar_added_word)
    val snackbarAiConnectedOk = stringResource(R.string.snackbar_ai_connected_ok)

    val lazyListState = rememberLazyListState()
    val pageSize = 200
    val currentPage by remember {
        derivedStateOf {
            if (filteredCount == 0) 0 
            else lazyListState.firstVisibleItemIndex / pageSize
        }
    }
    val totalPages by remember {
        derivedStateOf {
            if (filteredCount == 0) 0
            else (filteredCount + pageSize - 1) / pageSize
        }
    }

    // Manage effects and connection managers
    AppSideEffects(
        context = context,
        openedFileUri = openedFileUri,
        viewModel = viewModel,
        atpConnectionManager = atpConnectionManager,
        selectedIds = selectedIds,
        snackbarHostState = snackbarHostState
    )

    // Intercept back actions
    BackHandler(enabled = showAboutScreen || openedFileUri != null || hasUnsavedChanges) {
        if (showAboutScreen) {
            showAboutScreen = false
            return@BackHandler
        }
        if (openedFileUri != null) {
            if (hasUnsavedChanges) {
                showCloseFileWarningDialog = true
            } else {
                viewModel.closeFile()
            }
            return@BackHandler
        }
        if (hasUnsavedChanges) {
            showExitWarningDialog = true
        }
    }

    // Create Document picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            // Save perm for persistence across restarts
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        viewModel.loadFile(context, uri)
    }

    // Set up Activity Result Launcher for Word Form
    val wordFormLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        focusManager.clearFocus()
        keyboardController?.hide()

        if (result.resultCode != Activity.RESULT_OK) {
            editEntryTarget = null
            return@rememberLauncherForActivityResult
        }
        
        val data = result.data ?: return@rememberLauncherForActivityResult
        val chinese = data.getStringExtra("RESULT_CHINESE") ?: return@rememberLauncherForActivityResult
        val meanings = data.getStringArrayExtra("RESULT_MEANINGS")?.toList() ?: return@rememberLauncherForActivityResult
        val resultEntryId = data.getStringExtra("RESULT_ENTRY_ID")
        
        coroutineScope.launch {
            val target = editEntryTarget
            val finalId = resultEntryId ?: target?.id
            
            if (finalId != null) {
                val opResult = viewModel.updateEntry(finalId, chinese, meanings)
                when (opResult) {
                    EntryOpResult.Success -> {
                        snackbarHostState.showCustomSnackbar(coroutineScope, snackbarUpdatedWord, SnackbarType.SUCCESS)
                    }
                    EntryOpResult.Merged -> {
                        val mergedMsg = UiText.StringResource(
                            R.string.snackbar_merged_word,
                            listOf(chinese)
                        ).asString(context)
                        snackbarHostState.showCustomSnackbar(coroutineScope, mergedMsg, SnackbarType.SUCCESS)
                    }
                    EntryOpResult.Duplicate -> {
                        val dupMsg = UiText.StringResource(
                            R.string.error_duplicate_word_fallback,
                            listOf(chinese)
                        ).asString(context)
                        snackbarHostState.showCustomSnackbar(coroutineScope, dupMsg, SnackbarType.ERROR)
                    }
                    else -> {}
                }
                editEntryTarget = null
            } else {
                val ok = viewModel.addEntry(chinese, meanings)
                if (ok) snackbarHostState.showCustomSnackbar(coroutineScope, snackbarAddedWord, SnackbarType.SUCCESS)
            }
        }
    }

    if (showAboutScreen) {
        AboutScreen(
            onBack = { showAboutScreen = false },
            onCheckUpdates = { onFinished ->
                viewModel.triggerManualUpdateCheck(com.dicteditor.percynguyen92.BuildConfig.VERSION_NAME, onFinished)
            }
        )
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .appBackground(),
            topBar = {
            AppTopBar(
                hazeState = hazeState,
                openedFileUri = openedFileUri,
                totalWords = totalWords,
                hasUnsavedChanges = hasUnsavedChanges,
                canUndo = canUndo,
                canRedo = canRedo,
                searchQuery = searchQuery,
                searchUseRegex = searchUseRegex,
                searchMatchCase = searchMatchCase,
                statusMessage = statusMessage,
                searchError = searchError,
                isAtpConnected = isAtpConnected,
                onUndoClick = viewModel::undo,
                onRedoClick = viewModel::redo,
                onSaveClick = { viewModel.saveFile(context) },
                onOpenFileClick = { filePickerLauncher.launch(arrayOf("text/plain", "*/*")) },
                onToggleRegex = { viewModel.setSearchUseRegex(!searchUseRegex) },
                onToggleMatchCase = { viewModel.setSearchMatchCase(!searchMatchCase) },
                onSortDefaultLengthDescending = viewModel::sortByDefaultLengthDescending,
                onSortLengthAscending = viewModel::sortByLengthAscending,
                onFindReplaceClick = { isReplaceMode = !isReplaceMode },
                onBatchImportClick = { showBatchImportDialog = true },
                onCheckAiConnectionClick = {
                    if (isAtpConnected) {
                        snackbarHostState.showCustomSnackbar(coroutineScope, snackbarAiConnectedOk, SnackbarType.SUCCESS)
                    } else {
                        atpConnectionManager.bindService()
                        showAiErrorDialog = true
                    }
                },
                onExitClick = {
                    if (hasUnsavedChanges) {
                        showExitWarningDialog = true
                    } else {
                        onExit()
                    }
                },
                onAboutClick = { showAboutScreen = true },
                isReplaceMode = isReplaceMode,
                replaceQuery = replaceQuery,
                onSearchQueryChange = {
                    viewModel.setSearchQuery(it)
                    selectedIds.clear()
                },
                onReplaceQueryChange = { replaceQuery = it },
                onClearSearch = {
                    viewModel.setSearchQuery("")
                    selectedIds.clear()
                },
                onReplaceClick = {
                    if (searchQuery.isNotEmpty()) {
                        val scopeIds = viewModel.filteredEntriesIds.value
                        viewModel.findAndReplace(
                            findText = searchQuery,
                            replaceText = replaceQuery,
                            useRegex = searchUseRegex,
                            matchCase = searchMatchCase,
                            scopeIds = scopeIds
                        )
                    }
                },
                onCloseReplaceMode = {
                    isReplaceMode = false
                    replaceQuery = ""
                }
            )
        },
        bottomBar = {
            if (openedFileUri != null && filteredCount > 0) {
                PaginationBar(
                    hazeState = hazeState,
                    currentPage = currentPage,
                    totalPages = totalPages,
                    pageSize = pageSize,
                    filteredCount = filteredCount,
                    onFirstPage = { coroutineScope.launch { lazyListState.scrollToItem(0) } },
                    onPrevPage = { coroutineScope.launch { lazyListState.scrollToItem(maxOf(0, (currentPage - 1) * pageSize)) } },
                    onNextPage = { coroutineScope.launch { lazyListState.scrollToItem(minOf(filteredCount - 1, (currentPage + 1) * pageSize)) } },
                    onLastPage = { coroutineScope.launch { lazyListState.scrollToItem(maxOf(0, (totalPages - 1) * pageSize)) } },
                    onJumpToPage = { page -> coroutineScope.launch { lazyListState.scrollToItem(page * pageSize) } }
                )
            }
        },
        floatingActionButton = {
            if (openedFileUri != null) {
                BulkSelectionBar(
                    hazeState = hazeState,
                    selectedCount = selectedCount,
                    onSelectAll = {
                        viewModel.filteredEntriesIds.value.forEach { id ->
                            selectedIds[id] = true
                        }
                    },
                    onClearSelection = selectedIds::clear,
                    onBulkDeleteClick = { showBulkDeleteConfirmDialog = true },
                    onBulkExportClick = { 
                        val selected = selectedIds.filterValues { it }.keys.toSet()
                        coroutineScope.launch {
                            val exportStr = viewModel.getExportStringForSelected(selected)
                            ExportSession.exportText = exportStr
                            context.startActivity(Intent(context, ExportActivity::class.java))
                            selectedIds.clear()
                        }
                    },
                    onFabClick = {
                        editEntryTarget = null
                        val intent = Intent(context, WordFormActivity::class.java).apply {
                            putExtra("EXTRA_EDIT_MODE", false)
                        }
                        wordFormLauncher.launch(intent)
                    }
                )
            }
        },
        snackbarHost = {}
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            MainContentArea(
                hazeState = hazeState,
                openedFileUri = openedFileUri,
                isLoading = isLoading,
                displayEntries = displayEntries,
                recentFiles = recentFiles,
                fileLoadError = fileLoadError,
                onClearError = viewModel::clearFileLoadError,
                searchQuery = searchQuery,
                selectedIds = selectedIds,
                highlightedIds = highlightedIds,
                isBulkMode = isBulkMode,
                onFileClick = { uri -> viewModel.loadFile(context, uri) },
                onOpenNewClick = { filePickerLauncher.launch(arrayOf("text/plain", "*/*")) },
                onSearchClear = { viewModel.setSearchQuery("") },
                searchError = searchError,
                onAddWordClick = {
                    editEntryTarget = null
                    val intent = Intent(context, WordFormActivity::class.java).apply {
                        putExtra("EXTRA_EDIT_MODE", false)
                    }
                    wordFormLauncher.launch(intent)
                },
                onEditClick = { entry ->
                    editEntryTarget = entry
                    val intent = Intent(context, WordFormActivity::class.java).apply {
                        putExtra("EXTRA_EDIT_MODE", true)
                        putExtra("EXTRA_ENTRY_ID", entry.id)
                        putExtra("EXTRA_CHINESE", entry.chinese)
                        putExtra("EXTRA_MEANINGS", entry.meanings.toTypedArray())
                    }
                    wordFormLauncher.launch(intent)
                },
                onDeleteConfirm = viewModel::deleteEntry,
                onSelectedChange = { id, isChecked ->
                    if (isChecked) {
                        selectedIds[id] = true
                    } else {
                        selectedIds.remove(id)
                    }
                },
                lazyListState = lazyListState,
                modifier = Modifier.fillMaxSize().hazeSource(hazeState),
                contentPadding = innerPadding
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = innerPadding.calculateTopPadding())
            ) { data ->
                val isDark = true
                val type = (data.visuals as? CustomSnackbarVisuals)?.type ?: SnackbarType.INFO
                val statusColor = when (type) {
                    SnackbarType.SUCCESS -> if (isDark) DarkColors.Success else LightColors.Success
                    SnackbarType.ERROR -> if (isDark) DarkColors.Error else LightColors.Error
                    SnackbarType.INFO -> if (isDark) DarkColors.Info else LightColors.Info
                }
                val bgTint = statusColor.copy(alpha = 0.85f)
                val contentColor = Color.White

                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .hazeGlassmorphism(
                            state = hazeState,
                            isDarkTheme = isDark,
                            tint = bgTint,
                            borderColor = statusColor.copy(alpha = 0.5f)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = data.visuals.message,
                            color = contentColor,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        IconButton(
                            onClick = { data.dismiss() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.description_close),
                                tint = contentColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
    }

    AppDialogs(
        context = context,
        viewModel = viewModel,
        hazeState = hazeState,
        showBatchImportDialog = showBatchImportDialog,
        onDismissBatchImport = { showBatchImportDialog = false },
        showCloseFileWarningDialog = showCloseFileWarningDialog,
        onDismissCloseFileWarning = { showCloseFileWarningDialog = false },
        onConfirmCloseFile = { save ->
            if (save) viewModel.saveFile(context)
            viewModel.closeFile()
        },
        showExitWarningDialog = showExitWarningDialog,
        onDismissExitWarning = { showExitWarningDialog = false },
        onConfirmExit = { save ->
            if (save) viewModel.saveFile(context) else viewModel.resetUnsavedChanges()
            onExit()
        },
        showBulkDeleteConfirmDialog = showBulkDeleteConfirmDialog,
        onDismissBulkDeleteConfirm = { showBulkDeleteConfirmDialog = false },
        selectedIds = selectedIds,
        onConfirmBulkDelete = {
            viewModel.deleteEntries(selectedIds.filterValues { it }.keys.toSet())
            selectedIds.clear()
        },
        showAiErrorDialog = showAiErrorDialog,
        onDismissAiError = { showAiErrorDialog = false },
        isAtpConnected = isAtpConnected,
        connectionError = connectionError,
        updateInfo = updateInfo,
        onDismissUpdate = viewModel::dismissUpdateDialog,
        onConfirmUpdate = {
            updateInfo?.releaseUrl?.let { url ->
                if (url.isNotEmpty()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    )
}
