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

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConceptualAttributeToolTest {

    @Test
    fun `closest side - click near right edge picks RIGHT`() {
        // Arrange
        val owner = ElementPosition(x = 100, y = 100, width = 102, height = 66)
        val click = Offset(205f, 133f) // near right edge, vertically centred

        // Act
        val side = closestConceptualAttributeAttachPonto(owner, click)

        // Assert
        assertEquals(ConceptualAttributeAttachPonto.RIGHT, side)
    }

    @Test
    fun `closest side - click near top edge picks TOP`() {
        // Arrange
        val owner = ElementPosition(x = 100, y = 100, width = 102, height = 66)
        val click = Offset(151f, 95f)

        // Act
        val side = closestConceptualAttributeAttachPonto(owner, click)

        // Assert
        assertEquals(ConceptualAttributeAttachPonto.TOP, side)
    }

    @Test
    fun `basic attribute on entity has correct owner and defaults`() {
        // Arrange
        val ent = SchemaElement.Entity(
            id = 1,
            name = "Entidade1",
            position = ElementPosition(100, 100, 102, 66),
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent),
            nextId = 2,
        )
        val click = Offset(205f, 133f)

        // Act
        val r = applyConceptualAttributeTool(schema, 1, click, ConceptualAttributeToolVariant.Basic)

        // Assert
        val ok = assertIs<ConceptualAttributeToolResult.Ok>(r)
        val attr = ok.schema.elements[ok.newPrimaryAttributeId]!!
        assertIs<SchemaElement.Attribute>(attr)
        assertEquals(1, attr.ownerId)
        assertTrue(attr.position.x > ent.position.x + ent.position.width)
        assertTrue(
            ok.schema.connections.any { it.elementIdA == attr.id && it.elementIdB == 1 },
            "attribute-owner Connection missing (needed to draw the line)",
        )
    }

    @Test
    fun `new attribute name avoids names used in hidden attribute trees`() {
        // Arrange
        val ent = SchemaElement.Entity(
            id = 1,
            name = "Entidade1",
            position = ElementPosition(100, 100, 102, 66),
            hiddenAttributes = listOf(
                HiddenAttribute(
                    name = "Atributo1",
                    type = "",
                    isIdentifier = false,
                    cardinality = AttributeCardinality(0, 0),
                    position = ElementPosition(-1, -1, 0, 0),
                ),
            ),
        )
        val schema = ConceptualSchema(elements = mapOf(1 to ent), nextId = 2)
        val click = Offset(205f, 133f)

        // Act
        val r = applyConceptualAttributeTool(schema, 1, click, ConceptualAttributeToolVariant.Basic)

        // Assert
        val ok = assertIs<ConceptualAttributeToolResult.Ok>(r)
        val attr = ok.schema.elements[ok.newPrimaryAttributeId] as SchemaElement.Attribute
        assertEquals("Atributo2", attr.name)
    }

    @Test
    fun `multivalued variant sets cardinality 1 n`() {
        // Arrange
        val ent = SchemaElement.Entity(
            id = 1,
            name = "Entidade1",
            position = ElementPosition(100, 100, 102, 66),
        )
        val schema = ConceptualSchema(elements = mapOf(1 to ent), nextId = 2)
        val click = Offset(205f, 133f)

        // Act
        val r = applyConceptualAttributeTool(schema, 1, click, ConceptualAttributeToolVariant.MultiValued)

        // Assert
        val ok = assertIs<ConceptualAttributeToolResult.Ok>(r)
        val attr = ok.schema.elements[ok.newPrimaryAttributeId] as SchemaElement.Attribute
        assertTrue(attr.isMultiValued)
        assertEquals(1, attr.cardinality.minCardinality)
        assertEquals(21, attr.cardinality.maxCardinality)
    }

    @Test
    fun `composite creates parent and two children`() {
        // Arrange
        val ent = SchemaElement.Entity(
            id = 1,
            name = "Entidade1",
            position = ElementPosition(100, 100, 102, 66),
        )
        val schema = ConceptualSchema(elements = mapOf(1 to ent), nextId = 2)
        val click = Offset(205f, 133f)

        // Act
        val r = applyConceptualAttributeTool(schema, 1, click, ConceptualAttributeToolVariant.Composite)

        // Assert
        val ok = assertIs<ConceptualAttributeToolResult.Ok>(r)
        val parent = ok.schema.elements[ok.newPrimaryAttributeId] as SchemaElement.Attribute
        assertEquals(2, parent.childAttributeIds.size)
        val c0 = ok.schema.elements[parent.childAttributeIds[0]] as SchemaElement.Attribute
        val c1 = ok.schema.elements[parent.childAttributeIds[1]] as SchemaElement.Attribute
        assertEquals(parent.id, c0.ownerId)
        assertEquals(parent.id, c1.ownerId)
        assertTrue(c0.position.y != c1.position.y)
        assertEquals(3, ok.schema.connections.count { it.elementIdA in setOf(parent.id, c0.id, c1.id) })
    }

    @Test
    fun `invalid owner returns error`() {
        // Arrange
        val spec = SchemaElement.Specialization(
            id = 1,
            name = "Esp1",
            position = ElementPosition(200, 200, 25, 31),
            baseEntityId = 99,
        )
        val schema = ConceptualSchema(elements = mapOf(1 to spec), nextId = 2)

        // Act
        val r = applyConceptualAttributeTool(schema, 1, Offset(210f, 210f), ConceptualAttributeToolVariant.Basic)

        // Assert
        assertIs<ConceptualAttributeToolResult.Error>(r)
    }

    @Test
    fun `child attribute on parent attribute always attaches on RIGHT`() {
        // Arrange
        val ent = SchemaElement.Entity(
            id = 1,
            name = "Entidade1",
            position = ElementPosition(100, 100, 102, 66),
        )
        val parentAttr = SchemaElement.Attribute(
            id = 2,
            name = "A1",
            position = ElementPosition(210, 117, 80, 32),
            ownerId = 1,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to parentAttr),
            connections = listOf(
                Connection(
                    id = 10,
                    elementIdA = 2,
                    elementIdB = 1,
                    cardinality = null,
                    showCardinality = false,
                    orientation = LineOrientation.VERTICAL,
                ),
            ),
            nextId = 20,
        )
        val clickLeftOfParent = Offset(180f, 130f)

        // Act
        val r = applyConceptualAttributeTool(schema, 2, clickLeftOfParent, ConceptualAttributeToolVariant.Basic)

        // Assert
        val ok = assertIs<ConceptualAttributeToolResult.Ok>(r)
        assertEquals(ConceptualAttributeAttachPonto.RIGHT, ok.attachSide)
        val child = ok.schema.elements[ok.newPrimaryAttributeId] as SchemaElement.Attribute
        assertTrue(child.position.x >= parentAttr.position.x + parentAttr.position.width)
    }

    @Test
    fun `composite variant on another attribute creates composite owned by that attribute`() {
        // Arrange
        val ent = SchemaElement.Entity(
            id = 1,
            name = "Entidade1",
            position = ElementPosition(100, 100, 102, 66),
        )
        val hostAttr = SchemaElement.Attribute(
            id = 2,
            name = "Host",
            position = ElementPosition(220, 117, 80, 32),
            ownerId = 1,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to hostAttr),
            connections = listOf(
                Connection(
                    id = 10,
                    elementIdA = 2,
                    elementIdB = 1,
                    cardinality = null,
                    showCardinality = false,
                    orientation = LineOrientation.VERTICAL,
                ),
            ),
            nextId = 20,
        )
        val click = Offset(310f, 130f)

        // Act
        val r = applyConceptualAttributeTool(schema, 2, click, ConceptualAttributeToolVariant.Composite)

        // Assert
        val ok = assertIs<ConceptualAttributeToolResult.Ok>(r)
        val composite = ok.schema.elements[ok.newPrimaryAttributeId] as SchemaElement.Attribute
        assertEquals(2, composite.ownerId)
        assertEquals(2, composite.childAttributeIds.size)
        val updatedHost = ok.schema.elements[2] as SchemaElement.Attribute
        assertTrue(ok.newPrimaryAttributeId in updatedHost.childAttributeIds)
    }
}
