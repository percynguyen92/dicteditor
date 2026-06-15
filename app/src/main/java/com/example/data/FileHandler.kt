package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.nio.charset.StandardCharsets

object FileHandler {

    suspend fun readLines(context: Context, uri: Uri): List<DictEntry> = withContext(Dispatchers.IO) {
        val entries = ArrayList<DictEntry>()
        var idCounter = 1

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
                lines.forEach { line ->
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
        context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
            outputStream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                entries.forEach { entry ->
                    writer.write(entry.toLine())
                    writer.newLine()
                }
            }
        }
        entries.size
    }
}
