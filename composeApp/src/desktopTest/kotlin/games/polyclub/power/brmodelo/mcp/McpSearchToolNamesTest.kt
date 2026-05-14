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

class McpSearchToolNamesTest {

    @Test
    fun searchToolNamesUseSearchGroupPrefix() {
        // Arrange
        val sep = McpSearchToolNames.SEARCH_SEPARATOR

        // Act
        val find = McpSearchToolNames.FIND
        val apply = McpSearchToolNames.APPLY_HIT

        // Assert
        assertEquals("search", McpSearchToolNames.SEARCH_GROUP)
        assertTrue(find.startsWith("search$sep"))
        assertEquals("search__find", find)
        assertEquals("search__apply_hit", apply)
    }
}
