package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DictEntry
import com.example.data.FileHandler
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.annotation.MainThread

class DictionaryViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchUseRegex = MutableStateFlow(false)
    val searchUseRegex: StateFlow<Boolean> = _searchUseRegex.asStateFlow()

    private val _searchMatchCase = MutableStateFlow(false)
    val searchMatchCase: StateFlow<Boolean> = _searchMatchCase.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _highlightedIds = MutableStateFlow<Set<String>>(emptySet())
    val highlightedIds: StateFlow<Set<String>> = _highlightedIds.asStateFlow()

    private val _openedFileUri = MutableStateFlow<Uri?>(null)
    val openedFileUri: StateFlow<Uri?> = _openedFileUri.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Event channel: for toasts and dialog messages to display to the user
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    // Core list in memory
    private var allEntries = ArrayList<DictEntry>()
    private val mutex = Mutex()

    // Undo/Redo history stacks
    private val undoStack = ArrayList<ArrayList<DictEntry>>()
    private val redoStack = ArrayList<ArrayList<DictEntry>>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private fun saveToHistory() {
        undoStack.add(ArrayList(allEntries))
        if (undoStack.size > 10) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun undo() {
        viewModelScope.launch {
            if (undoStack.isEmpty()) return@launch
            mutex.withLock {
                val currentState = ArrayList(allEntries)
                redoStack.add(currentState)
                
                val prevState = undoStack.removeAt(undoStack.lastIndex)
                allEntries = ArrayList(prevState)
                _hasUnsavedChanges.value = true
                applyFilterSynchronously(maintainPage = true)
                
                _canUndo.value = undoStack.isNotEmpty()
                _canRedo.value = redoStack.isNotEmpty()
            }
            _uiEvents.emit("Đã hoàn tác (Undo)")
        }
    }

    fun redo() {
        viewModelScope.launch {
            if (redoStack.isEmpty()) return@launch
            mutex.withLock {
                val currentState = ArrayList(allEntries)
                undoStack.add(currentState)
                
                val nextState = redoStack.removeAt(redoStack.lastIndex)
                allEntries = ArrayList(nextState)
                _hasUnsavedChanges.value = true
                applyFilterSynchronously(maintainPage = true)
                
                _canUndo.value = undoStack.isNotEmpty()
                _canRedo.value = redoStack.isNotEmpty()
            }
            _uiEvents.emit("Đã làm lại (Redo)")
        }
    }
    // Filtered list based on search query
    private var filteredEntries: List<DictEntry> = emptyList()

    private val _filteredEntriesCount = MutableStateFlow(0)
    val filteredEntriesCount: StateFlow<Int> = _filteredEntriesCount.asStateFlow()

    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _totalWords = MutableStateFlow(0)
    val totalWords: StateFlow<Int> = _totalWords.asStateFlow()

    private fun setFilteredList(list: List<DictEntry>) {
        filteredEntries = list
        _filteredEntriesCount.value = list.size
        _totalPages.value = if (list.isEmpty()) 0 else (list.size + pageSize - 1) / pageSize
        _totalWords.value = allEntries.size
    }

    // Paginated list shown on UI
    private val _displayEntries = MutableStateFlow<List<DictEntry>>(emptyList())
    val displayEntries: StateFlow<List<DictEntry>> = _displayEntries.asStateFlow()

    val pageSize = 200
    private var searchJob: Job? = null
    private var lastLoadedIdCounter = 0

    private val _recentFiles = MutableStateFlow<List<Uri>>(emptyList())
    val recentFiles: StateFlow<List<Uri>> = _recentFiles.asStateFlow()

    fun loadRecentFiles(context: Context) {
        val prefs = context.getSharedPreferences("dict_prefs", Context.MODE_PRIVATE)
        val recentsStr = prefs.getString("recent_files", "")
        if (!recentsStr.isNullOrEmpty()) {
            val uris = recentsStr.split(",").mapNotNull {
                try { Uri.parse(it) } catch (e: Exception) { null }
            }
            _recentFiles.value = uris
        } else {
            // Check for previous singleton last file uri
            val lastUriString = prefs.getString("last_file_uri", null)
            if (lastUriString != null) {
                try {
                    val lastUri = Uri.parse(lastUriString)
                    _recentFiles.value = listOf(lastUri)
                    addRecentFile(context, lastUri)
                } catch (e: Exception) {}
            }
        }
    }

    private fun addRecentFile(context: Context, uri: Uri) {
        val current = _recentFiles.value.toMutableList()
        current.remove(uri)
        current.add(0, uri)
        if (current.size > 10) {
            current.removeLast()
        }
        _recentFiles.value = current
        val prefs = context.getSharedPreferences("dict_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("recent_files", current.joinToString(",") { it.toString() }).apply()
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

    fun closeFile() {
        _openedFileUri.value = null
        allEntries = ArrayList()
        _filteredEntriesCount.value = 0
        _totalWords.value = 0
        setFilteredList(emptyList())
        _hasUnsavedChanges.value = false
        _searchQuery.value = ""
        _currentPage.value = 0
        _highlightedIds.value = emptySet()
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        updateDisplay()
        _statusMessage.value = "Đã đóng file"
    }

    fun loadFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Đang tải dữ liệu từ file..."
            try {
                val loaded = FileHandler.readLines(context, uri)
                // PM Feedback: Mở file không tự động sort
                allEntries = ArrayList(loaded)
                lastLoadedIdCounter = allEntries.size + 1
                undoStack.clear()
                redoStack.clear()
                _canUndo.value = false
                _canRedo.value = false
                _openedFileUri.value = uri
                _hasUnsavedChanges.value = false
                _searchQuery.value = ""
                _currentPage.value = 0
                setFilteredList(allEntries)
                updateDisplay()
                addRecentFile(context, uri)
                _statusMessage.value = "Đã tải ${allEntries.size} từ"
                _uiEvents.emit("Đã tải thành công ${allEntries.size} từ")
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Lỗi khi tải file"
                _uiEvents.emit("Lỗi khi tải file: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveFile(context: Context) {
        val uri = _openedFileUri.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Đang lưu dữ liệu về file..."
            try {
                val savedCount = FileHandler.writeLines(context, uri, allEntries)
                _hasUnsavedChanges.value = false
                _statusMessage.value = "Đã lưu $savedCount từ"
                _uiEvents.emit("Đã lưu thành công $savedCount từ")
            } catch (e: Exception) {
                e.printStackTrace()
                _statusMessage.value = "Lỗi khi lưu file"
                _uiEvents.emit("Không có quyền ghi file hoặc lỗi: ${e.message}")
            } finally {
                _isLoading.value = false
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
        // Cancel the previous active searching process to keep UI snappy
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            // Debounce for 300ms (to ensure typing is smooth but responsive)
            delay(300)
            _isLoading.value = true
            try {
                val filtered = withContext(Dispatchers.Default) {
                    mutex.withLock {
                        if (query.isEmpty()) {
                            ArrayList(allEntries)
                        } else {
                            val q = query.trim()
                            val isRegex = _searchUseRegex.value
                            val matchCase = _searchMatchCase.value
                            val ignoreCase = !matchCase

                            val regex = if (isRegex) {
                                try {
                                    val options = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
                                    Regex(q, options)
                                } catch (e: Exception) {
                                    null
                                }
                            } else null

                            allEntries.filter { entry ->
                                if (isRegex && regex != null) {
                                    regex.containsMatchIn(entry.chinese) ||
                                            entry.meanings.any { m -> regex.containsMatchIn(m) }
                                } else {
                                    entry.chinese.contains(q, ignoreCase = ignoreCase) ||
                                            entry.meanings.any { m -> m.contains(q, ignoreCase = ignoreCase) }
                                }
                            }
                        }
                    }
                }
                setFilteredList(filtered)
                _currentPage.value = 0
                updateDisplay()
                _statusMessage.value = "Tìm thấy ${filtered.size} từ"
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun nextPage() {
        val pages = (totalPages.value)
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

    @MainThread
    private fun applyFilterSynchronously(maintainPage: Boolean = true) {
        val query = _searchQuery.value
        val filtered = if (query.isEmpty()) {
            allEntries
        } else {
            val q = query.trim()
            val isRegex = _searchUseRegex.value
            val matchCase = _searchMatchCase.value
            val ignoreCase = !matchCase

            val regex = if (isRegex) {
                try {
                    val options = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
                    Regex(q, options)
                } catch (e: Exception) {
                    null
                }
            } else null

            allEntries.filter { entry ->
                if (isRegex && regex != null) {
                    regex.containsMatchIn(entry.chinese) ||
                            entry.meanings.any { m -> regex.containsMatchIn(m) }
                } else {
                    entry.chinese.contains(q, ignoreCase = ignoreCase) ||
                            entry.meanings.any { m -> m.contains(q, ignoreCase = ignoreCase) }
                }
            }
        }
        setFilteredList(filtered)
        if (!maintainPage) {
            _currentPage.value = 0
        } else {
            val pages = totalPages.value
            if (_currentPage.value >= pages && pages > 0) {
                _currentPage.value = pages - 1
            } else if (_currentPage.value < 0) {
                _currentPage.value = 0
            }
        }
        updateDisplay()
    }

    suspend fun addEntry(chinese: String, meanings: List<String>): Boolean {
        val cleanChinese = chinese.trim()
        if (cleanChinese.isEmpty()) return false

        return mutex.withLock {
            // Check duplicate within the entire dictionary to respect rules
            val exists = allEntries.any { it.chinese.equals(cleanChinese, ignoreCase = true) }
            if (exists) {
                // emit on viewmodelscope
                _uiEvents.emit("Từ '$cleanChinese' đã tồn tại trong từ điển, bỏ qua!")
                return@withLock false
            }

            saveToHistory()
            lastLoadedIdCounter++
            val entry = DictEntry("entry_$lastLoadedIdCounter", cleanChinese, meanings.map { it.trim() }.filter { it.isNotEmpty() })
            allEntries.add(0, entry)
            _hasUnsavedChanges.value = true
            _highlightedIds.value = setOf(entry.id)
            _currentPage.value = 0

            // Refresh filter and list display
            withContext(Dispatchers.Main) {
                applyFilterSynchronously(maintainPage = false)
            }
            true
        }
    }

    suspend fun updateEntry(id: String, chinese: String, meanings: List<String>): Boolean {
        val cleanChinese = chinese.trim()
        if (cleanChinese.isEmpty()) return false

        return mutex.withLock {
            // Check if chinese is updated to duplicate of another existing chinese key
            val entryIndex = allEntries.indexOfFirst { it.id == id }
            if (entryIndex == -1) return@withLock false

            val exists = allEntries.indices.any { i ->
                i != entryIndex && allEntries[i].chinese.equals(cleanChinese, ignoreCase = true)
            }
            if (exists) {
                _uiEvents.emit("Từ '$cleanChinese' đã trùng với một từ khác trong từ điển!")
                return@withLock false
            }

            saveToHistory()
            val updated = DictEntry(id, cleanChinese, meanings.map { it.trim() }.filter { it.isNotEmpty() })
            allEntries[entryIndex] = updated
            _hasUnsavedChanges.value = true

            // Re-execute filter and update display
            withContext(Dispatchers.Main) {
                applyFilterSynchronously(maintainPage = true)
            }
            true
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            mutex.withLock {
                val entryIndex = allEntries.indexOfFirst { it.id == id }
                if (entryIndex == -1) return@withLock

                saveToHistory()
                allEntries.removeAt(entryIndex)
                _hasUnsavedChanges.value = true

                // Maintain page index safety if deleting shifts elements
                withContext(Dispatchers.Main) {
                    applyFilterSynchronously(maintainPage = true)
                }
            }
        }
    }

    fun exportSelectedEntries(context: Context, uri: Uri, selectedIds: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val entriesToExport = mutex.withLock {
                    allEntries.filter { selectedIds.contains(it.id) }
                }
                
                context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                    stream.bufferedWriter().use { writer ->
                        for (entry in entriesToExport) {
                            val chinese = entry.chinese
                            val meanings = entry.meanings.joinToString("/")
                            writer.write("$chinese=$meanings\n")
                        }
                    }
                }
                _uiEvents.emit("Xuất file thành công!")
            } catch (e: Exception) {
                e.printStackTrace()
                _uiEvents.emit("Lỗi xuất file: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getExportStringForSelected(selectedIds: Set<String>): String {
        val entriesToExport = allEntries.filter { selectedIds.contains(it.id) }
        val sb = java.lang.StringBuilder()
        for (entry in entriesToExport) {
            val chinese = entry.chinese
            val meanings = entry.meanings.joinToString("/")
            sb.append("$chinese=$meanings\n")
        }
        return sb.toString()
    }

    fun deleteEntries(ids: Set<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            mutex.withLock {
                saveToHistory()
                allEntries.removeAll { it.id in ids }
                _hasUnsavedChanges.value = true
                withContext(Dispatchers.Main) {
                    applyFilterSynchronously(maintainPage = true)
                }
            }
            _uiEvents.emit("Đã xóa hàng loạt ${ids.size} từ")
        }
    }

    fun sortByDefaultLengthDescending() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Đang sắp xếp từ điển..."
            try {
                mutex.withLock {
                    saveToHistory()
                    withContext(Dispatchers.Default) {
                        allEntries.sortByDescending { it.chinese.length }
                    }
                    _hasUnsavedChanges.value = true
                    withContext(Dispatchers.Main) {
                        applyFilterSynchronously(maintainPage = false)
                    }
                }
                _uiEvents.emit("Đã sắp xếp từ dài tới ngắn (mặc định)")
                _statusMessage.value = "Đã sắp xếp dài -> ngắn"
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sortByLengthAscending() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Đang sắp xếp từ điển..."
            try {
                mutex.withLock {
                    saveToHistory()
                    withContext(Dispatchers.Default) {
                        allEntries.sortBy { it.chinese.length }
                    }
                    _hasUnsavedChanges.value = true
                    withContext(Dispatchers.Main) {
                        applyFilterSynchronously(maintainPage = false)
                    }
                }
                _uiEvents.emit("Đã sắp xếp từ ngắn tới dài")
                _statusMessage.value = "Đã sắp xếp ngắn -> dài"
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun findAndReplace(findText: String, replaceText: String, useRegex: Boolean) {
        if (findText.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Đang tìm và thay thế..."
            try {
                var totalReplacements = 0
                val regex = if (useRegex) Regex(findText) else null
                
                mutex.withLock {
                    val updated = withContext(Dispatchers.Default) {
                        allEntries.map { entry ->
                            var lineChanged = false
                            var newChinese = entry.chinese
                            if (useRegex) {
                                if (regex!!.containsMatchIn(newChinese)) {
                                    val matches = regex.findAll(newChinese).toList()
                                    if (matches.isNotEmpty()) {
                                        totalReplacements += matches.size
                                        lineChanged = true
                                        newChinese = newChinese.replace(regex, replaceText)
                                    }
                                }
                            } else {
                                if (newChinese.contains(findText)) {
                                    val count = newChinese.split(findText).size - 1
                                    if (count > 0) {
                                        totalReplacements += count
                                        lineChanged = true
                                        newChinese = newChinese.replace(findText, replaceText)
                                    }
                                }
                            }

                            val newMeanings = entry.meanings.map { meaning ->
                                if (useRegex) {
                                    if (regex!!.containsMatchIn(meaning)) {
                                        val matches = regex.findAll(meaning).toList()
                                        if (matches.isNotEmpty()) {
                                            totalReplacements += matches.size
                                            lineChanged = true
                                            meaning.replace(regex, replaceText)
                                        } else meaning
                                    } else meaning
                                } else {
                                    if (meaning.contains(findText)) {
                                        val count = meaning.split(findText).size - 1
                                        if (count > 0) {
                                            totalReplacements += count
                                            lineChanged = true
                                            meaning.replace(findText, replaceText)
                                        } else meaning
                                    } else meaning
                                }
                            }
                            
                            if (lineChanged) {
                                entry.copy(chinese = newChinese, meanings = newMeanings)
                            } else {
                                entry
                            }
                        }
                    } // end withContext
                    
                    if (totalReplacements > 0) {
                        saveToHistory()
                        allEntries = ArrayList(updated)
                        _hasUnsavedChanges.value = true
                        withContext(Dispatchers.Main) {
                            applyFilterSynchronously(maintainPage = true)
                        }
                    }
                } // end mutex
                
                _uiEvents.emit("Đã thay thế $totalReplacements lần")
                _statusMessage.value = "Thay thế: ${totalReplacements} lần"
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun batchImport(rawText: String) {
        if (rawText.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Đang import hàng loạt..."
            try {
                val lines = rawText.lines()
                var importedNewCount = 0
                var mergedCount = 0
                var invalidSkipCount = 0

                mutex.withLock {
                    val (updatedList, newIdCounter, modifiedIds) = withContext(Dispatchers.Default) {
                        val keyToIndex = allEntries.mapIndexed { index, entry -> entry.chinese.lowercase().trim() to index }.toMap().toMutableMap()
                        val updatedAllEntries = ArrayList(allEntries)
                        var idCounterTemp = lastLoadedIdCounter
                        val modifiedIdsSet = mutableSetOf<String>()

                        lines.forEach { line ->
                            if (line.trim().isEmpty()) return@forEach
                            val entryTemp = DictEntry.parse(line, "")
                            if (entryTemp == null) {
                                invalidSkipCount++
                                return@forEach
                            }
                            val keyLower = entryTemp.chinese.lowercase().trim()
                            val existingIndex = keyToIndex[keyLower]
                            if (existingIndex != null) {
                                val oldEntry = updatedAllEntries[existingIndex]
                                val mergedMeanings = (entryTemp.meanings + oldEntry.meanings).distinct()
                                val updatedEntry = oldEntry.copy(meanings = mergedMeanings)
                                updatedAllEntries[existingIndex] = updatedEntry
                                mergedCount++
                                modifiedIdsSet.add(updatedEntry.id)
                            } else {
                                idCounterTemp++
                                val finalEntry = entryTemp.copy(id = "entry_$idCounterTemp")
                                updatedAllEntries.add(finalEntry)
                                keyToIndex[keyLower] = updatedAllEntries.lastIndex
                                importedNewCount++
                                modifiedIdsSet.add(finalEntry.id)
                            }
                        }
                        
                        val newAllEntries = ArrayList<DictEntry>()
                        val (modified, untouched) = updatedAllEntries.partition { modifiedIdsSet.contains(it.id) }
                        newAllEntries.addAll(modified)
                        newAllEntries.addAll(untouched)

                        Triple(newAllEntries, idCounterTemp, modifiedIdsSet)
                    }

                    lastLoadedIdCounter = newIdCounter
                    saveToHistory()
                    allEntries = updatedList
                    _hasUnsavedChanges.value = true
                    _highlightedIds.value = modifiedIds
                    _currentPage.value = 0

                    withContext(Dispatchers.Main) {
                        applyFilterSynchronously(maintainPage = false)
                    }
                }

                _uiEvents.emit("Đã import $importedNewCount từ mới, gộp $mergedCount từ trùng và bỏ qua $invalidSkipCount dòng lỗi")
                _statusMessage.value = "Đã import $importedNewCount từ, gộp $mergedCount"
            } catch (e: Exception) {
                e.printStackTrace()
                _uiEvents.emit("Lỗi khi batch import: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetUnsavedChanges() {
        _hasUnsavedChanges.value = false
    }
}
