package com.dicteditor.percynguyen92

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dicteditor.percynguyen92.ui.components.hazeGlassmorphism
import androidx.compose.foundation.isSystemInDarkTheme
import com.dicteditor.percynguyen92.ui.theme.DarkColors
import com.dicteditor.percynguyen92.ui.theme.LightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.dicteditor.percynguyen92.aitranslateportal.AiPortalConnectionManager
import com.dicteditor.percynguyen92.data.DictEntry
import com.dicteditor.percynguyen92.data.EntryOpResult
import com.dicteditor.percynguyen92.ui.components.AppTopBar
import com.dicteditor.percynguyen92.ui.components.BulkSelectionBar
import com.dicteditor.percynguyen92.ui.components.MainContentArea
import com.dicteditor.percynguyen92.ui.components.PaginationBar
import com.dicteditor.percynguyen92.ui.components.appBackground
import com.dicteditor.percynguyen92.ui.components.dialogs.AiErrorDialog
import com.dicteditor.percynguyen92.ui.components.dialogs.BatchImportDialog
import com.dicteditor.percynguyen92.ui.components.dialogs.CloseFileWarningDialog
import com.dicteditor.percynguyen92.ui.components.dialogs.ExitWarningDialog
import com.dicteditor.percynguyen92.ui.components.dialogs.BulkDeleteConfirmDialog
import com.dicteditor.percynguyen92.ui.theme.MyApplicationTheme
import com.dicteditor.percynguyen92.viewmodel.DictionaryViewModel
import com.dicteditor.percynguyen92.viewmodel.SnackbarType
import com.dicteditor.percynguyen92.viewmodel.UiSnackbarEvent
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: DictionaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Load recent files
        viewModel.loadRecentFiles(applicationContext)

        setContent {
            MyApplicationTheme {
                DictEditorApp(viewModel = viewModel, onExit = {
                    window.decorView.postDelayed({ finish() }, 100)
                })
            }
        }
    }
}

@Composable
fun DictEditorApp(
    viewModel: DictionaryViewModel,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val hazeState = remember { HazeState() }

    // State flows from ViewModel
    val displayEntries by viewModel.displayEntries.collectAsStateWithLifecycle()
    val recentFiles by viewModel.recentFiles.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchUseRegex by viewModel.searchUseRegex.collectAsStateWithLifecycle()
    val searchMatchCase by viewModel.searchMatchCase.collectAsStateWithLifecycle()
    val highlightedIds by viewModel.highlightedIds.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
    val totalWords by viewModel.totalWords.collectAsStateWithLifecycle()
    val filteredCount by viewModel.filteredEntriesCount.collectAsStateWithLifecycle()
    val openedFileUri by viewModel.openedFileUri.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val fileLoadError by viewModel.fileLoadError.collectAsStateWithLifecycle()
    val searchError by viewModel.searchError.collectAsStateWithLifecycle()
    
    // Dialog state controllers
    var showBatchImportDialog by remember { mutableStateOf(false) }
    var showExitWarningDialog by remember { mutableStateOf(false) }
    var showCloseFileWarningDialog by remember { mutableStateOf(false) }
    var showBulkDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAiErrorDialog by remember { mutableStateOf(false) }

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
    BackHandler(enabled = openedFileUri != null || hasUnsavedChanges) {
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
                        snackbarHostState.showSnackbar(CustomSnackbarVisuals(snackbarUpdatedWord, type = SnackbarType.SUCCESS))
                    }
                    EntryOpResult.Merged -> {
                        val mergedMsg = context.getString(R.string.snackbar_merged_word, chinese)
                        snackbarHostState.showSnackbar(CustomSnackbarVisuals(mergedMsg, type = SnackbarType.SUCCESS))
                    }
                    EntryOpResult.Duplicate -> {
                        val dupMsg = context.getString(R.string.error_duplicate_word_fallback, chinese)
                        snackbarHostState.showSnackbar(CustomSnackbarVisuals(dupMsg, type = SnackbarType.ERROR))
                    }
                    else -> {}
                }
                editEntryTarget = null
            } else {
                val ok = viewModel.addEntry(chinese, meanings)
                if (ok) snackbarHostState.showSnackbar(CustomSnackbarVisuals(snackbarAddedWord, type = SnackbarType.SUCCESS))
            }
        }
    }

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
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(CustomSnackbarVisuals(snackbarAiConnectedOk, type = SnackbarType.SUCCESS))
                        }
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
                        val scopeIds = viewModel.filteredEntriesIds
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
            if (openedFileUri != null && filteredCount > 0 && totalPages > 0) {
                PaginationBar(
                    hazeState = hazeState,
                    currentPage = currentPage,
                    totalPages = totalPages,
                    pageSize = viewModel.pageSize,
                    filteredCount = filteredCount,
                    onFirstPage = viewModel::firstPage,
                    onPrevPage = viewModel::prevPage,
                    onNextPage = viewModel::nextPage,
                    onLastPage = viewModel::lastPage,
                    onJumpToPage = viewModel::jumpToPage
                )
            }
        },
        floatingActionButton = {
            if (openedFileUri != null) {
                BulkSelectionBar(
                    hazeState = hazeState,
                    selectedCount = selectedCount,
                    onSelectAll = {
                        displayEntries.forEach { entry ->
                            selectedIds[entry.id] = true
                        }
                    },
                    onClearSelection = selectedIds::clear,
                    onBulkDeleteClick = { showBulkDeleteConfirmDialog = true },
                    onBulkExportClick = { 
                        val selected = selectedIds.filterValues { it }.keys.toSet()
                        val exportStr = viewModel.getExportStringForSelected(selected)
                        ExportSession.exportText = exportStr
                        context.startActivity(Intent(context, ExportActivity::class.java))
                        selectedIds.clear()
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
                .hazeSource(hazeState)
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = innerPadding.calculateTopPadding())
            ) { data ->
                val isDark = isSystemInDarkTheme()
                val type = (data.visuals as? CustomSnackbarVisuals)?.type ?: SnackbarType.INFO
                val textColor = when (type) {
                    SnackbarType.SUCCESS -> if (isDark) DarkColors.Success else LightColors.Success
                    SnackbarType.ERROR -> if (isDark) DarkColors.Error else LightColors.Error
                    SnackbarType.INFO -> if (isDark) DarkColors.Info else LightColors.Info
                }
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .hazeGlassmorphism(
                            state = hazeState
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
                            color = textColor,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        IconButton(
                            onClick = { data.dismiss() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.description_close),
                                tint = MaterialTheme.colorScheme.primary
                            )
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
        connectionError = connectionError
    )
}

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
            snackbarHostState.showSnackbar(
                CustomSnackbarVisuals(
                    message = event.message,
                    type = event.type
                )
            )
        }
    }
}

@Composable
fun AppDialogs(
    context: Context,
    viewModel: DictionaryViewModel,
    hazeState: HazeState,
    showBatchImportDialog: Boolean,
    onDismissBatchImport: () -> Unit,
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
    connectionError: String?
) {
    if (showBatchImportDialog) {
        BatchImportDialog(
            hazeState = hazeState,
            onDismiss = onDismissBatchImport,
            onImport = { rawText ->
                viewModel.batchImport(rawText)
                onDismissBatchImport()
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
}

class CustomSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: androidx.compose.material3.SnackbarDuration = androidx.compose.material3.SnackbarDuration.Short,
    val type: SnackbarType = SnackbarType.INFO
) : androidx.compose.material3.SnackbarVisuals

