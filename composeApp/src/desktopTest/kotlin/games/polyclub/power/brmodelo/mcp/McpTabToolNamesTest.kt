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

class McpTabToolNamesTest {

    @Test
    fun `tool names use category double underscore suffix pattern`() {
        // Arrange
        val sep = McpTabToolNames.TAB_TOOL_SEPARATOR

        // Act & Assert
        assertEquals("tabs", McpTabToolNames.TAB_TOOL_CATEGORY)
        assertEquals("__", sep)
        assertTrue(McpTabToolNames.LIST_OPEN.startsWith("tabs$sep"))
        assertEquals("tabs__list_open", McpTabToolNames.LIST_OPEN)
        assertEquals("tabs__select_resource", McpTabToolNames.SELECT_RESOURCE)
        assertEquals("tabs__open_xml", McpTabToolNames.OPEN_XML)
        assertEquals("tabs__save_resource", McpTabToolNames.SAVE_RESOURCE)
    }
}
