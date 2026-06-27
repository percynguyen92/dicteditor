package com.dicteditor.percynguyen92.data.model

data class DictEntry(
    val id: String,
    val chinese: String,
    val meanings: List<String>
) {
    fun toLine(): String = "$chinese=${meanings.joinToString("/")}"

    companion object {
        fun parse(line: String, id: String? = null): DictEntry? {
            if (line.isBlank()) return null
            
            val eqIndex = line.indexOf('=')
            if (eqIndex == -1) return null

            val chinese = line.substring(0, eqIndex).trim()
            if (chinese.isEmpty()) return null

            val meaningsStr = line.substring(eqIndex + 1).trim()
            
            val finalId = id ?: "entry_temp_${System.nanoTime()}_${(1..1000).random()}"
            return DictEntry(finalId, chinese, parseMeanings(meaningsStr))
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
