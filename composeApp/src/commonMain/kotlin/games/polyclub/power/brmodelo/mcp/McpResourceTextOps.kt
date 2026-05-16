/*
 * Power brModelo - Kotlin port of brModelo 3.0 originally written in Pascal
 * Copyright (C) 2026  Polyana Fontes
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package games.polyclub.power.brmodelo.mcp

/** 1-based line and column (column counts characters on the line, including the indexed character). */
internal data class McpTextLineColumn(val line1: Int, val column1: Int)

/**
 * A text span with UTF-16 indices (same as Kotlin [String] indices) and 1-based line/column for agents.
 *
 * @param startIndex inclusive character index in [text]
 * @param endIndexExclusive exclusive end index (Kotlin [String.substring] style)
 */
internal data class McpTextMatchSpan(
    val startIndex: Int,
    val endIndexExclusive: Int,
    val startLine1: Int,
    val startColumn1: Int,
    val endLine1: Int,
    val endColumn1: Int,
    val match: String,
)

internal object McpResourceTextOps {

    /** Converts a 0-based character index to 1-based line/column (newline = `\n` only). */
    fun indexToLineColumn1(text: String, index: Int): McpTextLineColumn {
        if (index <= 0) return McpTextLineColumn(1, 1)
        val capped = minOf(index, text.length)
        var line = 1
        var lineStart = 0
        var i = 0
        while (i < capped) {
            if (text[i] == '\n') {
                line++
                lineStart = i + 1
            }
            i++
        }
        val col = capped - lineStart + 1
        return McpTextLineColumn(line, maxOf(1, col))
    }

    /**
     * Returns lines [startLine1] through [endLine1] inclusive (1-based), joined with `\n`.
     * Empty lines are preserved; a final trailing newline does not add an extra empty line beyond [String.lines].
     */
    fun sliceLines1Based(text: String, startLine1: Int, endLine1: Int): Pair<String?, String?> {
        if (startLine1 < 1 || endLine1 < 1) return null to "line_numbers_must_be_positive"
        if (startLine1 > endLine1) return null to "start_line_after_end_line"
        val lines = text.lines()
        if (startLine1 > lines.size) {
            return null to "start_line_past_eof"
        }
        val to = minOf(endLine1, lines.size)
        val slice = lines.subList(startLine1 - 1, to).joinToString("\n")
        return slice to null
    }

    /**
     * [startIndex] inclusive, [endIndexExclusive] exclusive (same as [String.substring]).
     */
    fun sliceByCharRange(text: String, startIndex: Int, endIndexExclusive: Int): Pair<String?, String?> {
        if (startIndex < 0 || endIndexExclusive < 0) return null to "indices_must_be_non_negative"
        if (startIndex > endIndexExclusive) return null to "start_index_after_end"
        if (endIndexExclusive > text.length) return null to "end_index_past_eof"
        return text.substring(startIndex, endIndexExclusive) to null
    }

    /**
     * Non-overlapping literal search (next match starts after the end of the previous match).
     */
    fun findAllLiteral(text: String, query: String): Pair<List<McpTextMatchSpan>, String?> {
        if (query.isEmpty()) return emptyList<McpTextMatchSpan>() to "query_must_not_be_empty"
        val out = mutableListOf<McpTextMatchSpan>()
        var i = 0
        while (i <= text.length - query.length) {
            val idx = text.indexOf(query, i)
            if (idx < 0) break
            val end = idx + query.length
            out.add(spanForRange(text, idx, end, text.substring(idx, end)))
            i = end
        }
        return out to null
    }

    /**
     * Regex search; uses [RegexOption.MULTILINE] so `^` / `$` match line starts/ends.
     *
     * [dotMatchesAll] is implemented with a leading `(?s)` inline flag instead of
     * [RegexOption.DOT_MATCHES_ALL], which is not available in Kotlin common metadata (KMP).
     */
    fun findAllRegex(text: String, pattern: String, dotMatchesAll: Boolean): Pair<List<McpTextMatchSpan>, String?> {
        if (pattern.isEmpty()) return emptyList<McpTextMatchSpan>() to "pattern_must_not_be_empty"
        val effectivePattern = if (dotMatchesAll) "(?s)$pattern" else pattern
        val opts = setOf(RegexOption.MULTILINE)
        val regex = try {
            Regex(effectivePattern, opts)
        } catch (e: Exception) {
            return emptyList<McpTextMatchSpan>() to "invalid_regex:${e.message ?: e::class.simpleName}"
        }
        val out = mutableListOf<McpTextMatchSpan>()
        for (m in regex.findAll(text)) {
            val range = m.range
            val start = range.first
            val end = range.last + 1
            out.add(spanForRange(text, start, end, m.value))
        }
        return out to null
    }

    private fun spanForRange(text: String, start: Int, endExclusive: Int, match: String): McpTextMatchSpan {
        val s = indexToLineColumn1(text, start)
        val lastChar = maxOf(start, endExclusive - 1)
        val e = indexToLineColumn1(text, lastChar)
        return McpTextMatchSpan(
            startIndex = start,
            endIndexExclusive = endExclusive,
            startLine1 = s.line1,
            startColumn1 = s.column1,
            endLine1 = e.line1,
            endColumn1 = e.column1,
            match = match,
        )
    }
}
