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

package games.polyclub.power.brmodelo.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CanvasSelectionTest {

    @Test
    fun `select all elements empty schema is None`() {
        // Arrange
        val schema = ConceptualSchema(nextId = 1)

        // Act
        val sel = canvasSelectionSelectAllElements(schema)

        // Assert
        assertEquals(CanvasSelection.None, sel)
    }

    @Test
    fun `select all elements includes every element id`() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(0, 0, 10, 10))
        val e2 = SchemaElement.Entity(2, "B", ElementPosition(20, 0, 10, 10))
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 2 to e2),
            connections = emptyList(),
            nextId = 3,
        )

        // Act
        val sel = canvasSelectionSelectAllElements(schema)

        // Assert
        val multi = assertIs<CanvasSelection.Multiple>(sel)
        assertEquals(setOf(1, 2), multi.elementIds)
        assertEquals(emptySet(), multi.cardinalityConnectionIds)
    }
}
