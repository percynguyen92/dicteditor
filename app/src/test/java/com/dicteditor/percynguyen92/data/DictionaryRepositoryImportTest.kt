package com.dicteditor.percynguyen92.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class DictionaryRepositoryImportTest {

    @Test
    fun testBatchImportEntries_InsertMode() = runTest {
        val repository = DictionaryRepository(maxHistorySize = 10)
        
        // Setup initial data
        repository.batchImport("龙=rồng", ImportMergeMode.INSERT)
        assertEquals(1, repository.entriesFlow.value.size)

        // Import entries
        val newEntries = listOf(
            DictEntry("temp_1", "龙", listOf("long")),
            DictEntry("temp_2", "火", listOf("lửa", "hỏa"))
        )
        
        val result = repository.batchImportEntries(newEntries, ImportMergeMode.INSERT)
        
        assertEquals(1, result.importedNewCount)
        assertEquals(1, result.mergedCount)
        
        val allEntries = repository.entriesFlow.value
        assertEquals(2, allEntries.size)
        
        val dragonEntry = allEntries.find { it.chinese == "龙" }!!
        assertEquals(listOf("long", "rồng"), dragonEntry.meanings) // INSERT adds to front
        
        val fireEntry = allEntries.find { it.chinese == "火" }!!
        assertEquals(listOf("lửa", "hỏa"), fireEntry.meanings)
    }

    @Test
    fun testBatchImportEntries_AppendMode() = runTest {
        val repository = DictionaryRepository(maxHistorySize = 10)
        
        repository.batchImport("龙=rồng", ImportMergeMode.INSERT)

        val newEntries = listOf(
            DictEntry("temp_1", "龙", listOf("long"))
        )
        
        repository.batchImportEntries(newEntries, ImportMergeMode.APPEND)
        
        val allEntries = repository.entriesFlow.value
        val dragonEntry = allEntries.find { it.chinese == "龙" }!!
        assertEquals(listOf("rồng", "long"), dragonEntry.meanings) // APPEND adds to back
    }

    @Test
    fun testBatchImportEntries_ReplaceMode() = runTest {
        val repository = DictionaryRepository(maxHistorySize = 10)
        
        repository.batchImport("龙=rồng", ImportMergeMode.INSERT)

        val newEntries = listOf(
            DictEntry("temp_1", "龙", listOf("long"))
        )
        
        repository.batchImportEntries(newEntries, ImportMergeMode.REPLACE)
        
        val allEntries = repository.entriesFlow.value
        val dragonEntry = allEntries.find { it.chinese == "龙" }!!
        assertEquals(listOf("long"), dragonEntry.meanings) // REPLACE overwrites
    }

    @Test
    fun testBatchImportEntries_EmptyList() = runTest {
        val repository = DictionaryRepository(maxHistorySize = 10)
        
        repository.batchImport("龙=rồng", ImportMergeMode.INSERT)

        val result = repository.batchImportEntries(emptyList(), ImportMergeMode.INSERT)
        
        assertEquals(0, result.importedNewCount)
        assertEquals(0, result.mergedCount)
        
        assertEquals(1, repository.entriesFlow.value.size)
    }
}
