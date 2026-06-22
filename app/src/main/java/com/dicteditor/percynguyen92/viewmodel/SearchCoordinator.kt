package com.dicteditor.percynguyen92.viewmodel

import com.dicteditor.percynguyen92.data.DictEntry
import com.dicteditor.percynguyen92.data.SearchEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchCoordinator(
    private val scope: CoroutineScope,
    private val entriesSource: StateFlow<List<DictEntry>>,
    private val onLoadingChange: (Boolean) -> Unit
) {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _useRegex = MutableStateFlow(false)
    val useRegex: StateFlow<Boolean> = _useRegex.asStateFlow()

    private val _matchCase = MutableStateFlow(false)
    val matchCase: StateFlow<Boolean> = _matchCase.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _filtered = MutableStateFlow<List<DictEntry>>(emptyList())
    val filtered: StateFlow<List<DictEntry>> = _filtered.asStateFlow()

    private val _filteredCount = MutableStateFlow(0)
    val filteredCount: StateFlow<Int> = _filteredCount.asStateFlow()

    private var searchJob: Job? = null

    /** Called when the entries source changes (from ViewModel.init collector) */
    suspend fun onEntriesChanged(entries: List<DictEntry>) {
        applyFilter(entries, debounce = false)
    }

    fun setQuery(query: String) {
        _query.value = query
        scheduleFilter()
    }

    fun setUseRegex(useRegex: Boolean) {
        _useRegex.value = useRegex
        scheduleFilter()
    }

    fun setMatchCase(matchCase: Boolean) {
        _matchCase.value = matchCase
        scheduleFilter()
    }

    private fun scheduleFilter() {
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(300)
            applyFilter(entriesSource.value, debounce = true)
        }
    }

    private suspend fun applyFilter(entries: List<DictEntry>, debounce: Boolean) {
        if (debounce) onLoadingChange(true)
        try {
            val result = withContext(Dispatchers.Default) {
                SearchEngine.filterEntries(
                    query = _query.value,
                    entries = entries,
                    useRegex = _useRegex.value,
                    matchCase = _matchCase.value
                )
            }
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { list ->
                        _error.value = null
                        _filtered.value = list
                        _filteredCount.value = list.size
                    },
                    onFailure = { e ->
                        _error.value = e.message
                        _filtered.value = emptyList()
                        _filteredCount.value = 0
                    }
                )
            }
        } finally {
            if (debounce) onLoadingChange(false)
        }
    }
}
