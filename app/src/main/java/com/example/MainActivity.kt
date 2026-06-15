package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DictEntry
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DictionaryViewModel
import com.example.ui.components.appBackground
import com.example.ui.components.glassTextFieldColors
import com.example.ui.components.*
import com.example.ui.components.Dialogs.*
import com.example.utils.getFileName
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

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
    var showAddDialog by remember { mutableStateOf(false) }
    var editEntryTarget by remember { mutableStateOf<DictEntry?>(null) }
    var showFindReplaceDialog by remember { mutableStateOf(false) }
    var showBatchImportDialog by remember { mutableStateOf(false) }
    var showExitWarningDialog by remember { mutableStateOf(false) }
    var showCloseFileWarningDialog by remember { mutableStateOf(false) }
    var showBulkDeleteConfirmDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Bulk selection state
    val selectedIds = remember { mutableStateMapOf<String, Boolean>() }
    val selectedCount = selectedIds.count { it.value }
    val isBulkMode = selectedCount > 0
    val coroutineScope = rememberCoroutineScope()
    
    val atpConnectionManager = remember { com.example.aitranslateportal.AiPortalConnectionManager(context) }
    val isAtpConnected by atpConnectionManager.isConnected.collectAsStateWithLifecycle()
    val connectionError by atpConnectionManager.connectionError.collectAsStateWithLifecycle()
    var showAiErrorDialog by remember { mutableStateOf(false) }

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

    // Intercept back actions
    BackHandler(enabled = openedFileUri != null || hasUnsavedChanges) {
        if (openedFileUri != null) {
            if (hasUnsavedChanges) {
                showCloseFileWarningDialog = true
            } else {
                viewModel.closeFile()
            }
        } else {
            if (hasUnsavedChanges) {
                showExitWarningDialog = true
            }
        }
    }

    // Create Document picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                // Save perm for persistence across restarts
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            viewModel.loadFile(context, uri)
        }
    }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val selected = selectedIds.filterValues { it }.keys.toSet()
            viewModel.exportSelectedEntries(context, uri, selected)
            selectedIds.clear()
        }
    }

    // Set up Activity Result Launcher for Word Form
    val wordFormLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val chinese = data?.getStringExtra("RESULT_CHINESE") ?: return@rememberLauncherForActivityResult
            val meanings = data.getStringArrayExtra("RESULT_MEANINGS")?.toList() ?: return@rememberLauncherForActivityResult
            if (editEntryTarget != null) {
                coroutineScope.launch {
                    val ok = viewModel.updateEntry(editEntryTarget!!.id, chinese, meanings)
                    if (ok) {
                        Toast.makeText(context, "Đã cập nhật từ", Toast.LENGTH_SHORT).show()
                    }
                    editEntryTarget = null
                }
            } else {
                coroutineScope.launch {
                    val ok = viewModel.addEntry(chinese, meanings)
                    if (ok) {
                        Toast.makeText(context, "Đã thêm từ mới vào list", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            editEntryTarget = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (openedFileUri != null) {
                val hasPagination = filteredCount > 0 && totalPages > 0
                
                Box(
                    modifier = Modifier.padding(bottom = if (hasPagination) 72.dp else 0.dp)
                ) {
                    BulkSelectionBar(
                        hazeState = hazeState,
                        selectedCount = selectedCount,
                        onSelectAll = {
                            displayEntries.forEach { entry ->
                                selectedIds[entry.id] = true
                            }
                        },
                        onClearSelection = { selectedIds.clear() },
                        onBulkDeleteClick = { showBulkDeleteConfirmDialog = true },
                        onBulkExportClick = { 
                            val selected = selectedIds.filterValues { it }.keys.toSet()
                            val exportStr = viewModel.getExportStringForSelected(selected)
                            com.example.ExportSession.exportText = exportStr
                            context.startActivity(android.content.Intent(context, com.example.ExportActivity::class.java))
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .appBackground()
                .haze(hazeState)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
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
                    onUndoClick = { viewModel.undo() },
                    onRedoClick = { viewModel.redo() },
                    onSaveClick = { viewModel.saveFile(context) },
                    onOpenFileClick = { filePickerLauncher.launch(arrayOf("text/plain", "*/*")) },
                    onToggleRegex = { viewModel.setSearchUseRegex(!searchUseRegex) },
                    onToggleMatchCase = { viewModel.setSearchMatchCase(!searchMatchCase) },
                    onSortDefaultLengthDescending = { viewModel.sortByDefaultLengthDescending() },
                    onSortLengthAscending = { viewModel.sortByLengthAscending() },
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
                    onDeleteConfirm = { id -> viewModel.deleteEntry(id) },
                    onSelectedChange = { id, isChecked ->
                        if (isChecked) {
                            selectedIds[id] = true
                        } else {
                            selectedIds.remove(id)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            AppBottomBar(
                hazeState = hazeState,
                openedFileUri = openedFileUri,
                currentPage = currentPage,
                totalPages = totalPages,
                pageSize = viewModel.pageSize,
                filteredCount = filteredCount,
                onFirstPage = { viewModel.firstPage() },
                onPrevPage = { viewModel.prevPage() },
                onNextPage = { viewModel.nextPage() },
                onLastPage = { viewModel.lastPage() },
                onJumpToPage = { viewModel.jumpToPage(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // Removed Interactive popup Dialog forms for Add/Edit as they are moved to Activity
    if (showFindReplaceDialog) {
        FindReplaceDialog(
            onDismiss = { showFindReplaceDialog = false },
            onReplaceAll = { find, replace, useRegex ->
                viewModel.findAndReplace(find, replace, useRegex)
                showFindReplaceDialog = false
            }
        )
    }

    if (showBatchImportDialog) {
        BatchImportDialog(
            onDismiss = { showBatchImportDialog = false },
            onImport = { rawText ->
                viewModel.batchImport(rawText)
                showBatchImportDialog = false
            }
        )
    }

    if (showCloseFileWarningDialog) {
        CloseFileWarningDialog(
            onDismiss = { showCloseFileWarningDialog = false },
            onSaveAndClose = {
                showCloseFileWarningDialog = false
                viewModel.saveFile(context)
                viewModel.closeFile()
            },
            onDiscardAndClose = {
                showCloseFileWarningDialog = false
                viewModel.closeFile()
            }
        )
    }

    if (showExitWarningDialog) {
        ExitWarningDialog(
            onDismiss = { showExitWarningDialog = false },
            onSaveAndExit = {
                showExitWarningDialog = false
                viewModel.saveFile(context)
                onExit()
            },
            onDiscardAndExit = {
                showExitWarningDialog = false
                viewModel.resetUnsavedChanges()
                onExit()
            }
        )
    }

    if (showBulkDeleteConfirmDialog) {
        BulkDeleteConfirmDialog(
            selectedCount = selectedCount,
            onDismiss = { showBulkDeleteConfirmDialog = false },
            onConfirmDelete = {
                viewModel.deleteEntries(selectedIds.filterValues { it }.keys.toSet())
                selectedIds.clear()
                showBulkDeleteConfirmDialog = false
            }
        )
    }

    if (showAiErrorDialog) {
        AiErrorDialog(
            isAtpConnected = isAtpConnected,
            connectionError = connectionError,
            onDismiss = { showAiErrorDialog = false }
        )
    }
}
