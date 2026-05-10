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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConceptualPromoteAttributeToEntityTest {

    @Test
    fun `menu disabled for entity selection`() {
        // Arrange
        val e = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 102, 66))
        val schema = ConceptualSchema(elements = mapOf(1 to e), nextId = 2)

        // Act & Assert
        assertFalse(canPromoteAttributeToEntityMenu(schema, CanvasSelection.Element(1)))
    }

    @Test
    fun `menu disabled when attribute owner is another attribute`() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 102, 66))
        val parentAttr = SchemaElement.Attribute(2, "P", ElementPosition(200, 30, 73, 16), ownerId = 1)
        val childAttr = SchemaElement.Attribute(3, "C", ElementPosition(220, 50, 73, 16), ownerId = 2)
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to parentAttr, 3 to childAttr),
            connections = listOf(
                Connection(1, 2, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(2, 3, 2, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 4,
        )

        // Act & Assert
        assertFalse(canPromoteAttributeToEntityMenu(schema, CanvasSelection.Element(3)))
    }

    @Test
    fun `menu enabled for root attribute on entity`() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 102, 66))
        val a = SchemaElement.Attribute(2, "NomeAttr", ElementPosition(200, 30, 73, 16), ownerId = 1)
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to a),
            connections = listOf(Connection(1, 2, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL)),
            nextId = 3,
        )

        // Act & Assert
        assertTrue(canPromoteAttributeToEntityMenu(schema, CanvasSelection.Element(2)))
    }

    @Test
    fun `promote from entity creates relationship and links both entities`() {
        // Arrange
        val ent = SchemaElement.Entity(1, "Cliente", ElementPosition(100, 100, 102, 66))
        val a = SchemaElement.Attribute(
            id = 2,
            name = "Pedido",
            position = ElementPosition(250, 120, 73, 16),
            ownerId = 1,
            observations = "obs",
            dictionary = "dic",
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to a),
            connections = listOf(Connection(10, 2, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL)),
            nextId = 11,
        )
        val sel = CanvasSelection.Element(2)

        // Act
        val out = applyPromoteAttributeToEntity(schema, sel)

        // Assert
        assertNotNull(out)
        assertNull(out.elements[2])
        val newEnt = out.entities.single { it.name == "Pedido" }
        assertEquals("obs", newEnt.observations)
        assertEquals("dic", newEnt.dictionary)
        val rel = out.relationships.single { it.name.startsWith("Relacao") }
        val legsToRel = out.connections.filter { it.elementIdA == rel.id }
        assertEquals(2, legsToRel.size)
        assertEquals(setOf(1, newEnt.id), legsToRel.map { it.elementIdB }.toSet())
    }

    @Test
    fun `promote composite moves children to new entity`() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(100, 100, 102, 66))
        val child = SchemaElement.Attribute(3, "Filho", ElementPosition(220, 90, 73, 16), ownerId = 2)
        val parentAttr = SchemaElement.Attribute(
            id = 2,
            name = "Composto",
            position = ElementPosition(200, 120, 73, 16),
            ownerId = 1,
            childAttributeIds = listOf(3),
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to parentAttr, 3 to child),
            connections = listOf(
                Connection(10, 2, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(11, 3, 2, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 12,
        )

        // Act
        val out = applyPromoteAttributeToEntity(schema, CanvasSelection.Element(2))

        // Assert
        assertNotNull(out)
        assertNull(out.elements[2])
        val newEnt = out.entities.single { it.name == "Composto" }
        val moved = out.elements[3] as SchemaElement.Attribute
        assertEquals(newEnt.id, moved.ownerId)
        assertTrue(out.connections.any { it.elementIdA == 3 && it.elementIdB == newEnt.id })
    }

    @Test
    fun `promote from existing relationship reuses hub`() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(0, 100, 102, 66))
        val e2 = SchemaElement.Entity(2, "B", ElementPosition(400, 100, 102, 66))
        val rel = SchemaElement.Relationship(3, "R1", ElementPosition(200, 100, 102, 51))
        val attr = SchemaElement.Attribute(4, "X", ElementPosition(200, 30, 73, 16), ownerId = 3)
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 2 to e2, 3 to rel, 4 to attr),
            connections = listOf(
                Connection(10, 3, 1, null, showCardinality = true, orientation = LineOrientation.HORIZONTAL),
                Connection(11, 3, 2, null, showCardinality = true, orientation = LineOrientation.HORIZONTAL),
                Connection(12, 4, 3, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 20,
        )

        // Act
        val out = applyPromoteAttributeToEntity(schema, CanvasSelection.Element(4))

        // Assert
        assertNotNull(out)
        assertNull(out.elements[4])
        assertNotNull(out.elements[3] as? SchemaElement.Relationship)
        val newE = out.entities.single { it.name == "X" }
        val extra = out.connections.filter { it.elementIdA == 3 && it.elementIdB == newE.id }
        assertEquals(1, extra.size)
    }
}
