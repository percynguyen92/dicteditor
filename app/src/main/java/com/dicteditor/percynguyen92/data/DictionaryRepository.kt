package com.dicteditor.percynguyen92.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface EntryOpResult {
    object Success : EntryOpResult
    object Duplicate : EntryOpResult
    object Merged : EntryOpResult
    object Invalid : EntryOpResult
}

data class ImportResult(
    val importedNewCount: Int,
    val mergedCount: Int,
    val invalidSkipCount: Int
)

class DictionaryRepository(maxHistorySize: Int = 10) {
    private val mutex = Mutex()
    private var allEntries = ArrayList<DictEntry>()
    private var savedState = ArrayList<DictEntry>()
    private val historyManager = HistoryManager<DictEntry>(maxHistorySize)

    private var activeLoadSessionId = 0
    private var lastLoadedIdCounter = 0

    private val _entriesFlow = MutableStateFlow<List<DictEntry>>(emptyList())
    val entriesFlow: StateFlow<List<DictEntry>> = _entriesFlow.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _openedFileUri = MutableStateFlow<Uri?>(null)
    val openedFileUri: StateFlow<Uri?> = _openedFileUri.asStateFlow()

    private val _highlightedIds = MutableStateFlow<Set<String>>(emptySet())
    val highlightedIds: StateFlow<Set<String>> = _highlightedIds.asStateFlow()

    private fun updateStateFlowsLocked() {
        _entriesFlow.value = ArrayList(allEntries)
        _hasUnsavedChanges.value = allEntries != savedState
        _canUndo.value = historyManager.canUndo()
        _canRedo.value = historyManager.canRedo()
    }

    fun clearHighlightedIds() {
        _highlightedIds.value = emptySet()
    }

    fun resetUnsavedChanges() {
        _hasUnsavedChanges.value = false
    }

    suspend fun loadFile(context: Context, uri: Uri): Result<Int> {
        val sessionId = mutex.withLock {
            activeLoadSessionId++
            activeLoadSessionId
        }

        val loaded = try {
            FileHandler.readLines(context.applicationContext, uri)
        } catch (e: Throwable) {
            return Result.failure(e)
        }

        return mutex.withLock {
            if (sessionId != activeLoadSessionId) {
                return@withLock Result.failure(CancellationException("Stale load task cancelled"))
            }
            allEntries = ArrayList(loaded)
            savedState = ArrayList(loaded)
            lastLoadedIdCounter = allEntries.size + 1
            historyManager.clear()
            _openedFileUri.value = uri
            _highlightedIds.value = emptySet()
            updateStateFlowsLocked()
            Result.success(loaded.size)
        }
    }

    suspend fun saveFile(context: Context): Result<Int> {
        val uri = _openedFileUri.value ?: return Result.failure(IllegalStateException("No file is currently opened"))
        
        val entriesToSave = mutex.withLock { ArrayList(allEntries) }

        val result = try {
            // Use NonCancellable to prevent file corruption during stream copy if cancelled
            withContext(NonCancellable) {
                val count = FileHandler.writeLines(context.applicationContext, uri, entriesToSave)
                Result.success(count)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

        mutex.withLock {
            if (result.isSuccess) {
                savedState = ArrayList(entriesToSave)
            }
            updateStateFlowsLocked()
        }

        return result
    }

    suspend fun closeFile() {
        mutex.withLock {
            activeLoadSessionId++
            _openedFileUri.value = null
            allEntries = ArrayList()
            savedState = ArrayList()
            historyManager.clear()
            _highlightedIds.value = emptySet()
            updateStateFlowsLocked()
        }
    }

    suspend fun undo(): Boolean {
        return mutex.withLock {
            val currentState = ArrayList(allEntries)
            val prevState = historyManager.undo(currentState)
            if (prevState != null) {
                allEntries = ArrayList(prevState)
                updateStateFlowsLocked()
                true
            } else {
                false
            }
        }
    }

    suspend fun redo(): Boolean {
        return mutex.withLock {
            val currentState = ArrayList(allEntries)
            val nextState = historyManager.redo(currentState)
            if (nextState != null) {
                allEntries = ArrayList(nextState)
                updateStateFlowsLocked()
                true
            } else {
                false
            }
        }
    }

    suspend fun addEntry(chinese: String, meanings: List<String>): EntryOpResult {
        val cleanChinese = chinese.trim()
        if (cleanChinese.isEmpty()) return EntryOpResult.Invalid

        return mutex.withLock {
            val exists = allEntries.any { it.chinese.equals(cleanChinese, ignoreCase = true) }
            if (exists) {
                return@withLock EntryOpResult.Duplicate
            }

            historyManager.saveToHistory(allEntries)
            lastLoadedIdCounter++
            val entry = DictEntry(
                "entry_$lastLoadedIdCounter",
                cleanChinese,
                meanings.map { it.trim() }.filter { it.isNotEmpty() }
            )
            allEntries.add(0, entry)
            _highlightedIds.value = setOf(entry.id)
            updateStateFlowsLocked()
            EntryOpResult.Success
        }
    }

    suspend fun updateEntry(id: String, chinese: String, meanings: List<String>): EntryOpResult {
        val cleanChinese = chinese.trim()
        if (cleanChinese.isEmpty()) return EntryOpResult.Invalid

        return mutex.withLock {
            val entryIndex = allEntries.indexOfFirst { it.id == id }
            if (entryIndex == -1) return@withLock EntryOpResult.Invalid

            val duplicateIndex = allEntries.indexOfFirst { i ->
                i.chinese.equals(cleanChinese, ignoreCase = true) && i.id != id
            }
            if (duplicateIndex != -1) {
                historyManager.saveToHistory(allEntries)
                val duplicateEntry = allEntries[duplicateIndex]
                val mergedMeanings = (duplicateEntry.meanings + meanings.map { it.trim() }.filter { it.isNotEmpty() }).distinct()
                allEntries[duplicateIndex] = duplicateEntry.copy(meanings = mergedMeanings)
                allEntries.removeAt(entryIndex)
                updateStateFlowsLocked()
                return@withLock EntryOpResult.Merged
            }

            historyManager.saveToHistory(allEntries)
            val updated = DictEntry(
                id,
                cleanChinese,
                meanings.map { it.trim() }.filter { it.isNotEmpty() }
            )
            allEntries[entryIndex] = updated
            updateStateFlowsLocked()
            EntryOpResult.Success
        }
    }

    suspend fun deleteEntry(id: String) {
        mutex.withLock {
            val entryIndex = allEntries.indexOfFirst { it.id == id }
            if (entryIndex == -1) return@withLock

            historyManager.saveToHistory(allEntries)
            allEntries.removeAt(entryIndex)
            updateStateFlowsLocked()
        }
    }

    suspend fun deleteEntries(ids: Set<String>) {
        if (ids.isEmpty()) return
        mutex.withLock {
            historyManager.saveToHistory(allEntries)
            allEntries.removeAll { it.id in ids }
            updateStateFlowsLocked()
        }
    }

    suspend fun sortByDefaultLengthDescending() = withContext(Dispatchers.Default) {
        mutex.withLock {
            historyManager.saveToHistory(allEntries)
            allEntries.sortByDescending { it.chinese.length }
            updateStateFlowsLocked()
        }
    }

    suspend fun sortByLengthAscending() = withContext(Dispatchers.Default) {
        mutex.withLock {
            historyManager.saveToHistory(allEntries)
            allEntries.sortBy { it.chinese.length }
            updateStateFlowsLocked()
        }
    }

    suspend fun findAndReplace(
        findText: String,
        replaceText: String,
        useRegex: Boolean,
        matchCase: Boolean,
        scopeIds: Set<String>? = null
    ): Result<Int> = withContext(Dispatchers.Default) {
        if (findText.isEmpty()) return@withContext Result.success(0)

        mutex.withLock {
            val scopedEntries = if (scopeIds != null) {
                allEntries.filter { it.id in scopeIds }
            } else {
                allEntries
            }

            val result = SearchEngine.replaceInEntries(
                findText = findText,
                replaceText = replaceText,
                entries = scopedEntries,
                useRegex = useRegex,
                matchCase = matchCase
            )

            val replaceResult = result.getOrElse {
                return@withContext Result.failure(it)
            }

            if (replaceResult.totalReplacements > 0) {
                historyManager.saveToHistory(allEntries)
                val replacedMap = replaceResult.replacedEntries
                for (i in allEntries.indices) {
                    replacedMap[allEntries[i].id]?.let {
                        allEntries[i] = it
                    }
                }
                updateStateFlowsLocked()
            }
            Result.success(replaceResult.totalReplacements)
        }
    }


    suspend fun batchImport(rawText: String): ImportResult = withContext(Dispatchers.Default) {
        if (rawText.isBlank()) return@withContext ImportResult(0, 0, 0)

        val lines = rawText.lines()
        var invalidSkipCount = 0

        // Perform heavy parsing outside mutex lock
        val parsedEntries = lines.mapNotNull { line ->
            if (line.trim().isEmpty()) return@mapNotNull null
            val entry = DictEntry.parse(line, "")
            if (entry == null) {
                invalidSkipCount++
                null
            } else {
                entry
            }
        }

        mutex.withLock {
            historyManager.saveToHistory(allEntries)

            val keyToIndex = allEntries.mapIndexed { index, entry -> 
                entry.chinese.lowercase().trim() to index 
            }.toMap().toMutableMap()

            val updatedAllEntries = ArrayList(allEntries)
            var idCounterTemp = lastLoadedIdCounter
            val modifiedIdsSet = mutableSetOf<String>()
            var importedNewCount = 0
            var mergedCount = 0

            parsedEntries.forEach { entryTemp ->
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

            allEntries = newAllEntries
            lastLoadedIdCounter = idCounterTemp
            _highlightedIds.value = modifiedIdsSet

            updateStateFlowsLocked()

            ImportResult(importedNewCount, mergedCount, invalidSkipCount)
        }
    }

    suspend fun getExportStringForSelected(selectedIds: Set<String>): String = mutex.withLock {
        val entriesToExport = allEntries.filter { selectedIds.contains(it.id) }
        val sb = java.lang.StringBuilder()
        for (entry in entriesToExport) {
            val chinese = entry.chinese
            val meanings = entry.meanings.joinToString("/")
            sb.append("$chinese=$meanings\n")
        }
        sb.toString()
    }

    suspend fun exportSelectedEntries(context: Context, uri: Uri, selectedIds: Set<String>): Result<Int> {
        val entriesToExport = mutex.withLock {
            allEntries.filter { selectedIds.contains(it.id) }
        }

        return try {
            withContext(NonCancellable) {
                val exportedCount = FileHandler.writeLines(context.applicationContext, uri, entriesToExport)
                Result.success(exportedCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
