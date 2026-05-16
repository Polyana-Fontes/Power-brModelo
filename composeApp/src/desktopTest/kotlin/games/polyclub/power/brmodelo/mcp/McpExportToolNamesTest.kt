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

class McpExportToolNamesTest {

    @Test
    fun `export tool names use export__ prefix`() {
        // Arrange
        // (constants under test)

        // Act & Assert
        assertEquals("export", McpExportToolNames.EXPORT_GROUP)
        assertEquals("__", McpExportToolNames.EXPORT_SEPARATOR)
        assertEquals("export__subset_raster", McpExportToolNames.SUBSET_RASTER)
        assertEquals("export__current_canvas_selection", McpExportToolNames.CURRENT_CANVAS_SELECTION)
        assertEquals("export__canvas_elements_detail", McpExportToolNames.CANVAS_ELEMENTS_DETAIL)
    }
}
