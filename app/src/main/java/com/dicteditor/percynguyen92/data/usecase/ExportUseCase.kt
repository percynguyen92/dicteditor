package com.dicteditor.percynguyen92.data.usecase

import android.content.Context
import android.net.Uri
import com.dicteditor.percynguyen92.data.local.FileHandler
import com.dicteditor.percynguyen92.data.repository.dictionary.DictionaryStateHolder
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ExportUseCase(private val state: DictionaryStateHolder) {

    suspend fun getExportStringForSelected(selectedIds: Set<String>): String = state.mutex.withLock {
        val entriesToExport = state.allEntries.filter { selectedIds.contains(it.id) }
        val sb = java.lang.StringBuilder()
        for (entry in entriesToExport) {
            val chinese = entry.chinese
            val meanings = entry.meanings.joinToString("/")
            sb.append("$chinese=$meanings\n")
        }
        sb.toString()
    }

    suspend fun exportSelectedEntries(context: Context, uri: Uri, selectedIds: Set<String>): Result<Int> {
        val entriesToExport = state.mutex.withLock {
            state.allEntries.filter { selectedIds.contains(it.id) }
        }

        return try {
            withContext(NonCancellable) {
                val exportedCount = FileHandler.writeLines(context.applicationContext, uri, entriesToExport) { it.toLine() }
                Result.success(exportedCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
