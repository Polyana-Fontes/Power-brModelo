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

class McpEditToolNamesTest {

    @Test
    fun `edit tool names use edit__ prefix`() {
        // Arrange
        // (constants under test)

        // Act & Assert
        assertEquals("edit__model", McpEditToolNames.MODEL)
        assertEquals("edit__canvas_element", McpEditToolNames.CANVAS_ELEMENT)
        assertEquals("edit__connection", McpEditToolNames.CONNECTION)
        assertEquals("edit__hidden_attribute", McpEditToolNames.HIDDEN_ATTRIBUTE)
        assertEquals("edit__canvas_selection", McpEditToolNames.CANVAS_SELECTION)
        assertEquals("edit__canvas_selection_rectangle", McpEditToolNames.CANVAS_SELECTION_RECTANGLE)
    }
}
