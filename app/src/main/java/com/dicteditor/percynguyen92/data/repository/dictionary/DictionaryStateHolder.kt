package com.dicteditor.percynguyen92.data.repository.dictionary

import android.net.Uri
import com.dicteditor.percynguyen92.data.model.DictEntry
import com.dicteditor.percynguyen92.data.model.InvalidLine
import com.dicteditor.percynguyen92.utils.HistoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

class DictionaryStateHolder(maxHistorySize: Int = 10) {
    val mutex = Mutex()
    var allEntries = ArrayList<DictEntry>()
    var savedState = ArrayList<DictEntry>()
    val historyManager = HistoryManager<DictEntry>(maxHistorySize)

    var activeLoadSessionId = 0
    var lastLoadedIdCounter = 0

    private val _entriesFlow = MutableStateFlow<List<DictEntry>>(emptyList())
    val entriesFlow: StateFlow<List<DictEntry>> = _entriesFlow.asStateFlow()

    var invalidLines = ArrayList<InvalidLine>()
    private val _invalidLinesFlow = MutableStateFlow<List<InvalidLine>>(emptyList())
    val invalidLinesFlow: StateFlow<List<InvalidLine>> = _invalidLinesFlow.asStateFlow()

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

    suspend fun updateStateFlowsLocked() {
        _entriesFlow.value = ArrayList(allEntries)
        _invalidLinesFlow.value = ArrayList(invalidLines)
        val hasChanges = withContext(Dispatchers.Default) {
            allEntries != savedState
        }
        _hasUnsavedChanges.value = hasChanges
        _canUndo.value = historyManager.canUndo()
        _canRedo.value = historyManager.canRedo()
    }

    fun clearHighlightedIds() {
        _highlightedIds.value = emptySet()
    }

    fun setHighlightedIds(ids: Set<String>) {
        _highlightedIds.value = ids
    }

    fun resetUnsavedChanges() {
        _hasUnsavedChanges.value = false
    }

    fun setOpenedFileUri(uri: Uri?) {
        _openedFileUri.value = uri
    }
}
