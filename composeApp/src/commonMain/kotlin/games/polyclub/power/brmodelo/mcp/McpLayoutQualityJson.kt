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

import games.polyclub.power.brmodelo.domain.CONCEPTUAL_LAYOUT_TIGHT_CLEARANCE_MAX_PX
import games.polyclub.power.brmodelo.domain.ConceptualLayoutQualityReport
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.conceptualLayoutAgentSignals

internal object McpLayoutQualityJson {

    private fun jsonString(s: String): String {
        val escaped = buildString(s.length + 8) {
            append('"')
            for (ch in s) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (ch.code < 32) {
                        append("\\u")
                        append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        append(ch)
                    }
                }
            }
            append('"')
        }
        return escaped
    }

    fun layoutQualityObjectJson(report: ConceptualLayoutQualityReport, schema: ConceptualSchema? = null): String {
        val signals = conceptualLayoutAgentSignals(report, schema)
        val affectedJson = signals.affectedElementIds.joinToString(separator = ",")
        val agentHintJson = signals.agentHint?.let { jsonString(it) } ?: "null"
        val overlaps = report.overlaps.joinToString(separator = ",") { p ->
            """{"elementIdA":${p.elementIdA},"elementIdB":${p.elementIdB}}"""
        }
        val tight = report.tightClearances.joinToString(separator = ",") { p ->
            """{"elementIdA":${p.elementIdA},"elementIdB":${p.elementIdB},"gapPx":${p.gapPx}}"""
        }
        val crossings = report.lineCrossings.joinToString(separator = ",") { c ->
            """{"connectionIdA":${c.connectionIdA},"connectionIdB":${c.connectionIdB}}"""
        }
        val note =
            "Link crossings use straight center-to-center segments between endpoints; the editor may route polylines " +
                "differently. Use tab PNG/JPEG resources or export subset raster for visual confirmation."
        return buildString {
            append('{')
            append(""""approximateConnectionRoutingNote":${jsonString(note)},""")
            append(""""tightClearanceThresholdPx":$CONCEPTUAL_LAYOUT_TIGHT_CLEARANCE_MAX_PX,""")
            append(""""hasAnyIssue":${report.hasAnyIssue},""")
            append(""""hasBlockingOverlap":${signals.hasBlockingOverlap},""")
            append(""""affectedElementIds":[$affectedJson],""")
            append(""""agentHint":$agentHintJson,""")
            append(""""overlaps":[$overlaps],""")
            append(""""tightClearances":[$tight],""")
            append(""""lineCrossings":[$crossings]""")
            append('}')
        }
    }

    /**
     * Inserts a `layoutQuality` field immediately before the closing brace of the root JSON object.
     * Handles nested objects/arrays by tracking brace depth outside of string literals.
     */
    fun mergeLayoutQualityIntoJsonObjectBody(
        body: String,
        report: ConceptualLayoutQualityReport,
        schema: ConceptualSchema? = null,
    ): String {
        val insertAt = indexOfRootClosingBrace(body)
        val inner = layoutQualityObjectJson(report, schema)
        return buildString {
            append(body, 0, insertAt)
            append(',')
            append("\"layoutQuality\":")
            append(inner)
            append(body, insertAt, body.length)
        }
    }

    private fun indexOfRootClosingBrace(body: String): Int {
        require(body.startsWith('{')) { "expected JSON object" }
        var depth = 0
        var i = 0
        var inString = false
        var escaped = false
        while (i < body.length) {
            val c = body[i]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    inString = false
                }
                i++
                continue
            }
            when (c) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
            i++
        }
        error("unbalanced JSON braces")
    }

    fun layoutQualityInspectToolSuccessJson(
        resourceUri: String,
        elementIdsScope: Set<Int>?,
        report: ConceptualLayoutQualityReport,
        schema: ConceptualSchema? = null,
    ): String {
        val scopeJson = when {
            elementIdsScope == null -> "null"
            elementIdsScope.isEmpty() -> "null"
            else -> elementIdsScope.sorted().joinToString(separator = ",", prefix = "[", postfix = "]")
        }
        return buildString {
            append("""{"ok":true,"resourceUri":${jsonString(resourceUri)},"elementIdsScope":$scopeJson,"layoutQuality":""")
            append(layoutQualityObjectJson(report, schema))
            append('}')
        }
    }
}
