package com.dicteditor.percynguyen92.data.usecase

import com.dicteditor.percynguyen92.data.model.DictEntry
import com.dicteditor.percynguyen92.data.repository.dictionary.DictionaryStateHolder
import com.dicteditor.percynguyen92.data.repository.dictionary.ImportMergeMode
import com.dicteditor.percynguyen92.data.repository.dictionary.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MergeEntriesUseCase(private val state: DictionaryStateHolder) {

    suspend fun batchImport(
        rawText: String,
        mergeMode: ImportMergeMode = ImportMergeMode.INSERT
    ): ImportResult = withContext(Dispatchers.Default) {
        if (rawText.isBlank()) return@withContext ImportResult(0, 0, 0)

        val lines = rawText.lines()
        var invalidSkipCount = 0

        val parsedEntries = lines.mapNotNull { line ->
            if (line.trim().isEmpty()) return@mapNotNull null
            val entry = DictEntry.parse(line)
            if (entry == null) {
                invalidSkipCount++
                null
            } else {
                entry
            }
        }

        state.mutex.withLock {
            val result = mergeEntriesLocked(parsedEntries, mergeMode)
            result.copy(invalidSkipCount = invalidSkipCount)
        }
    }

    suspend fun batchImportEntries(
        entriesToImport: List<DictEntry>,
        mergeMode: ImportMergeMode = ImportMergeMode.INSERT
    ): ImportResult = withContext(Dispatchers.Default) {
        if (entriesToImport.isEmpty()) return@withContext ImportResult(0, 0, 0)

        state.mutex.withLock {
            mergeEntriesLocked(entriesToImport, mergeMode)
        }
    }

    private suspend fun mergeEntriesLocked(
        entriesToMerge: List<DictEntry>,
        mergeMode: ImportMergeMode
    ): ImportResult {
        state.historyManager.saveToHistory(state.allEntries)

        val keyToIndex = state.allEntries.mapIndexed { index, entry -> 
            entry.chinese.lowercase().trim() to index 
        }.toMap().toMutableMap()

        val updatedAllEntries = ArrayList(state.allEntries)
        var idCounterTemp = state.lastLoadedIdCounter
        val modifiedIdsSet = mutableSetOf<String>()
        var importedNewCount = 0
        var mergedCount = 0

        entriesToMerge.forEach { entryTemp ->
            val keyLower = entryTemp.chinese.lowercase().trim()
            val existingIndex = keyToIndex[keyLower]
            if (existingIndex != null) {
                val oldEntry = updatedAllEntries[existingIndex]
                val mergedMeanings = when (mergeMode) {
                    ImportMergeMode.REPLACE -> entryTemp.meanings
                    ImportMergeMode.APPEND -> (oldEntry.meanings + entryTemp.meanings).distinct()
                    ImportMergeMode.INSERT -> (entryTemp.meanings + oldEntry.meanings).distinct()
                }
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

        state.allEntries = newAllEntries
        state.lastLoadedIdCounter = idCounterTemp
        state.setHighlightedIds(modifiedIdsSet)

        state.updateStateFlowsLocked()

        return ImportResult(importedNewCount, mergedCount, 0)
    }
}
