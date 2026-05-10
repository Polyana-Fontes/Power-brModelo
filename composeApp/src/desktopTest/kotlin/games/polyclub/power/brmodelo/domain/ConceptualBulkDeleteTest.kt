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
}
