package com.dicteditor.percynguyen92.utils

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HanVietDictionary {
    private val dict = mutableMapOf<Char, String>()
    private var isLoaded = false

    suspend fun load(context: Context) {
        if (isLoaded) return
        withContext(Dispatchers.IO) {
            try {
                // Try reading from assets
                val inputStream = context.assets.open("HanViet.txt")
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.forEachLine { line ->
                        val parts = line.split("=")
                        if (parts.size == 2) {
                            val char = parts[0].firstOrNull()
                            val pinyin = parts[1]
                            if (char != null) {
                                dict[char] = pinyin
                            }
                        }
                    }
                }
                isLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun translate(chinese: String): String {
        return chinese.map { char ->
            dict[char] ?: char.toString()
        }.joinToString(" ").replace(Regex("\\s+"), " ").trim()
    }
}
