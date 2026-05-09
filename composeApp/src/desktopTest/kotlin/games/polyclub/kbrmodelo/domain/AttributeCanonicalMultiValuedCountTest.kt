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

    @Test
    fun canonicalMultiValuedCount_nonComposite_isZeroRegardlessOfStoredField() {
        // Arrange
        val attr = SchemaElement.Attribute(
            id = 1,
            name = "nome",
            position = ElementPosition(x = 0, y = 0, width = 40, height = 20),
            ownerId = 2,
            isMultiValued = true,
            multiValuedCount = 8,
            childAttributeIds = emptyList(),
        )

        // Act
        val canonical = attr.canonicalMultiValuedCount

        // Assert
        assertEquals(0, canonical)
    }

    @Test
    fun canonicalMultiValuedCount_composite_matchesChildAttributeIdsSize() {
        // Arrange
        val attr = SchemaElement.Attribute(
            id = 1,
            name = "composto",
            position = ElementPosition(x = 0, y = 0, width = 40, height = 20),
            ownerId = 2,
            multiValuedCount = 1,
            childAttributeIds = listOf(10, 11, 12),
        )

        // Act
        val canonical = attr.canonicalMultiValuedCount

        // Assert
        assertEquals(3, canonical)
    }

    @Test
    fun withNormalizedAttributeMultiValuedCounts_fixesStaleQtdeOnSimpleAttribute() {
        // Arrange
        val bad = SchemaElement.Attribute(
            id = 1,
            name = "nome",
            position = ElementPosition(x = 0, y = 0, width = 40, height = 20),
            ownerId = 2,
            multiValuedCount = 8,
            childAttributeIds = emptyList(),
        )
        val schema = ConceptualSchema(elements = mapOf(1 to bad))

        // Act
        val fixed = schema.withNormalizedAttributeMultiValuedCounts()

        // Assert
        val out = fixed.elements[1] as SchemaElement.Attribute
        assertEquals(0, out.multiValuedCount)
    }
}
