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
 * MCP tool names for procedural conceptual canvas inserts (same domain rules as ribbon tools,
 * without switching the user's active canvas tool).
 */
internal object McpProceduralToolsToolNames {
    const val TOOLS_GROUP = "tools"
    const val TOOLS_SEPARATOR = "__"

    private fun tool(suffix: String): String = "$TOOLS_GROUP$TOOLS_SEPARATOR$suffix"

    val PLACE_ENTITY = tool("place_entity")
    val PLACE_RELATIONSHIP = tool("place_relationship")
    val PLACE_ASSOCIATIVE_ENTITY = tool("place_associative_entity")

    /** Same rules as the editor **Ligar Objetos** tool (two picks); MCP exposes both ends in one call. */
    val LINK_OBJECTS = tool("link_objects")

    /** Same as ribbon specialization tool (triangle only, optional). */
    val APPLY_SPECIALIZATION_BASIC = tool("apply_specialization_basic")

    /**
     * Specialization branch with automatic child entity; pass `exclusive` (true = restricted / ribbon A, false = optional / ribbon B).
     */
    val APPLY_SPECIALIZATION_TREE = tool("apply_specialization_tree")
}
