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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class McpResourceUtilityToolNamesTest {

    @Test
    fun `utility tool names use resource_utility double underscore suffix pattern`() {
        // Arrange
        val sep = McpResourceUtilityToolNames.RESOURCE_UTILITY_SEPARATOR

        // Act & Assert
        assertEquals("resource_utility", McpResourceUtilityToolNames.RESOURCE_UTILITY_GROUP)
        assertEquals("__", sep)
        assertTrue(McpResourceUtilityToolNames.READ_FULL.startsWith("resource_utility$sep"))
        assertEquals("resource_utility__read_full", McpResourceUtilityToolNames.READ_FULL)
        assertEquals("resource_utility__read_lines", McpResourceUtilityToolNames.READ_LINES)
        assertEquals("resource_utility__read_range", McpResourceUtilityToolNames.READ_RANGE)
        assertEquals("resource_utility__search", McpResourceUtilityToolNames.SEARCH)
        assertEquals("resource_utility__search_regex", McpResourceUtilityToolNames.SEARCH_REGEX)
    }
}
