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
import com.dicteditor.percynguyen92.data.DictEntry
import com.dicteditor.percynguyen92.data.DictEntryPagingSource
import com.dicteditor.percynguyen92.data.DictionaryRepository
import com.dicteditor.percynguyen92.data.EntryOpResult
import com.dicteditor.percynguyen92.data.RecentFilesManager
import com.dicteditor.percynguyen92.data.SearchEngine
import com.dicteditor.percynguyen92.utils.UiText
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class DictionaryViewModel : ViewModel() {

    private val repository = DictionaryRepository(maxHistorySize = 10)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchUseRegex = MutableStateFlow(false)
    val searchUseRegex: StateFlow<Boolean> = _searchUseRegex.asStateFlow()

    private val _searchMatchCase = MutableStateFlow(false)
    val searchMatchCase: StateFlow<Boolean> = _searchMatchCase.asStateFlow()

    private val _statusMessage = MutableStateFlow<UiText?>(null)
    val statusMessage: StateFlow<UiText?> = _statusMessage.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiSnackbarEvent>()
    val uiEvents: SharedFlow<UiSnackbarEvent> = _uiEvents.asSharedFlow()

    private val _recentFiles = MutableStateFlow<List<Uri>>(emptyList())
    val recentFiles: StateFlow<List<Uri>> = _recentFiles.asStateFlow()

    private val _fileLoadError = MutableStateFlow<Pair<Uri, String>?>(null)
    val fileLoadError: StateFlow<Pair<Uri, String>?> = _fileLoadError.asStateFlow()

    fun clearFileLoadError() {
        _fileLoadError.value = null
    }

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    // Expose flows directly from repository
    val openedFileUri: StateFlow<Uri?> = repository.openedFileUri
    val hasUnsavedChanges: StateFlow<Boolean> = repository.hasUnsavedChanges
    val canUndo: StateFlow<Boolean> = repository.canUndo
    val canRedo: StateFlow<Boolean> = repository.canRedo
    val highlightedIds: StateFlow<Set<String>> = repository.highlightedIds

    // Filtered list based on search query
    private var filteredEntries: List<DictEntry> = emptyList()

    val filteredEntriesIds: Set<String>
        get() = filteredEntries.map { it.id }.toSet()

    private val _filteredEntriesCount = MutableStateFlow(0)
    val filteredEntriesCount: StateFlow<Int> = _filteredEntriesCount.asStateFlow()

    private val _totalWords = MutableStateFlow(0)
    val totalWords: StateFlow<Int> = _totalWords.asStateFlow()

    private val _filteredEntriesFlow = MutableStateFlow<List<DictEntry>>(emptyList())
    val entryPagingFlow: Flow<PagingData<DictEntry>> = _filteredEntriesFlow
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

    private var searchJob: Job? = null
    private val activeOperationsCount = AtomicInteger(0)

    init {
        // Collect repository entries and update filtered list reactively
        viewModelScope.launch {
            repository.entriesFlow.collect { entries ->
                applyFilter(entries, maintainPage = true)
            }
        }
    }

    private fun incrementLoading() {
        if (activeOperationsCount.incrementAndGet() == 1) {
            _isLoading.value = true
        }
    }

    private fun decrementLoading() {
        val remaining = activeOperationsCount.decrementAndGet()
        if (remaining <= 0) {
            activeOperationsCount.set(0)
            _isLoading.value = false
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

    private fun setFilteredList(list: List<DictEntry>, totalSize: Int) {
        filteredEntries = list
        _filteredEntriesCount.value = list.size
        _totalWords.value = totalSize
        _filteredEntriesFlow.value = list
    }

    private suspend fun applyFilter(entries: List<DictEntry>, maintainPage: Boolean) {
        val query = _searchQuery.value
        val isRegex = _searchUseRegex.value
        val matchCase = _searchMatchCase.value
        
        val result = withContext(Dispatchers.Default) {
            SearchEngine.filterEntries(query, entries, isRegex, matchCase)
        }
        
        withContext(Dispatchers.Main) {
            val filtered = result.getOrElse { exception ->
                _searchError.value = exception.message
                setFilteredList(emptyList(), entries.size)
                return@withContext
            }
            
            _searchError.value = null
            setFilteredList(filtered, entries.size)
        }
    }

    fun closeFile() {
        viewModelScope.launch {
            incrementLoading()
            try {
                repository.closeFile()
                _searchQuery.value = ""
                _statusMessage.value = UiText.StringResource(R.string.vm_status_closed)
            } finally {
                decrementLoading()
            }
        }
    }

    fun loadFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            incrementLoading()
            _statusMessage.value = UiText.StringResource(R.string.vm_status_loading)
            try {
                val result = repository.loadFile(context, uri)
                if (result.isSuccess) {
                    val count = result.getOrThrow()
                    addRecentFile(context, uri)
                    _statusMessage.value = UiText.StringResource(R.string.vm_status_loaded, listOf(count))
                    _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_loaded, listOf(count)), SnackbarType.SUCCESS))
                } else {
                    val exception = result.exceptionOrNull()
                    _statusMessage.value = UiText.StringResource(R.string.vm_status_load_error)
                    val errorMsg = exception?.message ?: context.getString(R.string.vm_error_unknown)
                    _fileLoadError.value = Pair(uri, errorMsg)
                    removeRecentFile(context, uri)
                }
            } finally {
                decrementLoading()
            }
        }
    }

    fun saveFile(context: Context) {
        viewModelScope.launch {
            incrementLoading()
            _statusMessage.value = UiText.StringResource(R.string.vm_status_saving)
            try {
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
            } finally {
                decrementLoading()
            }
        }
    }

    fun setSearchUseRegex(useRegex: Boolean) {
        _searchUseRegex.value = useRegex
        setSearchQuery(_searchQuery.value)
    }

    fun setSearchMatchCase(matchCase: Boolean) {
        _searchMatchCase.value = matchCase
        setSearchQuery(_searchQuery.value)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            incrementLoading()
            try {
                applyFilter(repository.entriesFlow.value, maintainPage = false)
                _statusMessage.value = UiText.StringResource(R.string.vm_status_found, listOf(filteredEntries.size))
            } finally {
                decrementLoading()
            }
        }
    }

    fun undo() {
        viewModelScope.launch {
            incrementLoading()
            try {
                val ok = repository.undo()
                if (ok) {
                    _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_undo), SnackbarType.INFO))
                }
            } finally {
                decrementLoading()
            }
        }
    }

    fun redo() {
        viewModelScope.launch {
            incrementLoading()
            try {
                val ok = repository.redo()
                if (ok) {
                    _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_redo), SnackbarType.INFO))
                }
            } finally {
                decrementLoading()
            }
        }
    }

    suspend fun addEntry(chinese: String, meanings: List<String>): Boolean {
        incrementLoading()
        try {
            val result = repository.addEntry(chinese, meanings)
            when (result) {
                EntryOpResult.Success -> return true
                EntryOpResult.Merged -> return false
                EntryOpResult.Duplicate -> {
                    _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_duplicate, listOf(chinese)), SnackbarType.ERROR))
                    return false
                }
                EntryOpResult.Invalid -> return false
            }
        } finally {
            decrementLoading()
        }
    }

    suspend fun updateEntry(id: String, chinese: String, meanings: List<String>): EntryOpResult {
        incrementLoading()
        try {
            return repository.updateEntry(id, chinese, meanings)
        } finally {
            decrementLoading()
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            incrementLoading()
            try {
                repository.deleteEntry(id)
            } finally {
                decrementLoading()
            }
        }
    }

    fun deleteEntries(ids: Set<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            incrementLoading()
            try {
                repository.deleteEntries(ids)
                _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_deleted, listOf(ids.size)), SnackbarType.SUCCESS))
            } finally {
                decrementLoading()
            }
        }
    }

    fun sortByDefaultLengthDescending() {
        viewModelScope.launch {
            incrementLoading()
            _statusMessage.value = UiText.StringResource(R.string.vm_status_sorting)
            try {
                repository.sortByDefaultLengthDescending()
                _statusMessage.value = UiText.StringResource(R.string.vm_status_sorted_desc)
            } finally {
                decrementLoading()
            }
        }
    }

    fun sortByLengthAscending() {
        viewModelScope.launch {
            incrementLoading()
            _statusMessage.value = UiText.StringResource(R.string.vm_status_sorting)
            try {
                repository.sortByLengthAscending()
                _statusMessage.value = UiText.StringResource(R.string.vm_status_sorted_asc)
            } finally {
                decrementLoading()
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
            incrementLoading()
            _statusMessage.value = UiText.StringResource(R.string.vm_status_replacing)
            try {
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
            } finally {
                decrementLoading()
            }
        }
    }

    fun batchImport(rawText: String) {
        if (rawText.isBlank()) return
        viewModelScope.launch {
            incrementLoading()
            _statusMessage.value = UiText.StringResource(R.string.vm_status_importing)
            try {
                val importResult = repository.batchImport(rawText)
                _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_imported, listOf(importResult.importedNewCount, importResult.mergedCount, importResult.invalidSkipCount)), SnackbarType.SUCCESS))
                _statusMessage.value = UiText.StringResource(R.string.vm_status_imported, listOf(importResult.importedNewCount, importResult.mergedCount))
            } catch (e: Exception) {
                e.printStackTrace()
                _uiEvents.emit(UiSnackbarEvent(UiText.StringResource(R.string.vm_snackbar_import_error, listOf(e.message ?: "")), SnackbarType.ERROR))
            } finally {
                decrementLoading()
            }
        }
    }

    fun getExportStringForSelected(selectedIds: Set<String>): String {
        return runBlocking {
            repository.getExportStringForSelected(selectedIds)
        }
    }

    fun resetUnsavedChanges() {
        repository.resetUnsavedChanges()
    }

}

enum class SnackbarType {
    SUCCESS, ERROR, INFO
}

data class UiSnackbarEvent(
    val message: UiText,
    val type: SnackbarType = SnackbarType.INFO
)
