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
import androidx.compose.material3.Scaffold
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
import com.dicteditor.percynguyen92.aitranslateportal.AiPortalConnectionManager
import com.dicteditor.percynguyen92.data.DictEntry
import com.dicteditor.percynguyen92.ui.components.AppTopBar
import com.dicteditor.percynguyen92.ui.components.BulkSelectionBar
import com.dicteditor.percynguyen92.ui.components.MainContentArea
import com.dicteditor.percynguyen92.ui.components.PaginationBar
import com.dicteditor.percynguyen92.ui.components.appBackground
import com.dicteditor.percynguyen92.ui.components.dialogs.AiErrorDialog
import com.dicteditor.percynguyen92.ui.components.dialogs.BatchImportDialog
import com.dicteditor.percynguyen92.ui.components.dialogs.CloseFileWarningDialog
import com.dicteditor.percynguyen92.ui.components.dialogs.ExitWarningDialog
import com.dicteditor.percynguyen92.ui.components.dialogs.FindReplaceDialog
import com.dicteditor.percynguyen92.ui.components.dialogs.BulkDeleteConfirmDialog
import com.dicteditor.percynguyen92.ui.theme.MyApplicationTheme
import com.dicteditor.percynguyen92.viewmodel.DictionaryViewModel
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
    
    // Dialog state controllers
    var showFindReplaceDialog by remember { mutableStateOf(false) }
    var showBatchImportDialog by remember { mutableStateOf(false) }
    var showExitWarningDialog by remember { mutableStateOf(false) }
    var showCloseFileWarningDialog by remember { mutableStateOf(false) }
    var showBulkDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAiErrorDialog by remember { mutableStateOf(false) }

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

    // Manage effects and connection managers
    AppSideEffects(
        context = context,
        openedFileUri = openedFileUri,
        viewModel = viewModel,
        atpConnectionManager = atpConnectionManager,
        selectedIds = selectedIds
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
        if (result.resultCode != Activity.RESULT_OK) {
            editEntryTarget = null
            return@rememberLauncherForActivityResult
        }
        
        val data = result.data ?: return@rememberLauncherForActivityResult
        val chinese = data.getStringExtra("RESULT_CHINESE") ?: return@rememberLauncherForActivityResult
        val meanings = data.getStringArrayExtra("RESULT_MEANINGS")?.toList() ?: return@rememberLauncherForActivityResult
        
        coroutineScope.launch {
            val target = editEntryTarget
            if (target != null) {
                val ok = viewModel.updateEntry(target.id, chinese, meanings)
                if (ok) Toast.makeText(context, "Đã cập nhật từ", Toast.LENGTH_SHORT).show()
                editEntryTarget = null
            } else {
                val ok = viewModel.addEntry(chinese, meanings)
                if (ok) Toast.makeText(context, "Đã thêm từ mới vào list", Toast.LENGTH_SHORT).show()
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
                onUndoClick = viewModel::undo,
                onRedoClick = viewModel::redo,
                onSaveClick = { viewModel.saveFile(context) },
                onOpenFileClick = { filePickerLauncher.launch(arrayOf("text/plain", "*/*")) },
                onToggleRegex = { viewModel.setSearchUseRegex(!searchUseRegex) },
                onToggleMatchCase = { viewModel.setSearchMatchCase(!searchMatchCase) },
                onSortDefaultLengthDescending = viewModel::sortByDefaultLengthDescending,
                onSortLengthAscending = viewModel::sortByLengthAscending,
                onFindReplaceClick = { showFindReplaceDialog = true },
                onBatchImportClick = { showBatchImportDialog = true },
                onCheckAiConnectionClick = {
                    if (isAtpConnected) {
                        Toast.makeText(context, "Kết nối AI (AIDL) đang hoạt động tốt.", Toast.LENGTH_SHORT).show()
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
                onSearchQueryChange = {
                    viewModel.setSearchQuery(it)
                    selectedIds.clear()
                },
                onClearSearch = {
                    viewModel.setSearchQuery("")
                    selectedIds.clear()
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
        }
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
                searchQuery = searchQuery,
                selectedIds = selectedIds,
                highlightedIds = highlightedIds,
                isBulkMode = isBulkMode,
                onFileClick = { uri -> viewModel.loadFile(context, uri) },
                onOpenNewClick = { filePickerLauncher.launch(arrayOf("text/plain", "*/*")) },
                onSearchClear = { viewModel.setSearchQuery("") },
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
        }
    }

    AppDialogs(
        context = context,
        viewModel = viewModel,
        showFindReplaceDialog = showFindReplaceDialog,
        onDismissFindReplace = { showFindReplaceDialog = false },
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
    selectedIds: MutableMap<String, Boolean>
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

    // Collect SharedFlow events for side effects (Toasts)
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun AppDialogs(
    context: Context,
    viewModel: DictionaryViewModel,
    showFindReplaceDialog: Boolean,
    onDismissFindReplace: () -> Unit,
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
    if (showFindReplaceDialog) {
        FindReplaceDialog(
            onDismiss = onDismissFindReplace,
            onReplaceAll = { find, replace, useRegex ->
                viewModel.findAndReplace(find, replace, useRegex)
                onDismissFindReplace()
            }
        )
    }

    if (showBatchImportDialog) {
        BatchImportDialog(
            onDismiss = onDismissBatchImport,
            onImport = { rawText ->
                viewModel.batchImport(rawText)
                onDismissBatchImport()
            }
        )
    }

    if (showCloseFileWarningDialog) {
        CloseFileWarningDialog(
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
            isAtpConnected = isAtpConnected,
            connectionError = connectionError,
            onDismiss = onDismissAiError
        )
    }
}

