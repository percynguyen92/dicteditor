package com.dicteditor.percynguyen92.data.model

import com.dicteditor.percynguyen92.R

sealed interface ParseResult {
    data class Success(val entry: DictEntry) : ParseResult
    data class Failure(val error: ParseError) : ParseResult
}

enum class ParseError(val resId: Int) {
    EMPTY_LINE(R.string.error_parse_empty_line),
    MISSING_DELIMITER(R.string.error_parse_missing_delimiter),
    EMPTY_KEY(R.string.error_parse_empty_key)
}

data class InvalidLine(
    val id: String,
    val lineNumber: Int,
    val lineContent: String,
    val error: ParseError
)

data class DictEntry(
    val id: String,
    val chinese: String,
    val meanings: List<String>
) {
    fun toLine(): String = "$chinese=${meanings.joinToString("/")}"

    companion object {
        fun parse(line: String, id: String? = null): ParseResult {
            if (line.isBlank()) return ParseResult.Failure(ParseError.EMPTY_LINE)
            
            val eqIndex = line.indexOf('=')
            if (eqIndex == -1) return ParseResult.Failure(ParseError.MISSING_DELIMITER)

            val chinese = line.substring(0, eqIndex).trim()
            if (chinese.isEmpty()) return ParseResult.Failure(ParseError.EMPTY_KEY)

            val meaningsStr = line.substring(eqIndex + 1).trim()
            
            val finalId = id ?: "entry_temp_${System.nanoTime()}_${(1..1000).random()}"
            return ParseResult.Success(DictEntry(finalId, chinese, parseMeanings(meaningsStr)))
        }

        private fun parseMeanings(meaningsStr: String): List<String> {
            if (meaningsStr.isEmpty()) return emptyList()
            val result = ArrayList<String>()
            var start = 0
            while (true) {
                val nextSlash = meaningsStr.indexOf('/', start)
                val part = if (nextSlash == -1) {
                    meaningsStr.substring(start)
                } else {
                    meaningsStr.substring(start, nextSlash)
                }
                val trimmed = part.trim()
                if (trimmed.isNotEmpty()) {
                    result.add(trimmed)
                }
                if (nextSlash == -1) break
                start = nextSlash + 1
            }
            return result
        }
    }
}
