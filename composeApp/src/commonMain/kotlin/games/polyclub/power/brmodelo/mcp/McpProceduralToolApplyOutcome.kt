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

/**
 * When set, the MCP runtime merges a `layoutQuality` JSON object into the tool result after success,
 * using the tab schema read on the UI thread. [touchedElementIds] null means the whole diagram is analyzed; otherwise
 * only overlaps, tight clearances, and crossings that involve at least one listed element id are reported.
 */
internal data class McpProceduralToolLayoutQualityScan(
    val tabIndex: Int,
    val touchedElementIds: Set<Int>?,
)

/**
 * Result of applying a procedural conceptual tool on the UI thread (MCP handler interprets).
 */
internal data class McpProceduralToolApplyOutcome(
    val error: String?,
    val tabIndex: Int = -1,
    /** JSON object for the placed element (starts with `{`), unless [isFullResponseJson] is true. */
    val elementJson: String? = null,
    /** When true, [elementJson] is the entire tool success JSON body (including `ok` and usually `resourceUri`). */
    val isFullResponseJson: Boolean = false,
    val layoutQualityScan: McpProceduralToolLayoutQualityScan? = null,
) {
    companion object {
        fun ok(
            tabIndex: Int,
            elementJson: String,
            layoutQualityScan: McpProceduralToolLayoutQualityScan? = null,
        ): McpProceduralToolApplyOutcome =
            McpProceduralToolApplyOutcome(
                null,
                tabIndex,
                elementJson,
                isFullResponseJson = false,
                layoutQualityScan = layoutQualityScan,
            )

        fun okFullJson(
            bodyJson: String,
            layoutQualityScan: McpProceduralToolLayoutQualityScan? = null,
        ): McpProceduralToolApplyOutcome =
            McpProceduralToolApplyOutcome(
                null,
                -1,
                bodyJson,
                isFullResponseJson = true,
                layoutQualityScan = layoutQualityScan,
            )

        fun err(code: String): McpProceduralToolApplyOutcome =
            McpProceduralToolApplyOutcome(code, -1, null)
    }
}
