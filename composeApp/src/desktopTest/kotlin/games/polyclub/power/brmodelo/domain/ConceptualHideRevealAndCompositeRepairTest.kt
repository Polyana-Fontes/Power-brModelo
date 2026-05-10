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

class ConceptualHideRevealAndCompositeRepairTest {

    @Test
    fun `withoutElements drops composite flag when all child attributes are removed`() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 102, 66))
        val comp = SchemaElement.Attribute(
            id = 2,
            name = "Endereco",
            position = ElementPosition(120, 10, 73, 16),
            ownerId = 1,
            childAttributeIds = listOf(3),
            multiValuedCount = 1,
        )
        val child = SchemaElement.Attribute(
            id = 3,
            name = "Rua",
            position = ElementPosition(200, 10, 73, 16),
            ownerId = 2,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to comp, 3 to child),
            connections = listOf(
                Connection(10, 2, 1, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(11, 3, 2, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 20,
        )

        // Act
        val out = schema.withoutElements(setOf(3))

        // Assert
        val parent = out.elements[2] as SchemaElement.Attribute
        assertTrue(parent.childAttributeIds.isEmpty())
        assertFalse(parent.isComposite)
        assertEquals(0, parent.multiValuedCount)
    }

    @Test
    fun `hide then reveal round-trips a simple attribute onto an entity`() {
        // Arrange
        val ent = SchemaElement.Entity(1, "Pessoa", ElementPosition(0, 0, 102, 66))
        val attr = SchemaElement.Attribute(
            id = 2,
            name = "Nome",
            position = ElementPosition(120, 10, 73, 16),
            ownerId = 1,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to attr),
            connections = listOf(
                Connection(10, 2, 1, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 50,
        )

        // Act — hide
        val hidden = applyHideCanvasAttribute(schema, CanvasSelection.Element(2))
        assertNotNull(hidden)
        val entAfterHide = hidden.elements[1] as SchemaElement.Entity
        assertEquals(1, entAfterHide.hiddenAttributes.size)
        assertEquals("Nome", entAfterHide.hiddenAttributes.single().name)

        // Act — reveal
        val revealed = applyRevealHiddenAttribute(hidden, 1, listOf(0))
        assertNotNull(revealed)
        val (after, newId) = revealed
        val back = after.elements[newId] as SchemaElement.Attribute
        assertEquals("Nome", back.name)
        assertTrue((after.elements[1] as SchemaElement.Entity).hiddenAttributes.isEmpty())
    }

    @Test
    fun `hide stores max cardinality 0 for non multi-valued attribute so reveal is not multi-valued`() {
        // Arrange — tool-created simple attributes often use (1,1) with isMultiValued false; oculto must mirror Pascal (Ma := 0).
        val ent = SchemaElement.Entity(1, "Pessoa", ElementPosition(0, 0, 102, 66))
        val attr = SchemaElement.Attribute(
            id = 2,
            name = "Nome",
            position = ElementPosition(120, 10, 73, 16),
            ownerId = 1,
            isMultiValued = false,
            cardinality = AttributeCardinality(1, 1),
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to attr),
            connections = listOf(
                Connection(10, 2, 1, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 50,
        )

        // Act — hide
        val hidden = applyHideCanvasAttribute(schema, CanvasSelection.Element(2))
        assertNotNull(hidden)
        val entAfterHide = hidden.elements[1] as SchemaElement.Entity
        val oculto = entAfterHide.hiddenAttributes.single()
        assertFalse(oculto.isMultiValued)
        assertEquals(0, oculto.cardinality.maxCardinality)
        assertEquals(1, oculto.cardinality.minCardinality)

        // Act — reveal
        val revealed = applyRevealHiddenAttribute(hidden, 1, listOf(0))
        assertNotNull(revealed)
        val (_, newId) = revealed
        val back = revealed.first.elements[newId] as SchemaElement.Attribute

        // Assert
        assertFalse(back.isMultiValued)
        assertEquals(1, back.cardinality.minCardinality)
        assertEquals(0, back.cardinality.maxCardinality)
    }

    @Test
    fun `hide then reveal restores full composite canvas subtree`() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 102, 66))
        val child1 = SchemaElement.Attribute(
            id = 3,
            name = "Rua",
            position = ElementPosition(200, 10, 73, 16),
            ownerId = 2,
        )
        val child2 = SchemaElement.Attribute(
            id = 4,
            name = "Num",
            position = ElementPosition(200, 30, 73, 16),
            ownerId = 2,
        )
        val comp = SchemaElement.Attribute(
            id = 2,
            name = "Endereco",
            position = ElementPosition(120, 10, 73, 16),
            ownerId = 1,
            childAttributeIds = listOf(3, 4),
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to comp, 3 to child1, 4 to child2),
            connections = listOf(
                Connection(10, 2, 1, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(11, 3, 2, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(12, 4, 2, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 50,
        )

        // Act
        val hidden = applyHideCanvasAttribute(schema, CanvasSelection.Element(2))
        assertNotNull(hidden)
        val revealed = applyRevealHiddenAttribute(hidden, 1, listOf(0))
        assertNotNull(revealed)

        // Assert
        val (after, rootId) = revealed
        val root = after.elements[rootId] as SchemaElement.Attribute
        assertEquals("Endereco", root.name)
        assertEquals(2, root.childAttributeIds.size)
        val c1 = after.elements[root.childAttributeIds[0]] as SchemaElement.Attribute
        val c2 = after.elements[root.childAttributeIds[1]] as SchemaElement.Attribute
        assertEquals("Rua", c1.name)
        assertEquals("Num", c2.name)
    }

    @Test
    fun `hiding each composite child separately keeps parent composite until all are ocultos`() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 102, 66))
        val child1 = SchemaElement.Attribute(
            id = 3,
            name = "Rua",
            position = ElementPosition(200, 10, 73, 16),
            ownerId = 2,
        )
        val child2 = SchemaElement.Attribute(
            id = 4,
            name = "Num",
            position = ElementPosition(200, 30, 73, 16),
            ownerId = 2,
        )
        val comp = SchemaElement.Attribute(
            id = 2,
            name = "Endereco",
            position = ElementPosition(120, 10, 73, 16),
            ownerId = 1,
            childAttributeIds = listOf(3, 4),
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to comp, 3 to child1, 4 to child2),
            connections = listOf(
                Connection(10, 2, 1, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(11, 3, 2, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(12, 4, 2, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 50,
        )

        // Act — hide first bar child
        val afterFirst = applyHideCanvasAttribute(schema, CanvasSelection.Element(3))
        assertNotNull(afterFirst)
        val parentAfterFirst = afterFirst.elements[2] as SchemaElement.Attribute
        assertTrue(parentAfterFirst.isComposite)
        assertEquals(listOf(4), parentAfterFirst.childAttributeIds)

        // Act — hide second bar child
        val afterSecond = applyHideCanvasAttribute(afterFirst, CanvasSelection.Element(4))
        assertNotNull(afterSecond)
        val parentAfterSecond = afterSecond.elements[2] as SchemaElement.Attribute

        // Assert — still composite (only ocultos under parent)
        assertTrue(parentAfterSecond.isComposite)
        assertTrue(parentAfterSecond.compostoPersisted)
        assertTrue(parentAfterSecond.childAttributeIds.isEmpty())
        assertEquals(2, parentAfterSecond.hiddenAttributes.size)
    }

    @Test
    fun `reveal materializes canvas children and keeps nested ocultos on the attribute`() {
        // Arrange
        val nested = HiddenAttribute(
            name = "só_oculto",
            type = "Int",
            isIdentifier = false,
            cardinality = AttributeCardinality(0, 0),
            position = ElementPosition(-1, -1, 0, 0),
        )
        val ent = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 102, 66))
        val child = SchemaElement.Attribute(
            id = 3,
            name = "Visivel",
            position = ElementPosition(200, 10, 73, 16),
            ownerId = 2,
        )
        val comp = SchemaElement.Attribute(
            id = 2,
            name = "Comp",
            position = ElementPosition(120, 10, 73, 16),
            ownerId = 1,
            childAttributeIds = listOf(3),
            hiddenAttributes = listOf(nested),
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to comp, 3 to child),
            connections = listOf(
                Connection(10, 2, 1, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(11, 3, 2, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 50,
        )

        // Act
        val hidden = applyHideCanvasAttribute(schema, CanvasSelection.Element(2))
        assertNotNull(hidden)
        val revealed = applyRevealHiddenAttribute(hidden, 1, listOf(0))
        assertNotNull(revealed)

        // Assert
        val (after, rootId) = revealed
        val root = after.elements[rootId] as SchemaElement.Attribute
        assertEquals(1, root.childAttributeIds.size)
        assertEquals("Visivel", (after.elements[root.childAttributeIds.single()] as SchemaElement.Attribute).name)
        assertEquals(1, root.hiddenAttributes.size)
        assertEquals("só_oculto", root.hiddenAttributes.single().name)
    }

    @Test
    fun `hide reveal preserves isOptional`() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 102, 66))
        val attr = SchemaElement.Attribute(
            id = 2,
            name = "Opt",
            position = ElementPosition(120, 10, 73, 16),
            ownerId = 1,
            isOptional = true,
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to attr),
            connections = listOf(
                Connection(10, 2, 1, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 50,
        )

        // Act
        val hidden = applyHideCanvasAttribute(schema, CanvasSelection.Element(2))
        assertNotNull(hidden)
        val revealed = applyRevealHiddenAttribute(hidden, 1, listOf(0))
        assertNotNull(revealed)

        // Assert
        val back = revealed.first.elements[revealed.second] as SchemaElement.Attribute
        assertTrue(back.isOptional)
    }

    @Test
    fun `reveal rejects non root path so composite subtree cannot be revealed alone`() {
        // Arrange — synthetic oculto composite (as stored after hiding a canvas composite)
        val childLeaf = HiddenAttribute(
            name = "Rua",
            type = "",
            isIdentifier = false,
            cardinality = AttributeCardinality(0, 0),
            position = ElementPosition(-1, -1, 0, 0),
        )
        val compositeHidden = HiddenAttribute(
            name = "Endereco",
            type = "",
            isIdentifier = false,
            cardinality = AttributeCardinality(0, 0),
            position = ElementPosition(120, 10, 73, 16),
            children = listOf(childLeaf),
        )
        val ent = SchemaElement.Entity(
            id = 1,
            name = "E",
            position = ElementPosition(0, 0, 102, 66),
            hiddenAttributes = listOf(compositeHidden),
        )
        val schema = ConceptualSchema(elements = mapOf(1 to ent), nextId = 10)

        // Act & Assert
        assertNull(applyRevealHiddenAttribute(schema, 1, listOf(0, 0)))
        assertNotNull(applyRevealHiddenAttribute(schema, 1, listOf(0)))
    }

    @Test
    fun `canRevealHiddenAttributeMenu allows only top level hidden index`() {
        // Arrange
        val childLeaf = HiddenAttribute(
            name = "Rua",
            type = "",
            isIdentifier = false,
            cardinality = AttributeCardinality(0, 0),
            position = ElementPosition(-1, -1, 0, 0),
        )
        val compositeHidden = HiddenAttribute(
            name = "Endereco",
            type = "",
            isIdentifier = false,
            cardinality = AttributeCardinality(0, 0),
            position = ElementPosition(120, 10, 73, 16),
            children = listOf(childLeaf),
        )
        val ent = SchemaElement.Entity(
            id = 1,
            name = "E",
            position = ElementPosition(0, 0, 102, 66),
            hiddenAttributes = listOf(compositeHidden),
        )
        val schema = ConceptualSchema(elements = mapOf(1 to ent), nextId = 10)

        // Act & Assert
        assertFalse(
            canRevealHiddenAttributeMenu(schema, CanvasSelection.Element(1), listOf(0, 0)),
        )
        assertTrue(
            canRevealHiddenAttributeMenu(schema, CanvasSelection.Element(1), listOf(0)),
        )
    }
}
