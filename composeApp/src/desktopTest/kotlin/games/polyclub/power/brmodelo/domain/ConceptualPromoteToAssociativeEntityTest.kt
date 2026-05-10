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
import kotlin.test.assertTrue

class ConceptualPromoteToAssociativeEntityTest {

    @Test
    fun `canPromote false without relationship in selection`() {
        // Arrange
        val e = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 102, 66))
        val schema = ConceptualSchema(elements = mapOf(1 to e), nextId = 2)
        val sel = CanvasSelection.Element(1)

        // Act & Assert
        assertFalse(canPromoteToAssociativeEntityMenu(schema, sel))
        assertTrue(relationshipIdsSelectedForPromote(schema, sel).isEmpty())
    }

    @Test
    fun `canPromote true when relationship element is selected`() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "E1", ElementPosition(0, 0, 102, 66))
        val e2 = SchemaElement.Entity(2, "E2", ElementPosition(200, 0, 102, 66))
        val rel = SchemaElement.Relationship(
            id = 3,
            name = "Relacao1",
            position = ElementPosition(100, 50, 40, 40),
            observations = "obs",
            dictionary = "dic",
            arrowDirection = ArrowDirection.RIGHT_UP,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 2 to e2, 3 to rel),
            connections = listOf(
                Connection(1, 1, 3, null, showCardinality = true, orientation = LineOrientation.VERTICAL),
                Connection(2, 2, 3, null, showCardinality = true, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 4,
        )
        val sel = CanvasSelection.Element(3)

        // Act & Assert
        assertTrue(canPromoteToAssociativeEntityMenu(schema, sel))
        assertEquals(setOf(3), relationshipIdsSelectedForPromote(schema, sel))
    }

    @Test
    fun `promote keeps connection endpoints and inner relationship metadata`() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "E1", ElementPosition(0, 0, 102, 66))
        val e2 = SchemaElement.Entity(2, "E2", ElementPosition(200, 0, 102, 66))
        val rel = SchemaElement.Relationship(
            id = 3,
            name = "Relacao1",
            position = ElementPosition(100, 50, 40, 40),
            observations = "obs-rel",
            dictionary = "dic-rel",
            labelStyle = LabelStyle(color = 5, bold = true, italic = false),
            arrowDirection = ArrowDirection.LEFT_DOWN,
            showName = false,
        )
        val beforeConnections = listOf(
            Connection(
                id = 10,
                elementIdA = 1,
                elementIdB = 3,
                showCardinality = true,
                orientation = LineOrientation.VERTICAL,
                cardinalityPosition = ElementPosition(1, 2, 30, 20),
            ),
            Connection(
                id = 11,
                elementIdA = 2,
                elementIdB = 3,
                showCardinality = false,
                orientation = LineOrientation.HORIZONTAL,
            ),
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 2 to e2, 3 to rel),
            connections = beforeConnections,
            nextId = 12,
        )
        val sel = CanvasSelection.Element(3)

        // Act
        val out = applyPromoteRelationshipsToAssociativeEntities(schema, sel)

        // Assert
        assertNotNull(out)
        assertEquals(beforeConnections, out.connections)
        val assoc = out.elements[3] as SchemaElement.AssociativeEntity
        assertEquals("EntAssoc1", assoc.name)
        assertEquals(rel.name, assoc.relationshipName)
        assertEquals(rel.dictionary, assoc.relationshipDictionary)
        assertEquals(rel.observations, assoc.relationshipObservations)
        assertEquals(rel.arrowDirection, assoc.arrowDirection)
        assertEquals(rel.labelStyle, assoc.labelStyle)
        assertEquals(rel.hiddenAttributes, assoc.hiddenAttributes)
        assertEquals("", assoc.observations)
        assertEquals("", assoc.dictionary)
    }

    @Test
    fun `promote multiple selected relationships assigns distinct outer names`() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "E1", ElementPosition(0, 0, 102, 66))
        val e2 = SchemaElement.Entity(2, "E2", ElementPosition(200, 0, 102, 66))
        val e3 = SchemaElement.Entity(4, "E3", ElementPosition(400, 0, 102, 66))
        val relA = SchemaElement.Relationship(3, "R_A", ElementPosition(100, 0, 40, 40))
        val relB = SchemaElement.Relationship(5, "R_B", ElementPosition(300, 0, 40, 40))
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 2 to e2, 3 to relA, 4 to e3, 5 to relB),
            connections = listOf(
                Connection(1, 1, 3, null, showCardinality = true, orientation = LineOrientation.VERTICAL),
                Connection(2, 2, 3, null, showCardinality = true, orientation = LineOrientation.VERTICAL),
                Connection(3, 2, 5, null, showCardinality = true, orientation = LineOrientation.VERTICAL),
                Connection(4, 4, 5, null, showCardinality = true, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 6,
        )
        val sel = CanvasSelection.Multiple(elementIds = setOf(3, 5), cardinalityConnectionIds = emptySet())

        // Act
        val out = applyPromoteRelationshipsToAssociativeEntities(schema, sel)

        // Assert
        assertNotNull(out)
        val a = out.elements[3] as SchemaElement.AssociativeEntity
        val b = out.elements[5] as SchemaElement.AssociativeEntity
        assertEquals("EntAssoc1", a.name)
        assertEquals("EntAssoc2", b.name)
        assertEquals("R_A", a.relationshipName)
        assertEquals("R_B", b.relationshipName)
    }

    @Test
    fun `outer name skips collision with plain entity named EntAssoc1`() {
        // Arrange
        val blocker = SchemaElement.Entity(1, "EntAssoc1", ElementPosition(0, 0, 102, 66))
        val e2 = SchemaElement.Entity(2, "E2", ElementPosition(200, 0, 102, 66))
        val rel = SchemaElement.Relationship(3, "R", ElementPosition(100, 0, 40, 40))
        val schema = ConceptualSchema(
            elements = mapOf(1 to blocker, 2 to e2, 3 to rel),
            connections = listOf(
                Connection(1, 1, 3, null, showCardinality = true, orientation = LineOrientation.VERTICAL),
                Connection(2, 2, 3, null, showCardinality = true, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 4,
        )
        val sel = CanvasSelection.Element(3)

        // Act
        val out = applyPromoteRelationshipsToAssociativeEntities(schema, sel)

        // Assert
        assertNotNull(out)
        val assoc = out.elements[3] as SchemaElement.AssociativeEntity
        assertEquals("EntAssoc2", assoc.name)
    }
}
