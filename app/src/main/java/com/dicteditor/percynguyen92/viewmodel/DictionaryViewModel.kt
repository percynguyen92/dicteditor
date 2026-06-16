package com.dicteditor.percynguyen92.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicteditor.percynguyen92.data.DictEntry
import com.dicteditor.percynguyen92.data.DictionaryRepository
import com.dicteditor.percynguyen92.data.EntryOpResult
import com.dicteditor.percynguyen92.data.RecentFilesManager
import com.dicteditor.percynguyen92.data.SearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import androidx.annotation.MainThread
import kotlinx.coroutines.CancellationException

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

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    private val _recentFiles = MutableStateFlow<List<Uri>>(emptyList())
    val recentFiles: StateFlow<List<Uri>> = _recentFiles.asStateFlow()

    // Expose flows directly from repository
    val openedFileUri: StateFlow<Uri?> = repository.openedFileUri
    val hasUnsavedChanges: StateFlow<Boolean> = repository.hasUnsavedChanges
    val canUndo: StateFlow<Boolean> = repository.canUndo
    val canRedo: StateFlow<Boolean> = repository.canRedo
    val highlightedIds: StateFlow<Set<String>> = repository.highlightedIds

    // Filtered list based on search query
    private var filteredEntries: List<DictEntry> = emptyList()

    private val _filteredEntriesCount = MutableStateFlow(0)
    val filteredEntriesCount: StateFlow<Int> = _filteredEntriesCount.asStateFlow()

    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _totalWords = MutableStateFlow(0)
    val totalWords: StateFlow<Int> = _totalWords.asStateFlow()

    // Paginated list shown on UI
    private val _displayEntries = MutableStateFlow<List<DictEntry>>(emptyList())
    val displayEntries: StateFlow<List<DictEntry>> = _displayEntries.asStateFlow()

    val pageSize = 200
    private var searchJob: Job? = null
    private var activeOperationsCount = 0

    init {
        // Collect repository entries and update filtered list reactively
        viewModelScope.launch {
            repository.entriesFlow.collect { entries ->
                applyFilter(entries, maintainPage = true)
            }
        }
    }

    private fun incrementLoading() {
        activeOperationsCount++
        _isLoading.value = true
    }

    private fun decrementLoading() {
        activeOperationsCount--
        if (activeOperationsCount <= 0) {
            activeOperationsCount = 0
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

    private fun updateDisplay() {
        val filtered = filteredEntries
        val page = _currentPage.value
        val start = page * pageSize
        val end = minOf(start + pageSize, filtered.size)

        if (start < filtered.size && start >= 0) {
            _displayEntries.value = filtered.subList(start, end).toList()
        } else {
            _displayEntries.value = emptyList()
        }
    }

    private fun setFilteredList(list: List<DictEntry>, totalSize: Int) {
        filteredEntries = list
        _filteredEntriesCount.value = list.size
        _totalPages.value = if (list.isEmpty()) 0 else (list.size + pageSize - 1) / pageSize
        _totalWords.value = totalSize
    }

    private suspend fun applyFilter(entries: List<DictEntry>, maintainPage: Boolean) {
        val query = _searchQuery.value
        val isRegex = _searchUseRegex.value
        val matchCase = _searchMatchCase.value
        
        val filtered = withContext(Dispatchers.Default) {
            SearchEngine.filterEntries(query, entries, isRegex, matchCase)
        }
        
        withContext(Dispatchers.Main) {
            setFilteredList(filtered, entries.size)
            if (!maintainPage) {
                _currentPage.value = 0
            } else {
                val pages = _totalPages.value
                if (_currentPage.value >= pages && pages > 0) {
                    _currentPage.value = pages - 1
                } else if (_currentPage.value < 0) {
                    _currentPage.value = 0
                }
            }
            updateDisplay()
        }
    }

    fun closeFile() {
        viewModelScope.launch {
            incrementLoading()
            try {
                repository.closeFile()
                _searchQuery.value = ""
                _currentPage.value = 0
                _statusMessage.value = "Đã đóng file"
            } finally {
                decrementLoading()
            }
        }
    }

    fun loadFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            incrementLoading()
            _statusMessage.value = "Đang tải dữ liệu từ file..."
            try {
                val result = repository.loadFile(context, uri)
                if (result.isSuccess) {
                    val count = result.getOrThrow()
                    addRecentFile(context, uri)
                    _statusMessage.value = "Đã tải $count từ"
                    _uiEvents.emit("Đã tải thành công $count từ")
                } else {
                    val exception = result.exceptionOrNull()
                    if (exception !is CancellationException) {
                        _statusMessage.value = "Lỗi khi tải file"
                        _uiEvents.emit("Lỗi khi tải file: ${exception?.message}")
                    }
                }
            } finally {
                decrementLoading()
            }
        }
    }

    fun saveFile(context: Context) {
        viewModelScope.launch {
            incrementLoading()
            _statusMessage.value = "Đang lưu dữ liệu về file..."
            try {
                val result = repository.saveFile(context)
                if (result.isSuccess) {
                    val count = result.getOrThrow()
                    _statusMessage.value = "Đã lưu $count từ"
                    _uiEvents.emit("Đã lưu thành công $count từ")
                } else {
                    val exception = result.exceptionOrNull()
                    _statusMessage.value = "Lỗi khi lưu file"
                    _uiEvents.emit("Không có quyền ghi file hoặc lỗi: ${exception?.message}")
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
                _statusMessage.value = "Tìm thấy ${filteredEntries.size} từ"
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
                    _uiEvents.emit("Đã hoàn tác (Undo)")
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
                    _uiEvents.emit("Đã làm lại (Redo)")
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
                EntryOpResult.Duplicate -> {
                    _uiEvents.emit("Từ '$chinese' đã tồn tại trong từ điển, bỏ qua!")
                    return false
                }
                EntryOpResult.Invalid -> return false
            }
        } finally {
            decrementLoading()
        }
    }

    suspend fun updateEntry(id: String, chinese: String, meanings: List<String>): Boolean {
        incrementLoading()
        try {
            val result = repository.updateEntry(id, chinese, meanings)
            when (result) {
                EntryOpResult.Success -> return true
                EntryOpResult.Duplicate -> {
                    _uiEvents.emit("Từ '$chinese' đã trùng với một từ khác trong từ điển!")
                    return false
                }
                EntryOpResult.Invalid -> return false
            }
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
                _uiEvents.emit("Đã xóa hàng loạt ${ids.size} từ")
            } finally {
                decrementLoading()
            }
        }
    }

    fun sortByDefaultLengthDescending() {
        viewModelScope.launch {
            incrementLoading()
            _statusMessage.value = "Đang sắp xếp từ điển..."
            try {
                repository.sortByDefaultLengthDescending()
                _uiEvents.emit("Đã sắp xếp từ dài tới ngắn (mặc định)")
                _statusMessage.value = "Đã sắp xếp dài -> ngắn"
            } finally {
                decrementLoading()
            }
        }
    }

    fun sortByLengthAscending() {
        viewModelScope.launch {
            incrementLoading()
            _statusMessage.value = "Đang sắp xếp từ điển..."
            try {
                repository.sortByLengthAscending()
                _uiEvents.emit("Đã sắp xếp từ ngắn tới dài")
                _statusMessage.value = "Đã sắp xếp ngắn -> dài"
            } finally {
                decrementLoading()
            }
        }
    }

    fun findAndReplace(findText: String, replaceText: String, useRegex: Boolean) {
        if (findText.isEmpty()) return
        viewModelScope.launch {
            incrementLoading()
            _statusMessage.value = "Đang tìm và thay thế..."
            try {
                val result = repository.findAndReplace(findText, replaceText, useRegex)
                if (result.isSuccess) {
                    val count = result.getOrThrow()
                    _uiEvents.emit("Đã thay thế $count lần")
                    _statusMessage.value = "Thay thế: ${count} lần"
                } else {
                    val exception = result.exceptionOrNull()
                    _uiEvents.emit("Lỗi tìm kiếm Regex: ${exception?.message}")
                    _statusMessage.value = "Lỗi Regex"
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
            _statusMessage.value = "Đang import hàng loạt..."
            try {
                val importResult = repository.batchImport(rawText)
                _uiEvents.emit("Đã import ${importResult.importedNewCount} từ mới, gộp ${importResult.mergedCount} từ trùng và bỏ qua ${importResult.invalidSkipCount} dòng lỗi")
                _statusMessage.value = "Đã import ${importResult.importedNewCount} từ, gộp ${importResult.mergedCount}"
            } catch (e: Exception) {
                e.printStackTrace()
                _uiEvents.emit("Lỗi khi batch import: ${e.message}")
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

    fun nextPage() {
        val pages = totalPages.value
        if (_currentPage.value < pages - 1) {
            _currentPage.value++
            updateDisplay()
        }
    }

    fun prevPage() {
        if (_currentPage.value > 0) {
            _currentPage.value--
            updateDisplay()
        }
    }

    fun firstPage() {
        if (_currentPage.value != 0) {
            _currentPage.value = 0
            updateDisplay()
        }
    }

    fun lastPage() {
        val pages = totalPages.value
        if (pages > 0 && _currentPage.value != pages - 1) {
            _currentPage.value = pages - 1
            updateDisplay()
        }
    }

    fun jumpToPage(pageIndex: Int) {
        val pages = totalPages.value
        if (pages > 0) {
            val target = pageIndex.coerceIn(0, pages - 1)
            if (_currentPage.value != target) {
                _currentPage.value = target
                updateDisplay()
            }
        }
    }
}

