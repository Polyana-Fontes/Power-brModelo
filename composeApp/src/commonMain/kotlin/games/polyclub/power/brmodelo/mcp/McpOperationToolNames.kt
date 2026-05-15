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

/** MCP tools that mirror editor **Operações** menu commands (desktop server only). */
internal object McpOperationToolNames {
    const val OPERATION_GROUP = "operation"
    const val OPERATION_SEPARATOR = "__"

    private fun operationTool(suffix: String): String = "$OPERATION_GROUP$OPERATION_SEPARATOR$suffix"

    /** Same rules as **Operações → Organizar Atributos**; optional per-side scope for MCP. */
    val ORGANIZE_ATTRIBUTES = operationTool("organize_attributes")

    /** Overlap / tight spacing / approximate link-crossing diagnostics for agents (optional element id scope). */
    val LAYOUT_QUALITY = operationTool("layout_quality")

    /**
     * Translates canvas elements by a delta in schema coordinates; optional expansion moves owned on-canvas
     * attributes with their owners. Cardinality label updates follow the same fixed/floating rules as canvas drag.
     */
    val MOVE_CANVAS_ELEMENTS = operationTool("move_canvas_elements")
}
