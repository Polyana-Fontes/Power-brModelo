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
import kotlin.test.assertTrue

class ConceptualBulkDeleteTest {

    @Test
    fun expandBulkDeleteClosure_includesOwnedAttributesAndCompositeChildren() {
        // Arrange
        val e = SchemaElement.Entity(id = 1, name = "E", position = ElementPosition(0, 0, 100, 50))
        val a1 = SchemaElement.Attribute(
            id = 2,
            name = "comp",
            position = ElementPosition(10, 60, 40, 20),
            ownerId = 1,
            childAttributeIds = listOf(3),
        )
        val a2 = SchemaElement.Attribute(
            id = 3,
            name = "sub",
            position = ElementPosition(10, 90, 30, 20),
            ownerId = 2,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to e, 2 to a1, 3 to a2),
        )

        // Act
        val expanded = expandBulkDeleteClosure(schema, setOf(1))

        // Assert
        assertEquals(setOf(1, 2, 3), expanded)
    }

    @Test
    fun expandBulkDeleteClosure_doesNotIncludeSelfRelationshipWhenOwnerEntityRemoved() {
        // Arrange
        val e = SchemaElement.Entity(id = 1, name = "E", position = ElementPosition(0, 0, 100, 50))
        val selfRel = SchemaElement.SelfRelationship(
            id = 2,
            name = "R",
            position = ElementPosition(120, 10, 40, 40),
            ownerEntityId = 1,
        )
        val schema = ConceptualSchema(elements = mapOf(1 to e, 2 to selfRel))

        // Act
        val expanded = expandBulkDeleteClosure(schema, setOf(1))

        // Assert
        assertEquals(setOf(1), expanded)
    }

    @Test
    fun bulkDeleteResolvedIds_usesBandAndClosure() {
        // Arrange
        val e = SchemaElement.Entity(id = 1, name = "E", position = ElementPosition(0, 0, 50, 50))
        val attr = SchemaElement.Attribute(
            id = 2,
            name = "a",
            position = ElementPosition(200, 200, 20, 20),
            ownerId = 1,
        )
        val schema = ConceptualSchema(elements = mapOf(1 to e, 2 to attr))
        val band = ConceptualBulkDeleteBand.fromCorners(0f, 0f, 40f, 40f)

        // Act
        val ids = bulkDeleteResolvedIds(schema, band)

        // Assert
        assertEquals(setOf(1, 2), ids)
    }

    @Test
    fun withoutElements_removesAllAndIncidentConnections() {
        // Arrange
        val a = SchemaElement.Entity(id = 1, name = "A", position = ElementPosition(0, 0, 10, 10))
        val b = SchemaElement.Entity(id = 2, name = "B", position = ElementPosition(50, 0, 10, 10))
        val conn = Connection(
            id = 99,
            elementIdA = 1,
            elementIdB = 2,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to a, 2 to b),
            connections = listOf(conn),
        )

        // Act
        val next = schema.withoutElements(setOf(1))

        // Assert
        assertEquals(1, next.elements.size)
        assertTrue(2 in next.elements)
        assertTrue(next.connections.isEmpty())
    }

    @Test
    fun selectedPickCount_matchesSelectionShape() {
        // Assert
        assertEquals(0, CanvasSelection.None.selectedPickCount())
        assertEquals(1, CanvasSelection.Element(3).selectedPickCount())
        assertEquals(1, CanvasSelection.Cardinality(9).selectedPickCount())
        assertEquals(
            4,
            CanvasSelection.Multiple(
                elementIds = setOf(1, 2),
                cardinalityConnectionIds = setOf(7, 8),
            ).selectedPickCount(),
        )
    }

    @Test
    fun mergeCanvasBandPick_additiveUnionsBandWithPriorSelection() {
        // Arrange
        val prior = CanvasSelection.Element(1)

        // Act
        val merged = mergeCanvasBandPick(true, prior, setOf(2, 3), setOf(9))

        // Assert
        val m = merged as CanvasSelection.Multiple
        assertEquals(setOf(1, 2, 3), m.elementIds)
        assertEquals(setOf(9), m.cardinalityConnectionIds)
    }

    @Test
    fun mergeCanvasBandPick_replaceUsesBandOnly() {
        // Arrange
        val prior = CanvasSelection.Element(5)

        // Act
        val merged = mergeCanvasBandPick(false, prior, setOf(2), emptySet())

        // Assert
        val m = merged as CanvasSelection.Multiple
        assertEquals(setOf(2), m.elementIds)
        assertEquals(emptySet<Int>(), m.cardinalityConnectionIds)
    }

    @Test
    fun bulkDeleteCategoryCountsForCanvasSelection_countsSingleElement() {
        // Arrange
        val e = SchemaElement.Entity(id = 1, name = "E", position = ElementPosition(0, 0, 10, 10))
        val schema = ConceptualSchema(elements = mapOf(1 to e))

        // Act
        val c = bulkDeleteCategoryCountsForCanvasSelection(schema, CanvasSelection.Element(1))

        // Assert
        assertEquals(1, c.entities)
        assertEquals(1, c.total)
    }

    @Test
    fun toggleElementInMultiSelection_addsThenRemovesFromSet() {
        // Arrange
        val start = CanvasSelection.Element(1)

        // Act
        val with2 = toggleElementInMultiSelection(start, 2)
        val without1 = toggleElementInMultiSelection(with2, 1)

        // Assert
        val m = without1 as CanvasSelection.Multiple
        assertEquals(setOf(2), m.elementIds)
        assertEquals(emptySet<Int>(), m.cardinalityConnectionIds)
    }

    @Test
    fun toggleElementInMultiSelection_removesSoleElementToNone() {
        // Arrange
        val start = CanvasSelection.Element(7)

        // Act
        val cleared = toggleElementInMultiSelection(start, 7)

        // Assert
        assertEquals(CanvasSelection.None, cleared)
    }

    @Test
    fun toggleCardinalityInMultiSelection_removesSoleCardinalityToNone() {
        // Arrange
        val start = CanvasSelection.Cardinality(99)

        // Act
        val cleared = toggleCardinalityInMultiSelection(start, 99)

        // Assert
        assertEquals(CanvasSelection.None, cleared)
    }

    @Test
    fun canvasSelectionFromPickSets_emptyIsNone() {
        // Act & Assert
        assertEquals(CanvasSelection.None, canvasSelectionFromPickSets(emptySet(), emptySet()))
    }
}
