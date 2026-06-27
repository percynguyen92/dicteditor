package com.dicteditor.percynguyen92.data.local

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

    suspend fun <T> readLines(
        context: Context,
        uri: Uri,
        transform: (line: String, lineNumber: Int) -> T?
    ): List<T> = withContext(Dispatchers.IO) {
        val entries = ArrayList<T>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                var lineCount = 0
                lines.forEach { line ->
                    lineCount++
                    if (lineCount % YIELD_BATCH_SIZE == 0) yield()
                    val entry = transform(line, lineCount)
                    if (entry != null) {
                        entries.add(entry)
                    }
                }
            }
        }
        entries
    }

    suspend fun <T> writeLines(
        context: Context,
        uri: Uri,
        items: List<T>,
        transform: (T) -> String
    ): Int = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val tempFile = java.io.File.createTempFile("dict_save_", ".tmp", context.cacheDir)
            try {
                tempFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                    var writeCount = 0
                    items.forEach { item ->
                        if (writeCount++ % YIELD_BATCH_SIZE == 0) yield()
                        writer.write(transform(item))
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
            items.size
        }
    }
}
