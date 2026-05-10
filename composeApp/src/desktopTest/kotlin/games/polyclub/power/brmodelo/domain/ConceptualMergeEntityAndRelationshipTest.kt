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

class ConceptualMergeEntityAndRelationshipTest {

    @Test
    fun `menu disabled unless exactly one entity and one relationship`() {
        // Arrange
        val e = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 102, 66))
        val r = SchemaElement.Relationship(2, "R", ElementPosition(200, 0, 102, 51))
        val schema = ConceptualSchema(elements = mapOf(1 to e, 2 to r), nextId = 3)

        // Act & Assert
        assertFalse(canMergeEntityAndRelationshipToAssociativeMenu(schema, CanvasSelection.Element(1)))
        assertFalse(
            canMergeEntityAndRelationshipToAssociativeMenu(
                schema,
                CanvasSelection.Multiple(setOf(1, 2, 3), emptySet()),
            ),
        )
        assertTrue(canMergeEntityAndRelationshipToAssociativeMenu(schema, CanvasSelection.Multiple(setOf(1, 2), emptySet())))
    }

    @Test
    fun `merge preserves entity geometry and maps relationship inner fields`() {
        // Arrange
        val entPos = ElementPosition(50, 80, 102, 66)
        val e1 = SchemaElement.Entity(
            id = 1,
            name = "Cliente",
            position = entPos,
            observations = "eo",
            dictionary = "ed",
            labelStyle = LabelStyle(color = 3, bold = true, italic = false),
        )
        val e2 = SchemaElement.Entity(2, "Pedido", ElementPosition(400, 80, 102, 66))
        val rel =
            SchemaElement.Relationship(
                id = 3,
                name = "Faz",
                position = ElementPosition(200, 80, 102, 51),
                observations = "ro",
                dictionary = "rd",
                arrowDirection = ArrowDirection.LEFT_UP,
            )
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 2 to e2, 3 to rel),
            connections = listOf(
                Connection(10, 3, 1, showCardinality = true, orientation = LineOrientation.HORIZONTAL),
                Connection(11, 3, 2, showCardinality = true, orientation = LineOrientation.HORIZONTAL),
            ),
            nextId = 20,
        )
        val sel = CanvasSelection.Multiple(elementIds = setOf(1, 3), cardinalityConnectionIds = emptySet())

        // Act
        val out = applyMergeEntityAndRelationshipToAssociative(schema, sel)

        // Assert
        assertNotNull(out)
        assertNull(out.elements[3])
        val assoc = out.elements[1] as SchemaElement.AssociativeEntity
        assertEquals(entPos, assoc.position)
        assertEquals("Cliente", assoc.name)
        assertEquals("eo", assoc.observations)
        assertEquals("ed", assoc.dictionary)
        assertEquals("Faz", assoc.relationshipName)
        assertEquals("rd", assoc.relationshipDictionary)
        assertEquals("ro", assoc.relationshipObservations)
        assertEquals(ArrowDirection.LEFT_UP, assoc.arrowDirection)
        assertEquals(1, out.connections.size)
        val leg = out.connections.single()
        assertEquals(1, leg.elementIdA)
        assertEquals(2, leg.elementIdB)
    }

    @Test
    fun `merge disabled when cardinality pick present`() {
        // Arrange
        val e = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 102, 66))
        val r = SchemaElement.Relationship(2, "R", ElementPosition(200, 0, 102, 51))
        val schema = ConceptualSchema(
            elements = mapOf(1 to e, 2 to r),
            connections = listOf(Connection(9, 2, 1, showCardinality = false, orientation = LineOrientation.VERTICAL)),
            nextId = 10,
        )

        // Act & Assert
        assertFalse(
            canMergeEntityAndRelationshipToAssociativeMenu(
                schema,
                CanvasSelection.Multiple(setOf(1, 2), setOf(9)),
            ),
        )
    }
}
