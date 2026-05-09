/*
 * KbrModelo - Kotlin port of brModelo 3.0 originally written in Pascal
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

package games.polyclub.kbrmodelo.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class AttributeCanonicalMultiValuedCountTest {

    private val dummyPosition get() = ElementPosition(x = 0, y = 0, width = 40, height = 20)

    @Test
    fun canonicalQtde_nonComposite_isZeroRegardlessOfStoredField() {
        // Arrange
        val attr = SchemaElement.Attribute(
            id = 1,
            name = "nome",
            position = dummyPosition,
            ownerId = 2,
            isMultiValued = true,
            multiValuedCount = 8,
            childAttributeIds = emptyList(),
        )
        val schema = ConceptualSchema(elements = mapOf(1 to attr))

        // Act
        val qtde = schema.canonicalQtdeMultivalorado(attr)

        // Assert
        assertEquals(0, qtde)
    }

    @Test
    fun canonicalQtde_composite_matchesSumOfVisibleChildLeaves() {
        // Arrange
        val child1 = SchemaElement.Attribute(
            id = 11,
            name = "a",
            position = dummyPosition,
            ownerId = 8,
            childAttributeIds = emptyList(),
        )
        val child2 = SchemaElement.Attribute(
            id = 14,
            name = "b",
            position = dummyPosition,
            ownerId = 8,
            childAttributeIds = emptyList(),
        )
        val composite = SchemaElement.Attribute(
            id = 8,
            name = "composto",
            position = dummyPosition,
            ownerId = 2,
            multiValuedCount = 1,
            childAttributeIds = listOf(11, 14),
        )
        val schema = ConceptualSchema(elements = mapOf(8 to composite, 11 to child1, 14 to child2))

        // Act
        val qtde = schema.canonicalQtdeMultivalorado(composite)

        // Assert
        assertEquals(2, qtde)
    }

    @Test
    fun canonicalQtde_composite_addsHiddenSubtreeLeavesAlongsideVisibleChildren() {
        // Arrange
        val hiddenLeaf = HiddenAttribute(
            name = "oculto",
            type = "Int",
            isIdentifier = false,
            cardinality = AttributeCardinality(1, 1),
            position = dummyPosition,
            children = emptyList(),
        )
        val visibleChild = SchemaElement.Attribute(
            id = 11,
            name = "visivel",
            position = dummyPosition,
            ownerId = 8,
            childAttributeIds = emptyList(),
        )
        val composite = SchemaElement.Attribute(
            id = 8,
            name = "composto",
            position = dummyPosition,
            ownerId = 2,
            hiddenAttributes = listOf(hiddenLeaf),
            childAttributeIds = listOf(11),
            multiValuedCount = 0,
        )
        val schema = ConceptualSchema(elements = mapOf(8 to composite, 11 to visibleChild))

        // Act
        val qtde = schema.canonicalQtdeMultivalorado(composite)

        // Assert — one visible leaf + one hidden leaf
        assertEquals(2, qtde)
    }

    @Test
    fun withNormalizedAttributeMultiValuedCounts_fixesStaleQtdeUsingCanonicalFormula() {
        // Arrange
        val child = SchemaElement.Attribute(
            id = 11,
            name = "a",
            position = dummyPosition,
            ownerId = 8,
            childAttributeIds = emptyList(),
        )
        val composite = SchemaElement.Attribute(
            id = 8,
            name = "composto",
            position = dummyPosition,
            ownerId = 2,
            multiValuedCount = 99,
            childAttributeIds = listOf(11),
        )
        val schema = ConceptualSchema(elements = mapOf(8 to composite, 11 to child))

        // Act
        val fixed = schema.withNormalizedAttributeMultiValuedCounts()

        // Assert
        val out = fixed.elements[8] as SchemaElement.Attribute
        assertEquals(1, out.multiValuedCount)
    }

    @Test
    fun physicalFieldLeafCount_nestedCompositeHidden_sumsLeaves() {
        // Arrange
        val inner = HiddenAttribute(
            name = "inner",
            type = "Texto",
            isIdentifier = false,
            cardinality = AttributeCardinality(1, 1),
            position = dummyPosition,
            children = emptyList(),
        )
        val compositeOculto = HiddenAttribute(
            name = "grupo",
            type = "",
            isIdentifier = false,
            cardinality = AttributeCardinality(0, 0),
            position = dummyPosition,
            children = listOf(inner, inner.copy(name = "inner2")),
        )

        // Act
        val n = compositeOculto.physicalFieldLeafCount()

        // Assert
        assertEquals(2, n)
    }
}
