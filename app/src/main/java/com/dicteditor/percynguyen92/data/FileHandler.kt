package com.dicteditor.percynguyen92.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

object FileHandler {

    private val writeMutex = Mutex()
    private const val YIELD_BATCH_SIZE = 50_000

    suspend fun readLines(context: Context, uri: Uri): List<DictEntry> = withContext(Dispatchers.IO) {
        val entries = ArrayList<DictEntry>()
        var idCounter = 1

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                var lineCount = 0
                lines.forEach { line ->
                    if (lineCount++ % YIELD_BATCH_SIZE == 0) yield()
                    val entry = DictEntry.parse(line, "entry_$idCounter")
                    if (entry != null) {
                        entries.add(entry)
                        idCounter++
                    }
                }
            }
        }
        entries
    }

    suspend fun writeLines(context: Context, uri: Uri, entries: List<DictEntry>): Int = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val tempFile = java.io.File.createTempFile("dict_save_", ".tmp", context.cacheDir)
            try {
                tempFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                    var writeCount = 0
                    entries.forEach { entry ->
                        if (writeCount++ % YIELD_BATCH_SIZE == 0) yield()
                        writer.write(entry.toLine())
                        writer.newLine()
                    }
                }
                context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    tempFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } finally {
                try { tempFile.delete() } catch (e: Exception) {}
            }
            entries.size
        }
    }
}
