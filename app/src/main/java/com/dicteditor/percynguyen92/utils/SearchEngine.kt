package com.dicteditor.percynguyen92.utils

import com.dicteditor.percynguyen92.data.model.DictEntry
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

data class ReplaceResult(
    val replacedEntries: Map<String, DictEntry>,  // id -> replaced entry
    val totalReplacements: Int                     // total occurrence count replaced
)

/**
 * Errors surfaced from [SearchEngine] so the UI can show a specific, actionable message
 * instead of a generic failure. Both are only ever produced when [useRegex] is true —
 * literal mode (escaped pattern) can't hit either case.
 */
sealed class SearchEngineError(message: String) : Exception(message) {

    /** The regex pattern failed to compile (unclosed group, bad escape, etc). */
    data class InvalidRegex(val pattern: String, val reason: String?) :
        SearchEngineError("Regex không hợp lệ: ${reason ?: "cú pháp không đúng"}")

    /**
     * The pattern can match an empty string (e.g. "a*", "\\d*", "x?"). Left unchecked,
     * Replace All would insert [replaceText] between every single character instead of
     * at real occurrences. Replace is aborted before touching any entry so this is
     * always raised pre-emptively, never partially applied.
     *
     * Note: detection works by testing whether the pattern matches "" directly, which
     * covers the common cases above. Context-dependent zero-width patterns (lookahead/
     * lookbehind that are only zero-width next to specific characters) aren't caught.
     */
    data class ZeroWidthMatch(val pattern: String) :
        SearchEngineError("Pattern \"$pattern\" có thể khớp với chuỗi rỗng — vui lòng chỉnh lại pattern trước khi Replace")
}

object SearchEngine {

    /**
     * Compiles [pattern] and validates it's safe to use for search/replace.
     * Single validation entry point shared by [filterEntries] and [replaceInEntries] so
     * the two never drift — the error the user sees while typing in the search box is
     * the exact same check that would otherwise gate Replace.
     */
    private fun validatePattern(pattern: String, matchCase: Boolean): Result<Regex> {
        val options = if (!matchCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
        val regex = try {
            Regex(pattern, options)
        } catch (e: Exception) {
            return Result.failure(SearchEngineError.InvalidRegex(pattern, e.message))
        }
        if (regex.containsMatchIn("")) {
            return Result.failure(SearchEngineError.ZeroWidthMatch(pattern))
        }
        return Result.success(regex)
    }

    /**
     * Filters [entries] by [query]. When [useRegex] is true and the pattern is invalid or
     * can zero-width match, returns [Result.failure] with a [SearchEngineError] so the
     * search box can surface the error as the user types — before they ever reach the
     * "Replace with" input.
     *
     * `suspend` + per-entry [ensureActive] so that when the caller cancels the debounced
     * search job (e.g. user keeps typing), the loop stops on the next entry instead of
     * running to completion on a dictionary with 100k+ entries. The check is a cheap
     * volatile read, so it's cheap enough to do every iteration rather than batching it
     * every N entries.
     */
    suspend fun filterEntries(
        query: String,
        entries: List<DictEntry>,
        useRegex: Boolean,
        matchCase: Boolean
    ): Result<List<DictEntry>> {
        if (query.isEmpty()) return Result.success(entries)
        val q = query.trim()
        if (q.isEmpty()) return Result.success(entries)
        val ignoreCase = !matchCase

        val regex = if (useRegex) {
            validatePattern(q, matchCase).getOrElse { e -> return Result.failure(e) }
        } else null

        val matches: (DictEntry) -> Boolean = { entry ->
            if (useRegex && regex != null) {
                regex.containsMatchIn(entry.chinese) || entry.meanings.any { m -> regex.containsMatchIn(m) }
            } else {
                entry.chinese.contains(q, ignoreCase = ignoreCase) ||
                        entry.meanings.any { m -> m.contains(q, ignoreCase = ignoreCase) }
            }
        }

        val result = ArrayList<DictEntry>()
        for (entry in entries) {
            coroutineContext.ensureActive()
            if (matches(entry)) result.add(entry)
        }
        return Result.success(result)
    }

    /**
     * Replaces [findText] with [replaceText] across all entries.
     *
     * Literal mode is implemented as an escaped regex so both modes share one code path
     * (previously the literal and regex branches were duplicated for `chinese` and for
     * each `meaning`, which made the two easy to accidentally drift apart).
     *
     * [findText] is trimmed before matching, mirroring [filterEntries], so Find and
     * Replace behave consistently for the same input.
     *
     * Validation reuses the exact same [validatePattern] as [filterEntries]: by the time
     * the user reaches this step the search box should have already surfaced any
     * [SearchEngineError], so failing here only happens if Replace is invoked without
     * going through Search first.
     *
     * `suspend` + per-entry [ensureActive], same reasoning as [filterEntries]: lets the
     * caller cancel mid-run (e.g. user navigates away while a 50k-entry Replace All is
     * still in progress) instead of burning CPU on a result nobody will see.
     */
    suspend fun replaceInEntries(
        findText: String,
        replaceText: String,
        entries: List<DictEntry>,
        useRegex: Boolean,
        matchCase: Boolean
    ): Result<ReplaceResult> {
        val find = findText.trim()
        if (find.isEmpty()) return Result.success(ReplaceResult(emptyMap(), 0))

        val pattern = if (useRegex) find else Regex.escape(find)
        val regex = validatePattern(pattern, matchCase).getOrElse { e -> return Result.failure(e) }

        var totalReplacements = 0
        val replacedEntries = mutableMapOf<String, DictEntry>()

        for (entry in entries) {
            coroutineContext.ensureActive()
            var entryChanged = false

            val (newChinese, chineseCount) = applyReplace(entry.chinese, regex, replaceText)
            if (chineseCount > 0) {
                entryChanged = true
                totalReplacements += chineseCount
            }

            var meaningsChanged = false
            val newMeanings = entry.meanings.map { meaning ->
                val (newMeaning, count) = applyReplace(meaning, regex, replaceText)
                if (count > 0) {
                    meaningsChanged = true
                    totalReplacements += count
                }
                newMeaning
            }
            if (meaningsChanged) entryChanged = true

            if (entryChanged) {
                replacedEntries[entry.id] = entry.copy(chinese = newChinese, meanings = newMeanings)
            }
        }

        return Result.success(ReplaceResult(replacedEntries, totalReplacements))
    }

    /**
     * Applies [regex] replacement to [text] in a single pass, returning the new text and
     * the number of occurrences replaced. Assumes [regex] cannot zero-width match — callers
     * must guard for that (see [SearchEngineError.ZeroWidthMatch]) before reaching here.
     *
     * Iterates the [Regex.findAll] Sequence directly instead of buffering it into a List —
     * at dictionary scale (tens of thousands of entries × multiple fields each) this avoids
     * one extra small allocation per string, since `findAll` already lazily produces one
     * MatchResult at a time.
     */
    private fun applyReplace(text: String, regex: Regex, replacement: String): Pair<String, Int> {
        val sb = StringBuilder(text.length)
        var lastEnd = 0
        var count = 0
        for (match in regex.findAll(text)) {
            sb.append(text, lastEnd, match.range.first)
            sb.append(replacement)
            lastEnd = match.range.last + 1
            count++
        }
        if (count == 0) return text to 0
        sb.append(text, lastEnd, text.length)
        return sb.toString() to count
    }
}