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

    @Test
    fun `tryBuildCanvasSelectionFromMcpPickLists builds multiple pick for valid ids`() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(0, 0, 10, 10))
        val e2 = SchemaElement.Entity(2, "B", ElementPosition(20, 0, 10, 10))
        val conn = Connection(id = 10, elementIdA = 1, elementIdB = 2)
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 2 to e2),
            connections = listOf(conn),
            nextId = 11,
        )

        // Act
        val (sel, err) = tryBuildCanvasSelectionFromMcpPickLists(schema, listOf(1), listOf(10))

        // Assert
        assertEquals(null, err)
        val multi = assertIs<CanvasSelection.Multiple>(sel)
        assertEquals(setOf(1), multi.elementIds)
        assertEquals(setOf(10), multi.cardinalityConnectionIds)
    }

    @Test
    fun `tryBuildCanvasSelectionFromMcpPickLists returns error for unknown element id`() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(0, 0, 10, 10))
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1),
            connections = emptyList(),
            nextId = 2,
        )

        // Act
        val (sel, err) = tryBuildCanvasSelectionFromMcpPickLists(schema, listOf(99), emptyList())

        // Assert
        assertEquals(null, sel)
        assertEquals("unknown_element_id:99", err)
    }

    @Test
    fun `merge canvas rectangle replace uses band only`() {
        // Arrange
        val start = CanvasSelection.Multiple(elementIds = setOf(1, 2), cardinalityConnectionIds = setOf(10))

        // Act
        val out = mergeCanvasRectangleSelection(
            CanvasSelectionRectangleMergeMode.REPLACE,
            start,
            bandElementIds = setOf(2, 3),
            bandCardinalityIds = setOf(11),
        )

        // Assert
        val multi = assertIs<CanvasSelection.Multiple>(out)
        assertEquals(setOf(2, 3), multi.elementIds)
        assertEquals(setOf(11), multi.cardinalityConnectionIds)
    }

    @Test
    fun `merge canvas rectangle add unions picks`() {
        // Arrange
        val start = CanvasSelection.Element(1)

        // Act
        val out = mergeCanvasRectangleSelection(
            CanvasSelectionRectangleMergeMode.ADD,
            start,
            bandElementIds = setOf(2),
            bandCardinalityIds = setOf(5),
        )

        // Assert
        val multi = assertIs<CanvasSelection.Multiple>(out)
        assertEquals(setOf(1, 2), multi.elementIds)
        assertEquals(setOf(5), multi.cardinalityConnectionIds)
    }

    @Test
    fun `merge canvas rectangle subtract removes band hits`() {
        // Arrange
        val start = CanvasSelection.Multiple(elementIds = setOf(1, 2, 3), cardinalityConnectionIds = setOf(9, 10))

        // Act
        val out = mergeCanvasRectangleSelection(
            CanvasSelectionRectangleMergeMode.SUBTRACT,
            start,
            bandElementIds = setOf(2),
            bandCardinalityIds = setOf(10, 99),
        )

        // Assert
        val multi = assertIs<CanvasSelection.Multiple>(out)
        assertEquals(setOf(1, 3), multi.elementIds)
        assertEquals(setOf(9), multi.cardinalityConnectionIds)
    }

    @Test
    fun `canvas selection symmetric pick delta lists differing ids`() {
        // Arrange
        val a = CanvasSelection.Multiple(elementIds = setOf(1, 2), cardinalityConnectionIds = setOf(10))
        val b = CanvasSelection.Multiple(elementIds = setOf(2, 3), cardinalityConnectionIds = setOf(11))

        // Act
        val (de, dc) = canvasSelectionSymmetricPickDelta(a, b)

        // Assert
        assertEquals(listOf(1, 3), de)
        assertEquals(listOf(10, 11), dc)
    }
}
