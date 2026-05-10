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

class ConceptualDemoteAssociativeEntityTest {

    @Test
    fun `demote menu false without associative in selection`() {
        // Arrange
        val e = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 102, 66))
        val schema = ConceptualSchema(elements = mapOf(1 to e), nextId = 2)

        // Act & Assert
        assertFalse(canDemoteAssociativeToRelationshipMenu(schema, CanvasSelection.Element(1)))
        assertFalse(canDemoteAssociativeToEntityMenu(schema, CanvasSelection.Element(1)))
    }

    @Test
    fun `demote to relationship preserves id connections and inner metadata`() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(0, 100, 102, 66))
        val e2 = SchemaElement.Entity(2, "B", ElementPosition(400, 100, 102, 66))
        val outer = ElementPosition(180, 90, 127, 66)
        val assoc =
            SchemaElement.AssociativeEntity(
                id = 3,
                name = "OuterName",
                position = outer,
                observations = "outObs",
                dictionary = "outDic",
                relationshipName = "InnerRel",
                relationshipDictionary = "inDic",
                relationshipObservations = "inObs",
                arrowDirection = ArrowDirection.TOP_RIGHT,
            )
        val inner = associativeInnerDiamondPosition(outer)
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 2 to e2, 3 to assoc),
            connections = listOf(
                Connection(
                    id = 10,
                    elementIdA = 3,
                    elementIdB = 1,
                    showCardinality = true,
                    orientation = LineOrientation.HORIZONTAL,
                    useAssociativeOuterForEndA = true,
                ),
                Connection(
                    id = 11,
                    elementIdA = 3,
                    elementIdB = 2,
                    showCardinality = true,
                    orientation = LineOrientation.HORIZONTAL,
                    useAssociativeOuterForEndA = false,
                ),
            ),
            nextId = 20,
        )

        // Act
        val out = applyDemoteAssociativeToRelationship(schema, CanvasSelection.Element(3))

        // Assert
        assertNotNull(out)
        val rel = out.elements[3] as SchemaElement.Relationship
        assertEquals("InnerRel", rel.name)
        assertEquals(inner, rel.position)
        assertEquals("inObs", rel.observations)
        assertEquals("inDic", rel.dictionary)
        assertEquals(ArrowDirection.TOP_RIGHT, rel.arrowDirection)
        assertEquals(2, out.connections.size)
        assertTrue(out.connections.none { it.useAssociativeOuterForEndA || it.useAssociativeOuterForEndB })
        assertEquals(
            setOf(3 to 1, 3 to 2),
            out.connections.map { it.elementIdA to it.elementIdB }.toSet(),
        )
    }

    @Test
    fun `demote to entity preserves id outer metadata and strips assoc flags`() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(0, 100, 102, 66))
        val outer = ElementPosition(200, 90, 127, 66)
        val assoc =
            SchemaElement.AssociativeEntity(
                id = 3,
                name = "Participante",
                position = outer,
                observations = "eo",
                dictionary = "ed",
                relationshipName = "R",
                relationshipDictionary = "rd",
            )
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 3 to assoc),
            connections = listOf(
                Connection(
                    id = 10,
                    elementIdA = 3,
                    elementIdB = 1,
                    useAssociativeOuterForEndA = false,
                ),
            ),
            nextId = 20,
        )

        // Act
        val out = applyDemoteAssociativeToEntity(schema, CanvasSelection.Element(3))

        // Assert
        assertNotNull(out)
        val ent = out.elements[3] as SchemaElement.Entity
        assertEquals("Participante", ent.name)
        assertEquals(outer, ent.position)
        assertEquals("eo", ent.observations)
        assertEquals("ed", ent.dictionary)
        val newRel = out.relationships.single { it.id != 3 }
        assertEquals("R", newRel.name)
        assertEquals(associativeInnerDiamondPosition(outer), newRel.position)
        assertEquals(1, out.connections.size)
        assertEquals(newRel.id, out.connections.single().elementIdA)
        assertEquals(1, out.connections.single().elementIdB)
        assertFalse(out.connections.any { it.elementIdA == 3 || it.elementIdB == 3 })
    }

    @Test
    fun `demote to entity inserts new relationship for inner binary participants`() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(0, 100, 102, 66))
        val e2 = SchemaElement.Entity(2, "B", ElementPosition(400, 100, 102, 66))
        val outer = ElementPosition(180, 90, 127, 66)
        val assoc =
            SchemaElement.AssociativeEntity(
                id = 3,
                name = "EntAssoc1",
                position = outer,
                relationshipName = "Relacao2",
            )
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 2 to e2, 3 to assoc),
            connections = listOf(
                Connection(
                    id = 10,
                    elementIdA = 3,
                    elementIdB = 1,
                    showCardinality = true,
                    orientation = LineOrientation.HORIZONTAL,
                    useAssociativeOuterForEndA = false,
                ),
                Connection(
                    id = 11,
                    elementIdA = 3,
                    elementIdB = 2,
                    showCardinality = true,
                    orientation = LineOrientation.HORIZONTAL,
                    useAssociativeOuterForEndA = false,
                ),
            ),
            nextId = 20,
        )

        // Act
        val out = applyDemoteAssociativeToEntity(schema, CanvasSelection.Element(3))

        // Assert
        assertNotNull(out)
        assertTrue(out.elements[3] is SchemaElement.Entity)
        val newRel = out.relationships.single { it.id != 3 }
        assertEquals("Relacao2", newRel.name)
        assertEquals(associativeInnerDiamondPosition(outer), newRel.position)
        val legs = out.connections.filter { it.elementIdA == newRel.id }.map { it.elementIdB }.toSet()
        assertEquals(setOf(1, 2), legs)
        assertFalse(out.connections.any { it.elementIdA == 3 || it.elementIdB == 3 })
    }

    @Test
    fun `apply returns null when nothing to demote`() {
        // Arrange
        val schema = ConceptualSchema(nextId = 1)

        // Act & Assert
        assertNull(applyDemoteAssociativeToRelationship(schema, CanvasSelection.None))
        assertNull(applyDemoteAssociativeToEntity(schema, CanvasSelection.None))
    }

    @Test
    fun `demote to entity creates inner relationship with no legs when associative has no connections`() {
        // Arrange
        val outer = ElementPosition(200, 90, 127, 66)
        val assoc =
            SchemaElement.AssociativeEntity(
                id = 3,
                name = "Isolada",
                position = outer,
                relationshipName = "RelVazia",
                relationshipDictionary = "dicRel",
            )
        val schema = ConceptualSchema(
            elements = mapOf(3 to assoc),
            connections = emptyList(),
            nextId = 10,
        )

        // Act
        val out = applyDemoteAssociativeToEntity(schema, CanvasSelection.Element(3))

        // Assert
        assertNotNull(out)
        assertTrue(out.elements[3] is SchemaElement.Entity)
        val newRel = out.relationships.single { it.id != 3 }
        assertEquals("RelVazia", newRel.name)
        assertEquals("dicRel", newRel.dictionary)
        assertEquals(associativeInnerDiamondPosition(outer), newRel.position)
        assertTrue(out.connections.isEmpty())
    }
}
