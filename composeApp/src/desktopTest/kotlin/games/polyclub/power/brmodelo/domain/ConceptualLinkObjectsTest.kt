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
import kotlin.test.assertTrue

class ConceptualLinkObjectsTest {

    private val pos = ElementPosition(0, 0, 100, 80)

    @Test
    fun `validate rejects inner associative end linked to outer of same associative`() {
        // Arrange
        val assoc = SchemaElement.AssociativeEntity(
            id = 1,
            name = "EntAssoc1",
            position = pos,
            relationshipName = "R",
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to assoc),
            nextId = 10,
        )
        val inner = ConceptualLinkPick(1, isAssociativeOuterEntitySide = false)
        val outer = ConceptualLinkPick(1, isAssociativeOuterEntitySide = true)

        // Act
        val r = validateAndBuildConceptualLink(schema, inner, outer)

        // Assert
        val err = assertIs<ConceptualLinkValidationResult.Error>(r)
        assertTrue(err.message.isNotBlank())
    }

    @Test
    fun `validate builds connection with outer entity end on associative when ordered`() {
        // Arrange
        val assoc = SchemaElement.AssociativeEntity(id = 1, name = "EA", position = pos)
        val rel = SchemaElement.Relationship(id = 2, name = "Rel", position = ElementPosition(200, 0, 80, 60))
        val schema = ConceptualSchema(
            elements = mapOf(1 to assoc, 2 to rel),
            nextId = 10,
        )
        val outer = ConceptualLinkPick(1, isAssociativeOuterEntitySide = true)
        val relPick = ConceptualLinkPick(2, isAssociativeOuterEntitySide = false)

        // Act
        val r = validateAndBuildConceptualLink(schema, outer, relPick)

        // Assert
        val ok = assertIs<ConceptualLinkValidationResult.Ok>(r)
        val conn = ok.schema.connections.single { it.id !in schema.connections.map { c -> c.id } }
        assertTrue(conn.useAssociativeOuterForEndB)
        assertTrue(!conn.useAssociativeOuterForEndA)
        assertTrue(conn.elementIdA == 2)
        assertTrue(conn.elementIdB == 1)
    }

    @Test
    fun `entity to entity creates relationship at midpoint and two connections`() {
        // Arrange
        val e1 = SchemaElement.Entity(id = 1, name = "A", position = ElementPosition(0, 0, 100, 80))
        val e2 = SchemaElement.Entity(id = 2, name = "B", position = ElementPosition(200, 100, 100, 80))
        val schema = ConceptualSchema(elements = mapOf(1 to e1, 2 to e2), nextId = 10)

        // Act
        val r = validateAndBuildConceptualLink(schema, ConceptualLinkPick(1), ConceptualLinkPick(2))

        // Assert
        val ok = assertIs<ConceptualLinkValidationResult.Ok>(r)
        val s = ok.schema
        val rel = s.relationships.single()
        assertEquals("Relacao1", rel.name)
        assertEquals(100, rel.position.x)
        assertEquals(50, rel.position.y)
        assertEquals(2, s.connections.size)
        assertTrue(s.connections.all { it.elementIdA == rel.id })
        assertEquals(setOf(1, 2), s.connections.map { it.elementIdB }.toSet())
    }

    @Test
    fun `entity to entity skips taken Relacao names`() {
        // Arrange
        val e1 = SchemaElement.Entity(id = 1, name = "A", position = ElementPosition(0, 0, 50, 50))
        val e2 = SchemaElement.Entity(id = 2, name = "B", position = ElementPosition(100, 0, 50, 50))
        val existing = SchemaElement.Relationship(id = 3, name = "Relacao1", position = ElementPosition(40, 80, 80, 40))
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 2 to e2, 3 to existing),
            nextId = 20,
        )

        // Act
        val ok = assertIs<ConceptualLinkValidationResult.Ok>(
            validateAndBuildConceptualLink(schema, ConceptualLinkPick(1), ConceptualLinkPick(2)),
        )

        // Assert
        val newRel = ok.schema.relationships.filter { it.id != 3 }.single()
        assertEquals("Relacao2", newRel.name)
    }

    @Test
    fun `same entity pick twice creates self-relationship with two connections and Auto name`() {
        // Arrange
        val e1 = SchemaElement.Entity(id = 1, name = "A", position = ElementPosition(10, 20, 100, 90))
        val schema = ConceptualSchema(elements = mapOf(1 to e1), nextId = 10)
        val pick = ConceptualLinkPick(1)

        // Act
        val r = validateAndBuildConceptualLink(schema, pick, pick)

        // Assert
        val ok = assertIs<ConceptualLinkValidationResult.Ok>(r)
        val s = ok.schema
        val selfRel = s.selfRelationships.single()
        assertEquals(1, selfRel.ownerEntityId)
        assertEquals("Auto1", selfRel.name)
        assertEquals(10 + 100 + 30, selfRel.position.x)
        assertEquals(20 + 90 / 6, selfRel.position.y)
        val third = 90 / 3
        assertEquals(2 * (90 - third), selfRel.position.width)
        assertEquals(90 - third, selfRel.position.height)
        assertEquals(2, s.connections.size)
        assertTrue(s.connections.all { it.elementIdA == selfRel.id && it.elementIdB == 1 })
    }

    @Test
    fun `second auto-rel on same entity is rejected`() {
        // Arrange
        val e1 = SchemaElement.Entity(id = 1, name = "A", position = ElementPosition(0, 0, 50, 60))
        val existing = SchemaElement.SelfRelationship(
            id = 2,
            name = "Auto1",
            position = ElementPosition(200, 0, 40, 40),
            ownerEntityId = 1,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 2 to existing),
            nextId = 20,
        )
        val pick = ConceptualLinkPick(1)

        // Act
        val r = validateAndBuildConceptualLink(schema, pick, pick)

        // Assert
        val err = assertIs<ConceptualLinkValidationResult.Error>(r)
        assertTrue(err.message.contains("auto-relacionamento", ignoreCase = true))
    }

    @Test
    fun `third leg from self-relationship to same entity is duplicate`() {
        // Arrange
        val e1 = SchemaElement.Entity(id = 1, name = "A", position = ElementPosition(0, 0, 50, 60))
        val selfRel = SchemaElement.SelfRelationship(
            id = 2,
            name = "Auto1",
            position = ElementPosition(100, 0, 40, 40),
            ownerEntityId = 1,
        )
        val c1 = Connection(
            id = 3,
            elementIdA = 2,
            elementIdB = 1,
            cardinality = Cardinality.ZERO_TO_MANY,
            showCardinality = true,
            orientation = LineOrientation.HORIZONTAL,
        )
        val c2 = Connection(
            id = 4,
            elementIdA = 2,
            elementIdB = 1,
            cardinality = Cardinality.ZERO_TO_MANY,
            showCardinality = true,
            orientation = LineOrientation.HORIZONTAL,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 2 to selfRel),
            connections = listOf(c1, c2),
            nextId = 20,
        )

        // Act
        val r = validateAndBuildConceptualLink(
            schema,
            ConceptualLinkPick(2),
            ConceptualLinkPick(1),
        )

        // Assert
        val err = assertIs<ConceptualLinkValidationResult.Error>(r)
        assertTrue(err.message.contains("Já existe", ignoreCase = true))
    }

    @Test
    fun `entity relationship then relationship entity upgrades diamond to self-relationship`() {
        // Arrange
        val e = SchemaElement.Entity(id = 1, name = "A", position = ElementPosition(0, 0, 80, 60))
        val r = SchemaElement.Relationship(
            id = 2,
            name = "Relacao1",
            position = ElementPosition(100, 0, 50, 50),
        )
        val schema = ConceptualSchema(elements = mapOf(1 to e, 2 to r), nextId = 10)
        val pickE = ConceptualLinkPick(1)
        val pickR = ConceptualLinkPick(2)

        // Act
        val step1 = validateAndBuildConceptualLink(schema, pickE, pickR)
        val ok1 = assertIs<ConceptualLinkValidationResult.Ok>(step1)
        val step2 = validateAndBuildConceptualLink(ok1.schema, pickR, pickE)

        // Assert
        val ok2 = assertIs<ConceptualLinkValidationResult.Ok>(step2)
        val s = ok2.schema
        val diamond = s.elements[2]
        assertTrue(diamond is SchemaElement.SelfRelationship)
        assertEquals(1, (diamond as SchemaElement.SelfRelationship).ownerEntityId)
        assertEquals("Relacao1", diamond.name)
        assertEquals(2, s.connections.size)
        assertTrue(s.connections.all { it.elementIdA == 2 && it.elementIdB == 1 })
    }

    @Test
    fun `second leg on loose relationship rejected when entity already has self-relationship`() {
        // Arrange
        val e = SchemaElement.Entity(id = 1, name = "A", position = ElementPosition(0, 0, 80, 60))
        val existingSelf = SchemaElement.SelfRelationship(
            id = 2,
            name = "Auto1",
            position = ElementPosition(50, 0, 40, 40),
            ownerEntityId = 1,
        )
        val looseRel = SchemaElement.Relationship(
            id = 3,
            name = "Relacao2",
            position = ElementPosition(150, 0, 50, 50),
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to e, 2 to existingSelf, 3 to looseRel),
            connections = listOf(
                Connection(
                    id = 10,
                    elementIdA = 2,
                    elementIdB = 1,
                    cardinality = Cardinality.ZERO_TO_MANY,
                    showCardinality = true,
                    orientation = LineOrientation.HORIZONTAL,
                ),
                Connection(
                    id = 11,
                    elementIdA = 2,
                    elementIdB = 1,
                    cardinality = Cardinality.ZERO_TO_MANY,
                    showCardinality = true,
                    orientation = LineOrientation.HORIZONTAL,
                ),
            ),
            nextId = 20,
        )
        val afterFirst = validateAndBuildConceptualLink(
            schema,
            ConceptualLinkPick(1),
            ConceptualLinkPick(3),
        )
        assertIs<ConceptualLinkValidationResult.Ok>(afterFirst)

        // Act
        val r = validateAndBuildConceptualLink(
            afterFirst.schema,
            ConceptualLinkPick(3),
            ConceptualLinkPick(1),
        )

        // Assert
        val err = assertIs<ConceptualLinkValidationResult.Error>(r)
        assertTrue(err.message.contains("Já existe", ignoreCase = true))
    }

    @Test
    fun `cannot add duplicate leg between relationship and entity when rel already links two entities`() {
        // Arrange
        val e1 = SchemaElement.Entity(id = 1, name = "A", position = ElementPosition(0, 0, 50, 50))
        val e2 = SchemaElement.Entity(id = 2, name = "B", position = ElementPosition(100, 0, 50, 50))
        val r = SchemaElement.Relationship(id = 3, name = "R", position = ElementPosition(50, 50, 40, 40))
        val schema = ConceptualSchema(elements = mapOf(1 to e1, 2 to e2, 3 to r), nextId = 10)
        val s1 = assertIs<ConceptualLinkValidationResult.Ok>(
            validateAndBuildConceptualLink(schema, ConceptualLinkPick(1), ConceptualLinkPick(3)),
        )
        val s2 = assertIs<ConceptualLinkValidationResult.Ok>(
            validateAndBuildConceptualLink(s1.schema, ConceptualLinkPick(2), ConceptualLinkPick(3)),
        )

        // Act
        val rDup = validateAndBuildConceptualLink(
            s2.schema,
            ConceptualLinkPick(3),
            ConceptualLinkPick(1),
        )

        // Assert
        assertIs<ConceptualLinkValidationResult.Error>(rDup)
    }
}
