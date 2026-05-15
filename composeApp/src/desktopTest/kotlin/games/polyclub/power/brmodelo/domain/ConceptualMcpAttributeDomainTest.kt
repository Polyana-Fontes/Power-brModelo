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

class ConceptualMcpAttributeDomainTest {

    @Test
    fun `apply simple attribute allows same name on different owners`() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(0, 0, 100, 60))
        val e2 = SchemaElement.Entity(2, "B", ElementPosition(200, 0, 100, 60))
        val existing = SchemaElement.Attribute(
            id = 3,
            name = "nome",
            position = ElementPosition(120, 20, 80, 24),
            ownerId = 1,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 2 to e2, 3 to existing),
            connections = listOf(
                Connection(10, 3, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 20,
        )

        // Act
        val r = applyConceptualSimpleAttributeTool(
            schema,
            ownerElementId = 2,
            variant = ConceptualAttributeToolVariant.Basic,
            attachSide = ConceptualAttributeAttachPonto.RIGHT,
            overrides = ConceptualSimpleAttributePlacementOverrides(name = "nome"),
        )

        // Assert
        val ok = assertIs<ConceptualAttributeToolResult.Ok>(r)
        val added = ok.schema.elements[ok.newPrimaryAttributeId] as SchemaElement.Attribute
        assertEquals("nome", added.name)
        assertEquals(2, added.ownerId)
    }

    @Test
    fun `apply simple attribute rejects duplicate name on same owner`() {
        // Arrange
        val e1 = SchemaElement.Entity(1, "A", ElementPosition(0, 0, 100, 60))
        val existing = SchemaElement.Attribute(
            id = 3,
            name = "nome",
            position = ElementPosition(120, 20, 80, 24),
            ownerId = 1,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 3 to existing),
            connections = listOf(
                Connection(10, 3, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 20,
        )

        // Act
        val r = applyConceptualSimpleAttributeTool(
            schema,
            ownerElementId = 1,
            variant = ConceptualAttributeToolVariant.Basic,
            attachSide = ConceptualAttributeAttachPonto.RIGHT,
            overrides = ConceptualSimpleAttributePlacementOverrides(name = "nome"),
        )

        // Assert
        assertIs<ConceptualAttributeToolResult.Error>(r)
    }

    @Test
    fun `preferred attach side picks edge with fewest attributes`() {
        // Arrange
        val ent = SchemaElement.Entity(
            id = 1,
            name = "E",
            position = ElementPosition(0, 0, 100, 60),
        )
        val aRight = SchemaElement.Attribute(
            id = 2,
            name = "A1",
            position = ElementPosition(120, 10, 80, 24),
            ownerId = 1,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to aRight),
            connections = listOf(
                Connection(
                    id = 1,
                    elementIdA = 2,
                    elementIdB = 1,
                    cardinality = null,
                    showCardinality = false,
                    orientation = LineOrientation.VERTICAL,
                ),
            ),
            nextId = 3,
        )

        // Act
        val side = preferredAttachSideForConceptualOwner(schema, 1)

        // Assert
        assertEquals(ConceptualAttributeAttachPonto.LEFT, side)
    }

    @Test
    fun `apply simple attribute with explicit LEFT side`() {
        // Arrange
        val ent = SchemaElement.Entity(
            id = 1,
            name = "E",
            position = ElementPosition(100, 100, 102, 66),
        )
        val schema = ConceptualSchema(elements = mapOf(1 to ent), nextId = 2)

        // Act
        val r = applyConceptualSimpleAttributeTool(
            schema,
            ownerElementId = 1,
            variant = ConceptualAttributeToolVariant.Basic,
            attachSide = ConceptualAttributeAttachPonto.LEFT,
            overrides = null,
        )

        // Assert
        val ok = assertIs<ConceptualAttributeToolResult.Ok>(r)
        assertEquals(ConceptualAttributeAttachPonto.LEFT, ok.attachSide)
        val attr = ok.schema.elements[ok.newPrimaryAttributeId] as SchemaElement.Attribute
        assertTrue(attr.position.x + attr.position.width <= ent.position.x)
    }

    @Test
    fun `composite with single leaf creates one child`() {
        // Arrange
        val ent = SchemaElement.Entity(
            id = 1,
            name = "E",
            position = ElementPosition(50, 50, 100, 60),
        )
        val schema = ConceptualSchema(elements = mapOf(1 to ent), nextId = 2)

        // Act
        val r = applyConceptualCompositeAttributeWithLeafChildren(
            schema,
            ownerElementId = 1,
            attachSide = ConceptualAttributeAttachPonto.RIGHT,
            leafSpecs = listOf(ConceptualCompositeLeafSpec(name = "Campo1")),
            nestedHiddenAttributes = emptyList(),
        )

        // Assert
        val ok = assertIs<ConceptualAttributeToolResult.Ok>(r)
        val parent = ok.schema.elements[ok.newPrimaryAttributeId] as SchemaElement.Attribute
        assertTrue(parent.isComposite)
        assertEquals(1, parent.childAttributeIds.size)
    }

    @Test
    fun `append hidden forest on entity`() {
        // Arrange
        val ent = SchemaElement.Entity(
            id = 1,
            name = "E",
            position = ElementPosition(0, 0, 80, 50),
        )
        val schema = ConceptualSchema(elements = mapOf(1 to ent), nextId = 2)
        val root = HiddenAttribute(
            name = "Oculto1",
            type = "INTEGER",
            isIdentifier = false,
            cardinality = AttributeCardinality(0, 0),
            position = ElementPosition(-1, -1, 0, 0),
        )

        // Act
        val out = applyAppendHiddenAttributeForest(schema, holderElementId = 1, newRoots = listOf(root))

        // Assert
        val ok = assertIs<ConceptualAppendHiddenAttributesResult.Ok>(out)
        val updated = ok.schema.elements[1] as SchemaElement.Entity
        assertEquals(1, updated.hiddenAttributes.size)
        assertEquals("Oculto1", updated.hiddenAttributes[0].name)
    }
}
