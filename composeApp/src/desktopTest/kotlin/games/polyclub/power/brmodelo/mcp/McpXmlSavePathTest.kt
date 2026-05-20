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

import games.polyclub.power.brmodelo.domain.ConceptualSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class McpXmlSavePathTest {

    @Test
    fun normalizeAbsoluteXmlSavePath_appendsXmlExtension() {
        // Arrange
        val input = "/tmp/meu-modelo"

        // Act
        val path = normalizeAbsoluteXmlSavePath(input)

        // Assert
        assertEquals("/tmp/meu-modelo.xml", path)
    }

    @Test
    fun normalizeAbsoluteXmlSavePath_rejectsRelativePath() {
        // Act
        val path = normalizeAbsoluteXmlSavePath("modelo.xml")

        // Assert
        assertNull(path)
    }

    @Test
    fun knownConceptualSchemaDiskPath_returnsNullWhenBlankOrBrm() {
        // Arrange
        val blank = ConceptualSchema(name = "x", filePath = "")
        val brm = ConceptualSchema(name = "y", filePath = "/a.xml", openedFromBrm = true)

        // Act & Assert
        assertNull(knownConceptualSchemaDiskPath(blank))
        assertNull(knownConceptualSchemaDiskPath(brm))
    }

    @Test
    fun knownConceptualSchemaDiskPath_returnsTrimmedPath() {
        // Arrange
        val schema = ConceptualSchema(name = "z", filePath = "  /data/z.xml  ")

        // Act
        val path = knownConceptualSchemaDiskPath(schema)

        // Assert
        assertEquals("/data/z.xml", path)
    }
}
