package com.dicteditor.percynguyen92.data.repository.dictionary

import com.dicteditor.percynguyen92.data.model.DictEntry
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
class DictionaryRepositoryTest {

    private lateinit var repository: DictionaryRepository

    @Before
    fun setup() {
        repository = DictionaryRepository(maxHistorySize = 10)
    }

    @Test
    fun `addEntry success adds new entry to the top`() = runTest {
        val result = repository.addEntry("龙", listOf("rồng", "long"))

        assertEquals(EntryOpResult.Success, result)
        val entries = repository.entriesFlow.value
        assertEquals(1, entries.size)
        assertEquals("龙", entries[0].chinese)
        assertEquals(listOf("rồng", "long"), entries[0].meanings)
        assertTrue(repository.hasUnsavedChanges.value)
    }

    @Test
    fun `addEntry duplicate returns Duplicate result`() = runTest {
        repository.addEntry("龙", listOf("rồng"))
        
        val result = repository.addEntry("龙", listOf("long"))

        assertEquals(EntryOpResult.Duplicate, result)
        assertEquals(1, repository.entriesFlow.value.size)
    }

    @Test
    fun `updateEntry success updates existing entry`() = runTest {
        repository.addEntry("龙", listOf("rồng"))
        val entryId = repository.entriesFlow.value[0].id

        val result = repository.updateEntry(entryId, "龙", listOf("rồng", "long"))

        assertEquals(EntryOpResult.Success, result)
        val entries = repository.entriesFlow.value
        assertEquals(1, entries.size)
        assertEquals(listOf("rồng", "long"), entries[0].meanings)
    }

    @Test
    fun `updateEntry to existing chinese merges meanings`() = runTest {
        repository.addEntry("龙", listOf("rồng"))
        repository.addEntry("火", listOf("lửa"))
        
        val entriesBefore = repository.entriesFlow.value
        val fireId = entriesBefore.first { it.chinese == "火" }.id

        // Update "火" to "龙"
        val result = repository.updateEntry(fireId, "龙", listOf("long"))

        assertEquals(EntryOpResult.Merged, result)
        val entriesAfter = repository.entriesFlow.value
        assertEquals(1, entriesAfter.size)
        assertEquals("龙", entriesAfter[0].chinese)
        assertEquals(listOf("rồng", "long"), entriesAfter[0].meanings)
    }

    @Test
    fun `deleteEntry removes entry and saves history`() = runTest {
        repository.addEntry("龙", listOf("rồng"))
        val id = repository.entriesFlow.value[0].id

        repository.deleteEntry(id)

        assertTrue(repository.entriesFlow.value.isEmpty())
        assertTrue(repository.canUndo.value)
    }

    @Test
    fun `undo and redo work correctly`() = runTest {
        repository.addEntry("龙", listOf("rồng")) // Entry 1
        repository.addEntry("火", listOf("lửa")) // Entry 2
        
        assertEquals(2, repository.entriesFlow.value.size)

        // Undo adding "火"
        val undo1 = repository.undo()
        assertTrue(undo1)
        assertEquals(1, repository.entriesFlow.value.size)
        assertEquals("龙", repository.entriesFlow.value[0].chinese)

        // Undo adding "龙"
        val undo2 = repository.undo()
        assertTrue(undo2)
        assertTrue(repository.entriesFlow.value.isEmpty())

        // Redo adding "龙"
        val redo1 = repository.redo()
        assertTrue(redo1)
        assertEquals(1, repository.entriesFlow.value.size)
        assertEquals("龙", repository.entriesFlow.value[0].chinese)
    }

    @Test
    fun `sortByDefaultLengthDescending sorts entries correctly`() = runTest {
        repository.addEntry("龙", listOf("rồng"))
        repository.addEntry("龙王", listOf("long vương"))
        repository.addEntry("龙王爷", listOf("long vương gia"))

        repository.sortByDefaultLengthDescending()

        val entries = repository.entriesFlow.value
        assertEquals("龙王爷", entries[0].chinese)
        assertEquals("龙王", entries[1].chinese)
        assertEquals("龙", entries[2].chinese)
    }

    @Test
    fun `findAndReplace works with plain text`() = runTest {
        repository.addEntry("龙", listOf("rồng", "long"))
        repository.addEntry("龙王", listOf("long vương"))

        val result = repository.findAndReplace(
            findText = "long",
            replaceText = "luồng",
            useRegex = false,
            matchCase = false
        )

        assertEquals(2, result.getOrNull())
        val entries = repository.entriesFlow.value
        // "龙" meanings: "rồng", "long" -> "rồng", "luồng"
        assertTrue(entries.any { it.chinese == "龙" && it.meanings.contains("luồng") })
        // "龙王" meanings: "long vương" -> "luồng vương"
        assertTrue(entries.any { it.chinese == "龙王" && it.meanings.contains("luồng vương") })
    }
}
