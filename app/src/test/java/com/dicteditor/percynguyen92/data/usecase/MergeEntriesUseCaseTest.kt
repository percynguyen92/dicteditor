package com.dicteditor.percynguyen92.data.usecase

import com.dicteditor.percynguyen92.data.model.DictEntry
import com.dicteditor.percynguyen92.data.repository.dictionary.DictionaryStateHolder
import com.dicteditor.percynguyen92.data.repository.dictionary.ImportMergeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MergeEntriesUseCaseTest {

    private lateinit var state: DictionaryStateHolder
    private lateinit var useCase: MergeEntriesUseCase

    @Before
    fun setup() {
        state = DictionaryStateHolder()
        useCase = MergeEntriesUseCase(state)
    }

    @Test
    fun `batchImport parses valid and invalid lines`() = runTest {
        val rawText = "龙=rồng\nInvalidLine\n火=lửa"
        
        val result = useCase.batchImport(rawText, ImportMergeMode.INSERT)

        assertEquals(2, result.importedNewCount)
        assertEquals(0, result.mergedCount)
        assertEquals(1, result.invalidSkipCount)
        
        val entries = state.entriesFlow.value
        assertEquals(2, entries.size)
        assertTrue(entries.any { it.chinese == "龙" })
        assertTrue(entries.any { it.chinese == "火" })
    }

    @Test
    fun `batchImportEntries new entries added to top`() = runTest {
        // Initial state
        state.allEntries.add(DictEntry("1", "Old", listOf("cũ")))
        state.updateStateFlowsLocked()

        val newEntries = listOf(
            DictEntry("temp1", "New1", listOf("mới 1")),
            DictEntry("temp2", "New2", listOf("mới 2"))
        )

        useCase.batchImportEntries(newEntries, ImportMergeMode.INSERT)

        val entries = state.entriesFlow.value
        assertEquals(3, entries.size)
        // Modified/New entries should be at the top
        assertEquals("New1", entries[0].chinese)
        assertEquals("New2", entries[1].chinese)
        assertEquals("Old", entries[2].chinese)
    }

    @Test
    fun `batchImportEntries existing entries replace mode`() = runTest {
        state.allEntries.add(DictEntry("id1", "龙", listOf("rồng")))
        state.updateStateFlowsLocked()

        val importEntries = listOf(DictEntry("temp1", "龙", listOf("long")))

        useCase.batchImportEntries(importEntries, ImportMergeMode.REPLACE)

        val entries = state.entriesFlow.value
        assertEquals(1, entries.size)
        assertEquals(listOf("long"), entries[0].meanings)
    }

    @Test
    fun `batchImportEntries existing entries append mode`() = runTest {
        state.allEntries.add(DictEntry("id1", "龙", listOf("rồng")))
        state.updateStateFlowsLocked()

        val importEntries = listOf(DictEntry("temp1", "龙", listOf("long")))

        useCase.batchImportEntries(importEntries, ImportMergeMode.APPEND)

        val entries = state.entriesFlow.value
        assertEquals(1, entries.size)
        // APPEND: existing + new
        assertEquals(listOf("rồng", "long"), entries[0].meanings)
    }

    @Test
    fun `batchImportEntries existing entries insert mode`() = runTest {
        state.allEntries.add(DictEntry("id1", "龙", listOf("rồng")))
        state.updateStateFlowsLocked()

        val importEntries = listOf(DictEntry("temp1", "龙", listOf("long")))

        useCase.batchImportEntries(importEntries, ImportMergeMode.INSERT)

        val entries = state.entriesFlow.value
        assertEquals(1, entries.size)
        // INSERT: new + existing
        assertEquals(listOf("long", "rồng"), entries[0].meanings)
    }

    @Test
    fun `batchImportEntries updates id counter correctly`() = runTest {
        state.lastLoadedIdCounter = 10
        
        val importEntries = listOf(DictEntry("temp1", "龙", listOf("rồng")))
        
        useCase.batchImportEntries(importEntries, ImportMergeMode.INSERT)
        
        assertEquals(11, state.lastLoadedIdCounter)
        assertEquals("entry_11", state.entriesFlow.value[0].id)
    }

    @Test
    fun `batchImportEntries sets highlighted ids`() = runTest {
        state.allEntries.add(DictEntry("id1", "Old", listOf("cũ")))
        state.updateStateFlowsLocked()

        val importEntries = listOf(
            DictEntry("temp1", "New", listOf("mới")),
            DictEntry("temp2", "Old", listOf("cũ", "xưa"))
        )

        useCase.batchImportEntries(importEntries, ImportMergeMode.APPEND)

        val highlighted = state.highlightedIds.value
        assertEquals(2, highlighted.size)
        assertTrue(highlighted.contains("id1")) // Modified "Old"
        assertTrue(highlighted.contains("entry_1")) // New "New" (starts from 0+1)
    }
}
