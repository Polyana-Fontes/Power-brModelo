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
}
