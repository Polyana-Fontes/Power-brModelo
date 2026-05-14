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
 * MCP tool names for reading and searching any registered resource body as text.
 */
internal object McpResourceUtilityToolNames {
    const val RESOURCE_UTILITY_GROUP = "resource_utility"
    const val RESOURCE_UTILITY_SEPARATOR = "__"

    private fun utilTool(suffix: String): String =
        "$RESOURCE_UTILITY_GROUP$RESOURCE_UTILITY_SEPARATOR$suffix"

    val READ_FULL = utilTool("read_full")
    val READ_LINES = utilTool("read_lines")
    val READ_RANGE = utilTool("read_range")
    val SEARCH = utilTool("search")
    val SEARCH_REGEX = utilTool("search_regex")
}
