package com.dicteditor.percynguyen92.data

object SearchEngine {
    fun filterEntries(
        query: String,
        entries: List<DictEntry>,
        useRegex: Boolean,
        matchCase: Boolean
    ): List<DictEntry> {
        if (query.isEmpty()) return entries
        val q = query.trim()
        val ignoreCase = !matchCase

        val regex = if (useRegex) {
            try {
                val options = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
                Regex(q, options)
            } catch (e: Exception) {
                null
            }
        } else null

        return entries.filter { entry ->
            if (useRegex && regex != null) {
                regex.containsMatchIn(entry.chinese) ||
                        entry.meanings.any { m -> regex.containsMatchIn(m) }
            } else if (useRegex && regex == null) {
                // If regex compilation fails, fallback to simple plain text search as original behavior
                entry.chinese.contains(q, ignoreCase = ignoreCase) ||
                        entry.meanings.any { m -> m.contains(q, ignoreCase = ignoreCase) }
            } else {
                entry.chinese.contains(q, ignoreCase = ignoreCase) ||
                        entry.meanings.any { m -> m.contains(q, ignoreCase = ignoreCase) }
            }
        }
    }
}
