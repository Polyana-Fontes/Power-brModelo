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
 * Result of applying a procedural conceptual tool on the UI thread (MCP handler interprets).
 */
internal data class McpProceduralToolApplyOutcome(
    val error: String?,
    val tabIndex: Int = -1,
    /** JSON object for the placed element (starts with `{`), unless [isFullResponseJson] is true. */
    val elementJson: String? = null,
    /** When true, [elementJson] is the entire tool success JSON body (including `ok` and `tabIndex`). */
    val isFullResponseJson: Boolean = false,
) {
    companion object {
        fun ok(tabIndex: Int, elementJson: String): McpProceduralToolApplyOutcome =
            McpProceduralToolApplyOutcome(null, tabIndex, elementJson, isFullResponseJson = false)

        fun okFullJson(bodyJson: String): McpProceduralToolApplyOutcome =
            McpProceduralToolApplyOutcome(null, -1, bodyJson, isFullResponseJson = true)

        fun err(code: String): McpProceduralToolApplyOutcome =
            McpProceduralToolApplyOutcome(code, -1, null)
    }
}
