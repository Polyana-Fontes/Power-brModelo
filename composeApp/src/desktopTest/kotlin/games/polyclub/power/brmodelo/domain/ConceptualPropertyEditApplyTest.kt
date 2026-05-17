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

class ConceptualPropertyEditApplyTest {

    @Test
    fun `applyEditConceptualModel updates name author observations`() {
        // Arrange
        val schema = ConceptualSchema(name = "A", author = "B", observations = "C")

        // Act
        val r = applyEditConceptualModel(
            schema,
            mapOf("name" to " X ", "author" to "Y", "observations" to "Z"),
        )

        // Assert
        val ok = assertIs<ConceptualPropertyEditResult.Ok>(r)
        assertEquals("X", ok.schema.name)
        assertEquals("Y", ok.schema.author)
        assertEquals("Z", ok.schema.observations)
    }

    @Test
    fun `applyEditConnection updates isWeak`() {
        // Arrange
        val c = Connection(id = 1, elementIdA = 10, elementIdB = 20, isWeak = false)
        val schema = ConceptualSchema(connections = listOf(c), nextId = 30)

        // Act
        val r = applyEditConnection(schema, 1, mapOf("isWeak" to true))

        // Assert
        val ok = assertIs<ConceptualPropertyEditResult.Ok>(r)
        assertEquals(true, ok.schema.connections.single().isWeak)
    }

    @Test
    fun `applyEditConnection updates cardinality label style`() {
        // Arrange
        val c = Connection(
            id = 1,
            elementIdA = 10,
            elementIdB = 20,
            cardinality = Cardinality.ZERO_TO_ONE,
            showCardinality = true,
        )
        val schema = ConceptualSchema(connections = listOf(c), nextId = 30)

        // Act
        val r = applyEditConnection(
            schema,
            1,
            mapOf(
                "labelFontFamilyName" to "Arial",
                "labelFontSizePoints" to 14,
                "labelBold" to true,
            ),
        )

        // Assert
        val ok = assertIs<ConceptualPropertyEditResult.Ok>(r)
        val st = ok.schema.connections.single().cardinalityLabelStyle
        assertEquals("Arial", st.fontFamilyName)
        assertEquals(14, st.fontSizePoints)
        assertEquals(true, st.bold)
    }

    @Test
    fun `applyEditConceptualModel rejects unknown patch keys`() {
        // Arrange
        val schema = ConceptualSchema()

        // Act
        val r = applyEditConceptualModel(schema, mapOf("version" to "9"))

        // Assert
        val err = assertIs<ConceptualPropertyEditResult.Err>(r)
        assertTrue(err.code.startsWith("field_not_applicable_to_model"))
    }

    @Test
    fun `applyEditCanvasElement rejects field not applicable to entity`() {
        // Arrange
        val e = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 80, 40))
        val schema = ConceptualSchema(elements = mapOf(1 to e), nextId = 2)

        // Act
        val r = applyEditCanvasElement(schema, 1, mapOf("showName" to false))

        // Assert
        val err = assertIs<ConceptualPropertyEditResult.Err>(r)
        assertEquals("field_not_applicable_to_element_kind:showName", err.code)
    }

    @Test
    fun `applyEditCanvasElement updates relationship showName and name`() {
        // Arrange
        val rel = SchemaElement.Relationship(2, "R", ElementPosition(10, 10, 60, 60), showName = true)
        val schema = ConceptualSchema(elements = mapOf(2 to rel), nextId = 3)

        // Act
        val r = applyEditCanvasElement(schema, 2, mapOf("name" to "Rel1", "showName" to false))

        // Assert
        val ok = assertIs<ConceptualPropertyEditResult.Ok>(r)
        val out = ok.schema.elements[2] as SchemaElement.Relationship
        assertEquals("Rel1", out.name)
        assertEquals(false, out.showName)
    }

    @Test
    fun `applyEditCanvasElement rejects cardinality when attribute not multi valued`() {
        // Arrange
        val attr = SchemaElement.Attribute(
            id = 5,
            name = "a",
            position = ElementPosition(0, 0, 40, 20),
            ownerId = 1,
            isMultiValued = false,
        )
        val schema = ConceptualSchema(elements = mapOf(5 to attr), nextId = 6)

        // Act
        val r = applyEditCanvasElement(schema, 5, mapOf("cardinalityMin" to 1))

        // Assert
        val err = assertIs<ConceptualPropertyEditResult.Err>(r)
        assertEquals("cardinality_not_applicable_when_not_multi_valued", err.code)
    }

    @Test
    fun `applyEditConnection returns connection_not_found for unknown id`() {
        // Arrange
        val schema = ConceptualSchema(connections = emptyList())

        // Act
        val r = applyEditConnection(schema, 99, mapOf("isWeak" to true))

        // Assert
        val err = assertIs<ConceptualPropertyEditResult.Err>(r)
        assertEquals("connection_not_found", err.code)
    }

    @Test
    fun `applyEditHiddenAttributeAtPath returns path error for empty path`() {
        // Arrange
        val e = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 80, 40))
        val schema = ConceptualSchema(elements = mapOf(1 to e), nextId = 2)

        // Act
        val r = applyEditHiddenAttributeAtPath(schema, 1, emptyList(), mapOf("name" to "x"))

        // Assert
        val err = assertIs<ConceptualPropertyEditResult.Err>(r)
        assertEquals("hidden_attribute_path_not_found", err.code)
    }

    @Test
    fun `applyEditHiddenAttributeAtPath rejects duplicate sibling names under same parent`() {
        // Arrange
        val c1 = HiddenAttribute(
            name = "X",
            type = "INT",
            isIdentifier = false,
            cardinality = AttributeCardinality(0, 0),
            position = ElementPosition(0, 0, 40, 20),
        )
        val c2 = HiddenAttribute(
            name = "Y",
            type = "INT",
            isIdentifier = false,
            cardinality = AttributeCardinality(0, 0),
            position = ElementPosition(0, 0, 40, 20),
        )
        val root = HiddenAttribute(
            name = "R",
            type = "INT",
            isIdentifier = false,
            cardinality = AttributeCardinality(0, 0),
            position = ElementPosition(0, 0, 40, 20),
            children = listOf(c1, c2),
        )
        val e = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 80, 40), hiddenAttributes = listOf(root))
        val schema = ConceptualSchema(elements = mapOf(1 to e), nextId = 2)

        // Act — second merged branch (index 1) renamed to collide with first
        val r = applyEditHiddenAttributeAtPath(schema, 1, listOf(0, 1), mapOf("name" to "X"))

        // Assert
        val err = assertIs<ConceptualPropertyEditResult.Err>(r)
        assertEquals("hidden_attribute_names_invalid", err.code)
    }
}
