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
        } catch (e: Exception) {
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

            val exists = allEntries.indices.any { i ->
                i != entryIndex && allEntries[i].chinese.equals(cleanChinese, ignoreCase = true)
            }
            if (exists) {
                return@withLock EntryOpResult.Duplicate
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

        val regexOptions = if (!matchCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
        val regex = if (useRegex) {
            try {
                Regex(findText, regexOptions)
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        } else null

        mutex.withLock {
            var totalReplacements = 0
            val updated = allEntries.map { entry ->
                // Neu scopeIds duoc chi dinh, chi xu ly nhung entry nam trong scope
                if (scopeIds != null && entry.id !in scopeIds) return@map entry

                var lineChanged = false
                var newChinese = entry.chinese

                if (useRegex && regex != null) {
                    val matches = regex.findAll(newChinese).toList()
                    if (matches.isNotEmpty()) {
                        totalReplacements += matches.size
                        lineChanged = true
                        newChinese = newChinese.replace(regex, replaceText)
                    }
                } else {
                    val count = newChinese.split(findText, ignoreCase = !matchCase).size - 1
                    if (count > 0) {
                        totalReplacements += count
                        lineChanged = true
                        newChinese = newChinese.replace(findText, replaceText, ignoreCase = !matchCase)
                    }
                }

                val newMeanings = entry.meanings.map { meaning ->
                    if (useRegex && regex != null) {
                        val matches = regex.findAll(meaning).toList()
                        if (matches.isNotEmpty()) {
                            totalReplacements += matches.size
                            lineChanged = true
                            meaning.replace(regex, replaceText)
                        } else meaning
                    } else {
                        val count = meaning.split(findText, ignoreCase = !matchCase).size - 1
                        if (count > 0) {
                            totalReplacements += count
                            lineChanged = true
                            meaning.replace(findText, replaceText, ignoreCase = !matchCase)
                        } else meaning
                    }
                }

                if (lineChanged) entry.copy(chinese = newChinese, meanings = newMeanings)
                else entry
            }

            if (totalReplacements > 0) {
                historyManager.saveToHistory(allEntries)
                allEntries = ArrayList(updated)
                updateStateFlowsLocked()
            }
            Result.success(totalReplacements)
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
