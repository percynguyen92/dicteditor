package com.dicteditor.percynguyen92.viewmodel

import android.content.Context
import java.util.concurrent.atomic.AtomicInteger
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.dicteditor.percynguyen92.R
import com.dicteditor.percynguyen92.data.model.DictEntry
import com.dicteditor.percynguyen92.data.local.DictEntryPagingSource
import com.dicteditor.percynguyen92.data.repository.dictionary.DictionaryRepository
import com.dicteditor.percynguyen92.data.repository.dictionary.EntryOpResult
import com.dicteditor.percynguyen92.data.repository.dictionary.ImportMergeMode
import com.dicteditor.percynguyen92.data.local.RecentFilesManager
import com.dicteditor.percynguyen92.utils.SearchEngine
import com.dicteditor.percynguyen92.utils.UiText
import com.dicteditor.percynguyen92.utils.UpdateInfo
import com.dicteditor.percynguyen92.utils.GithubUpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class DictionaryViewModel : ViewModel() {

    private val repository = DictionaryRepository(maxHistorySize = 10)

    private val loadingManager = LoadingStateManager()
    val isLoading: StateFlow<Boolean> = loadingManager.isLoading

    private val searchCoordinator = SearchCoordinator(
        scope = viewModelScope,
        entriesSource = repository.entriesFlow,
        onLoadingChange = { loading ->
            if (loading) loadingManager.increment() else loadingManager.decrement()
        }
    )

    val searchQuery: StateFlow<String> = searchCoordinator.query
    val searchUseRegex: StateFlow<Boolean> = searchCoordinator.useRegex
    val searchMatchCase: StateFlow<Boolean> = searchCoordinator.matchCase
    val searchError: StateFlow<String?> = searchCoordinator.error
    val filteredEntriesCount: StateFlow<Int> = searchCoordinator.filteredCount

    private val _statusMessage = MutableStateFlow<UiText?>(null)
    val statusMessage: StateFlow<UiText?> = _statusMessage.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiSnackbarEvent>()
    val uiEvents: SharedFlow<UiSnackbarEvent> = _uiEvents.asSharedFlow()

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _recentFiles = MutableStateFlow<List<Uri>>(emptyList())
    val recentFiles: StateFlow<List<Uri>> = _recentFiles.asStateFlow()

    private val _fileLoadError = MutableStateFlow<Pair<Uri, String>?>(null)
    val fileLoadError: StateFlow<Pair<Uri, String>?> = _fileLoadError.asStateFlow()

    fun clearFileLoadError() {
        _fileLoadError.value = null
    }

    // Expose flows directly from repository
    val openedFileUri: StateFlow<Uri?> = repository.openedFileUri
    val hasUnsavedChanges: StateFlow<Boolean> = repository.hasUnsavedChanges
    val canUndo: StateFlow<Boolean> = repository.canUndo
    val canRedo: StateFlow<Boolean> = repository.canRedo
    val highlightedIds: StateFlow<Set<String>> = repository.highlightedIds

    val filteredEntriesIds: StateFlow<Set<String>> = searchCoordinator.filtered
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptySet()
        )

    val totalWords: StateFlow<Int> = repository.entriesFlow
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0
        )

    val entryPagingFlow: Flow<PagingData<DictEntry>> = searchCoordinator.filtered
        .flatMapLatest { entries ->
            Pager(
                config = PagingConfig(
                    pageSize = 200,
                    enablePlaceholders = false,
                    initialLoadSize = 200
                ),
                pagingSourceFactory = { DictEntryPagingSource(entries) }
            ).flow
        }
        .cachedIn(viewModelScope)


    init {
        // Collect repository entries and update filtered list reactively
        viewModelScope.launch {
            repository.entriesFlow.collect { entries ->
                searchCoordinator.onEntriesChanged(entries)
            }
        }

        // Update status message when search results change
        viewModelScope.launch {
            searchCoordinator.filteredCount.collect { count ->
                if (searchCoordinator.query.value.isNotEmpty()) {
                    _statusMessage.value = UiText.StringResource(R.string.vm_status_found, listOf(count))
                }
            }
        }
    }



    fun loadRecentFiles(context: Context) {
        val recents = RecentFilesManager.getRecentFiles(context)
        if (recents.isNotEmpty()) {
            _recentFiles.value = recents
        } else {
            val prefs = context.getSharedPreferences("dict_prefs", Context.MODE_PRIVATE)
            val lastUriString = prefs.getString("last_file_uri", null)
            if (lastUriString != null) {
                try {
                    val lastUri = Uri.parse(lastUriString)
                    _recentFiles.value = RecentFilesManager.addRecentFile(context, lastUri)
                } catch (e: Exception) {}
            }
        }
    }

    private fun addRecentFile(context: Context, uri: Uri) {
        _recentFiles.value = RecentFilesManager.addRecentFile(context, uri)
    }

    fun removeRecentFile(context: Context, uri: Uri) {
        _recentFiles.value = RecentFilesManager.removeRecentFile(context, uri)
    }

    fun closeFile() {
        viewModelScope.launch {
            loadingManager.tracked {
                repository.closeFile()
                setSearchQuery("")
                _statusMessage.value = UiText.StringResource(R.string.vm_status_closed)
            }
        }
    }

    fun loadFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            loadingManager.tracked {
                _statusMessage.value = UiText.StringResource(R.string.vm_status_loading)
                val result = repository.loadFile(context, uri)
                if (result.isSuccess) {
                    val count = result.getOrThrow()
                    addRecentFile(context, uri)
                    _statusMessage.value = UiText.StringResource(R.string.vm_status_loaded, listOf(count))
                } else {
                    val exception = result.exceptionOrNull()
                    _statusMessage.value = UiText.StringResource(R.string.vm_status_load_error)
                    val errorMsg = exception?.message ?: UiText.StringResource(R.string.vm_error_unknown).asString(context)
                    _fileLoadError.value = Pair(uri, errorMsg)
                    removeRecentFile(context, uri)
                }
            }
        }
    }

    fun saveFile(context: Context) {
        viewModelScope.launch {
            loadingManager.tracked {
                _statusMessage.value = UiText.StringResource(R.string.vm_status_saving)
                val result = repository.saveFile(context)
                if (result.isSuccess) {
                    val count = result.getOrThrow()
                    _statusMessage.value = UiText.StringResource(R.string.vm_status_saved, listOf(count))
                    _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_saved, listOf(count)), SnackbarType.SUCCESS))
                } else {
                    val exception = result.exceptionOrNull()
                    _statusMessage.value = UiText.StringResource(R.string.vm_status_save_error)
                    _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_save_error, listOf(exception?.message ?: "")), SnackbarType.ERROR))
                }
            }
        }
    }

    fun setSearchUseRegex(useRegex: Boolean) {
        searchCoordinator.setUseRegex(useRegex)
    }

    fun setSearchMatchCase(matchCase: Boolean) {
        searchCoordinator.setMatchCase(matchCase)
    }

    fun setSearchQuery(query: String) {
        searchCoordinator.setQuery(query)
    }

    fun undo() {
        viewModelScope.launch {
            loadingManager.tracked {
                val ok = repository.undo()
                if (ok) {
                    _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_undo), SnackbarType.INFO))
                }
            }
        }
    }

    fun redo() {
        viewModelScope.launch {
            loadingManager.tracked {
                val ok = repository.redo()
                if (ok) {
                    _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_redo), SnackbarType.INFO))
                }
            }
        }
    }

    suspend fun addEntry(chinese: String, meanings: List<String>): Boolean {
        return loadingManager.tracked {
            val result = repository.addEntry(chinese, meanings)
            when (result) {
                EntryOpResult.Success -> true
                EntryOpResult.Merged -> false
                EntryOpResult.Duplicate -> {
                    _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_duplicate, listOf(chinese)), SnackbarType.ERROR))
                    false
                }
                EntryOpResult.Invalid -> false
            }
        }
    }

    suspend fun updateEntry(id: String, chinese: String, meanings: List<String>): EntryOpResult {
        return loadingManager.tracked {
            repository.updateEntry(id, chinese, meanings)
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            loadingManager.tracked {
                repository.deleteEntry(id)
            }
        }
    }

    fun deleteEntries(ids: Set<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            loadingManager.tracked {
                repository.deleteEntries(ids)
                _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_deleted, listOf(ids.size)), SnackbarType.SUCCESS))
            }
        }
    }

    fun sortByDefaultLengthDescending() {
        viewModelScope.launch {
            loadingManager.tracked {
                _statusMessage.value = UiText.StringResource(R.string.vm_status_sorting)
                repository.sortByDefaultLengthDescending()
                _statusMessage.value = UiText.StringResource(R.string.vm_status_sorted_desc)
            }
        }
    }

    fun sortByLengthAscending() {
        viewModelScope.launch {
            loadingManager.tracked {
                _statusMessage.value = UiText.StringResource(R.string.vm_status_sorting)
                repository.sortByLengthAscending()
                _statusMessage.value = UiText.StringResource(R.string.vm_status_sorted_asc)
            }
        }
    }

    fun findAndReplace(
        findText: String,
        replaceText: String,
        useRegex: Boolean,
        matchCase: Boolean,
        scopeIds: Set<String>? = null
    ) {
        if (findText.isEmpty()) return
        viewModelScope.launch {
            loadingManager.tracked {
                _statusMessage.value = UiText.StringResource(R.string.vm_status_replacing)
                val result = repository.findAndReplace(findText, replaceText, useRegex, matchCase, scopeIds)
                if (result.isSuccess) {
                    val count = result.getOrThrow()
                    _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_replaced, listOf(count)), SnackbarType.SUCCESS))
                    _statusMessage.value = UiText.StringResource(R.string.vm_status_replaced, listOf(count))
                } else {
                    val exception = result.exceptionOrNull()
                    _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_regex_error, listOf(exception?.message ?: "")), SnackbarType.ERROR))
                    _statusMessage.value = UiText.StringResource(R.string.vm_status_regex_error)
                }
            }
        }
    }

    fun batchImport(rawText: String, mergeMode: ImportMergeMode = ImportMergeMode.INSERT) {
        if (rawText.isBlank()) return
        viewModelScope.launch {
            loadingManager.tracked {
                _statusMessage.value = UiText.StringResource(R.string.vm_status_importing)
                try {
                    val importResult = repository.batchImport(rawText, mergeMode)
                    _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_imported, listOf(importResult.importedNewCount, importResult.mergedCount, importResult.invalidSkipCount)), SnackbarType.SUCCESS))
                    _statusMessage.value = UiText.StringResource(R.string.vm_status_imported, listOf(importResult.importedNewCount, importResult.mergedCount))
                } catch (e: Exception) {
                    e.printStackTrace()
                    _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_import_error, listOf(e.message ?: "")), SnackbarType.ERROR))
                }
            }
        }
    }

    fun batchImportEntries(entries: List<DictEntry>, mergeMode: ImportMergeMode = ImportMergeMode.INSERT) {
        if (entries.isEmpty()) return
        viewModelScope.launch {
            loadingManager.tracked {
                _statusMessage.value = UiText.StringResource(R.string.vm_status_importing)
                try {
                    val importResult = repository.batchImportEntries(entries, mergeMode)
                    _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_imported, listOf(importResult.importedNewCount, importResult.mergedCount, importResult.invalidSkipCount)), SnackbarType.SUCCESS))
                    _statusMessage.value = UiText.StringResource(R.string.vm_status_imported, listOf(importResult.importedNewCount, importResult.mergedCount))
                } catch (e: Exception) {
                    e.printStackTrace()
                    _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_import_error, listOf(e.message ?: "")), SnackbarType.ERROR))
                }
            }
        }
    }

    suspend fun getExportStringForSelected(selectedIds: Set<String>): String =
        repository.getExportStringForSelected(selectedIds)

    fun resetUnsavedChanges() {
        repository.resetUnsavedChanges()
    }

    fun checkForUpdates(currentVersion: String) {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                GithubUpdateChecker.checkForUpdate(currentVersion)
            }
            if (info != null) {
                _updateInfo.value = info
            }
        }
    }

    fun triggerManualUpdateCheck(currentVersion: String, onFinished: (UpdateInfo?) -> Unit) {
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                GithubUpdateChecker.checkForUpdate(currentVersion)
            }
            if (info != null) {
                _updateInfo.value = info
            }
            onFinished(info)
        }
    }

    fun dismissUpdateDialog() {
        _updateInfo.value = null
    }

}

enum class SnackbarType {
    SUCCESS, ERROR, INFO
}

data class UiSnackbarEvent(
    val message: UiText,
    val type: SnackbarType = SnackbarType.INFO
)
