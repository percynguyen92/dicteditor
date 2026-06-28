package com.dicteditor.percynguyen92.data.repository.dictionary

import android.content.Context
import android.net.Uri
import com.dicteditor.percynguyen92.data.model.DictEntry
import com.dicteditor.percynguyen92.data.model.InvalidLine
import com.dicteditor.percynguyen92.data.model.ParseResult
import com.dicteditor.percynguyen92.data.local.FileHandler
import com.dicteditor.percynguyen92.data.usecase.ExportUseCase
import com.dicteditor.percynguyen92.data.usecase.MergeEntriesUseCase
import com.dicteditor.percynguyen92.utils.SearchEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
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

/**
 * Defines how duplicate entries are handled during batch import.
 */
enum class ImportMergeMode {
    /** Replace all existing meanings with new ones */
    REPLACE,
    /** Keep existing meanings, add new ones after */
    APPEND,
    /** Keep existing meanings, add new ones before (prioritize new) */
    INSERT
}

class DictionaryRepository(maxHistorySize: Int = 10) {
    val state = DictionaryStateHolder(maxHistorySize)
    private val mergeUseCase = MergeEntriesUseCase(state)
    private val exportUseCase = ExportUseCase(state)

    val entriesFlow: StateFlow<List<DictEntry>> = state.entriesFlow
    val invalidLinesFlow: StateFlow<List<InvalidLine>> = state.invalidLinesFlow
    val hasUnsavedChanges: StateFlow<Boolean> = state.hasUnsavedChanges
    val canUndo: StateFlow<Boolean> = state.canUndo
    val canRedo: StateFlow<Boolean> = state.canRedo
    val openedFileUri: StateFlow<Uri?> = state.openedFileUri
    val highlightedIds: StateFlow<Set<String>> = state.highlightedIds

    fun clearHighlightedIds() {
        state.clearHighlightedIds()
    }

    fun resetUnsavedChanges() {
        state.resetUnsavedChanges()
    }

    suspend fun loadFile(context: Context, uri: Uri): Result<Int> {
        val sessionId = state.mutex.withLock {
            state.activeLoadSessionId++
            state.activeLoadSessionId
        }

        val localInvalidLines = ArrayList<InvalidLine>()
        var idCounter = 1
        var invalidIdCounter = 1

        val loaded = try {
            FileHandler.readLines(context.applicationContext, uri) { line, lineNumber ->
                when (val result = DictEntry.parse(line, "entry_$idCounter")) {
                    is ParseResult.Success -> {
                        idCounter++
                        result.entry
                    }
                    is ParseResult.Failure -> {
                        localInvalidLines.add(
                            InvalidLine(
                                id = "invalid_$invalidIdCounter",
                                lineNumber = lineNumber,
                                lineContent = line,
                                error = result.error
                            )
                        )
                        invalidIdCounter++
                        null
                    }
                }
            }
        } catch (e: Throwable) {
            return Result.failure(e)
        }

        return state.mutex.withLock {
            if (sessionId != state.activeLoadSessionId) {
                return@withLock Result.failure(CancellationException("Stale load task cancelled"))
            }
            state.allEntries = ArrayList(loaded)
            state.savedState = ArrayList(loaded)
            state.invalidLines = localInvalidLines
            state.lastLoadedIdCounter = idCounter
            state.historyManager.clear()
            state.setOpenedFileUri(uri)
            state.clearHighlightedIds()
            state.updateStateFlowsLocked()
            Result.success(loaded.size)
        }
    }

    suspend fun saveFile(context: Context): Result<Int> {
        val uri = state.openedFileUri.value ?: return Result.failure(IllegalStateException("No file is currently opened"))
        
        val entriesToSave = state.mutex.withLock { ArrayList(state.allEntries) }

        val result = try {
            withContext(NonCancellable) {
                val count = FileHandler.writeLines(context.applicationContext, uri, entriesToSave) { it.toLine() }
                Result.success(count)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

        state.mutex.withLock {
            if (result.isSuccess) {
                state.savedState = ArrayList(entriesToSave)
            }
            state.updateStateFlowsLocked()
        }

        return result
    }

    suspend fun closeFile() {
        state.mutex.withLock {
            state.activeLoadSessionId++
            state.setOpenedFileUri(null)
            state.allEntries = ArrayList()
            state.savedState = ArrayList()
            state.invalidLines = ArrayList()
            state.historyManager.clear()
            state.clearHighlightedIds()
            state.updateStateFlowsLocked()
        }
    }

    suspend fun undo(): Boolean {
        return state.mutex.withLock {
            val currentState = ArrayList(state.allEntries)
            val prevState = state.historyManager.undo(currentState)
            if (prevState != null) {
                state.allEntries = ArrayList(prevState)
                state.updateStateFlowsLocked()
                true
            } else {
                false
            }
        }
    }

    suspend fun redo(): Boolean {
        return state.mutex.withLock {
            val currentState = ArrayList(state.allEntries)
            val nextState = state.historyManager.redo(currentState)
            if (nextState != null) {
                state.allEntries = ArrayList(nextState)
                state.updateStateFlowsLocked()
                true
            } else {
                false
            }
        }
    }

    suspend fun addEntry(chinese: String, meanings: List<String>): EntryOpResult {
        val cleanChinese = chinese.trim()
        if (cleanChinese.isEmpty()) return EntryOpResult.Invalid

        return state.mutex.withLock {
            val exists = state.allEntries.any { it.chinese.equals(cleanChinese, ignoreCase = true) }
            if (exists) {
                return@withLock EntryOpResult.Duplicate
            }

            state.historyManager.saveToHistory(state.allEntries)
            state.lastLoadedIdCounter++
            val entry = DictEntry(
                "entry_${state.lastLoadedIdCounter}",
                cleanChinese,
                meanings.map { it.trim() }.filter { it.isNotEmpty() }
            )
            state.allEntries.add(0, entry)
            state.setHighlightedIds(setOf(entry.id))
            state.updateStateFlowsLocked()
            EntryOpResult.Success
        }
    }

    suspend fun updateEntry(id: String, chinese: String, meanings: List<String>): EntryOpResult {
        val cleanChinese = chinese.trim()
        if (cleanChinese.isEmpty()) return EntryOpResult.Invalid

        return state.mutex.withLock {
            val entryIndex = state.allEntries.indexOfFirst { it.id == id }
            if (entryIndex == -1) return@withLock EntryOpResult.Invalid

            val duplicateIndex = state.allEntries.indexOfFirst { i ->
                i.chinese.equals(cleanChinese, ignoreCase = true) && i.id != id
            }
            if (duplicateIndex != -1) {
                state.historyManager.saveToHistory(state.allEntries)
                val duplicateEntry = state.allEntries[duplicateIndex]
                val mergedMeanings = (duplicateEntry.meanings + meanings.map { it.trim() }.filter { it.isNotEmpty() }).distinct()
                state.allEntries[duplicateIndex] = duplicateEntry.copy(meanings = mergedMeanings)
                state.allEntries.removeAt(entryIndex)
                state.updateStateFlowsLocked()
                return@withLock EntryOpResult.Merged
            }

            state.historyManager.saveToHistory(state.allEntries)
            val updated = DictEntry(
                id,
                cleanChinese,
                meanings.map { it.trim() }.filter { it.isNotEmpty() }
            )
            state.allEntries[entryIndex] = updated
            state.updateStateFlowsLocked()
            EntryOpResult.Success
        }
    }

    suspend fun deleteEntry(id: String) {
        state.mutex.withLock {
            val entryIndex = state.allEntries.indexOfFirst { it.id == id }
            if (entryIndex == -1) return@withLock

            state.historyManager.saveToHistory(state.allEntries)
            state.allEntries.removeAt(entryIndex)
            state.updateStateFlowsLocked()
        }
    }

    suspend fun deleteEntries(ids: Set<String>) {
        if (ids.isEmpty()) return
        state.mutex.withLock {
            state.historyManager.saveToHistory(state.allEntries)
            state.allEntries.removeAll { it.id in ids }
            state.updateStateFlowsLocked()
        }
    }

    suspend fun countDuplicateKeys(): Int = withContext(Dispatchers.Default) {
        state.mutex.withLock {
            val groupCounts = state.allEntries.groupingBy { it.chinese.lowercase() }.eachCount()
            groupCounts.count { it.value > 1 }
        }
    }

    suspend fun mergeDuplicateKeys(): Pair<Int, Int> = withContext(Dispatchers.Default) {
        state.mutex.withLock {
            val groups = state.allEntries.groupBy { it.chinese.lowercase() }
            val duplicateGroups = groups.filterValues { it.size > 1 }
            
            if (duplicateGroups.isEmpty()) return@withLock Pair(0, 0)
            
            state.historyManager.saveToHistory(state.allEntries)
            
            var mergedKeyCount = 0
            var removedEntryCount = 0
            val newEntries = mutableListOf<DictEntry>()
            val processedKeys = mutableSetOf<String>()
            
            for (entry in state.allEntries) {
                val key = entry.chinese.lowercase()
                if (key in processedKeys) continue
                
                val group = duplicateGroups[key]
                if (group != null) {
                    val mergedMeanings = group
                        .flatMap { it.meanings }
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                    newEntries.add(entry.copy(meanings = mergedMeanings))
                    mergedKeyCount++
                    removedEntryCount += group.size - 1
                } else {
                    newEntries.add(entry)
                }
                processedKeys.add(key)
            }
            
            state.allEntries = ArrayList(newEntries)
            state.updateStateFlowsLocked()
            Pair(mergedKeyCount, removedEntryCount)
        }
    }

    suspend fun sortByDefaultLengthDescending() = withContext(Dispatchers.Default) {
        state.mutex.withLock {
            state.historyManager.saveToHistory(state.allEntries)
            state.allEntries.sortByDescending { it.chinese.length }
            state.updateStateFlowsLocked()
        }
    }

    suspend fun sortByLengthAscending() = withContext(Dispatchers.Default) {
        state.mutex.withLock {
            state.historyManager.saveToHistory(state.allEntries)
            state.allEntries.sortBy { it.chinese.length }
            state.updateStateFlowsLocked()
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

        state.mutex.withLock {
            val scopedEntries = if (scopeIds != null) {
                state.allEntries.filter { it.id in scopeIds }
            } else {
                state.allEntries
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
                state.historyManager.saveToHistory(state.allEntries)
                val replacedMap = replaceResult.replacedEntries
                for (i in state.allEntries.indices) {
                    replacedMap[state.allEntries[i].id]?.let {
                        state.allEntries[i] = it
                    }
                }
                state.updateStateFlowsLocked()
            }
            Result.success(replaceResult.totalReplacements)
        }
    }

    suspend fun batchImport(
        rawText: String,
        mergeMode: ImportMergeMode = ImportMergeMode.INSERT
    ): ImportResult = mergeUseCase.batchImport(rawText, mergeMode)

    suspend fun batchImportEntries(
        entriesToImport: List<DictEntry>,
        mergeMode: ImportMergeMode = ImportMergeMode.INSERT
    ): ImportResult = mergeUseCase.batchImportEntries(entriesToImport, mergeMode)

    suspend fun getExportStringForSelected(selectedIds: Set<String>): String = exportUseCase.getExportStringForSelected(selectedIds)

    suspend fun exportSelectedEntries(context: Context, uri: Uri, selectedIds: Set<String>): Result<Int> = exportUseCase.exportSelectedEntries(context, uri, selectedIds)
}
