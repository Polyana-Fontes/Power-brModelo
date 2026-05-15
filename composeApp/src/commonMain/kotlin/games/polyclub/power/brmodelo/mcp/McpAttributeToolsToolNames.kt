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
 * MCP tool names for conceptual attribute authoring (same rules as canvas / inspector).
 */
internal object McpAttributeToolsToolNames {
    const val ATTRIBUTE_TOOLS_GROUP = "tools"
    const val ATTRIBUTE_TOOLS_SEPARATOR = "__"

    private fun tool(suffix: String): String = "$ATTRIBUTE_TOOLS_GROUP$ATTRIBUTE_TOOLS_SEPARATOR$suffix"

    val APPLY_ATTRIBUTE = tool("apply_attribute")
    val APPLY_COMPOSITE_ATTRIBUTE = tool("apply_composite_attribute")
    val APPLY_HIDDEN_ATTRIBUTE = tool("apply_hidden_attribute")
}
