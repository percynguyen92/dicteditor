package com.example.data

data class DictEntry(
    val id: String,
    val chinese: String,
    val meanings: List<String>
) {
    fun toLine(): String {
        return "$chinese=${meanings.joinToString("/")}"
    }

    companion object {
        fun parse(line: String, id: String): DictEntry? {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return null
            val eqIndex = trimmed.indexOf('=')
            if (eqIndex == -1) return null // Skip invalid format lines gracefully without crashing

            val chinese = trimmed.substring(0, eqIndex).trim()
            if (chinese.isEmpty()) return null

            val meaningsStr = trimmed.substring(eqIndex + 1).trim()
            val meanings = if (meaningsStr.isEmpty()) {
                emptyList()
            } else {
                meaningsStr.split("/").map { it.trim() }.filter { it.isNotEmpty() }
            }

            return DictEntry(id, chinese, meanings)
        }
    }
}
