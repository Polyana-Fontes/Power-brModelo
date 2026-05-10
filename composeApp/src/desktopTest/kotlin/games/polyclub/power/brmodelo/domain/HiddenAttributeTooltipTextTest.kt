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
import kotlin.test.assertNull

class HiddenAttributeTooltipTextTest {

    @Test
    fun `hiddenAttributesTooltipText returns null for empty roots`() {
        // Act
        val t = hiddenAttributesTooltipText(emptyList())

        // Assert
        assertNull(t)
    }

    @Test
    fun `hiddenAttributesTooltipText formats single root and nested branches`() {
        // Arrange
        val inner = HiddenAttribute(
            name = "B",
            type = "",
            isIdentifier = false,
            cardinality = AttributeCardinality(0, 0),
            position = ElementPosition(0, 0, 0, 0),
        )
        val root = HiddenAttribute(
            name = "A",
            type = "",
            isIdentifier = false,
            cardinality = AttributeCardinality(0, 0),
            position = ElementPosition(0, 0, 0, 0),
            children = listOf(inner),
        )

        // Act
        val t = hiddenAttributesTooltipText(listOf(root))

        // Assert
        val expected = """
            Atributos Ocultos:
            └── A
                └── B
        """.trimIndent()
        assertEquals(expected, t)
    }

    @Test
    fun `hiddenAttributesTooltipText interleaves children then nestedHiddenAttributes`() {
        // Arrange
        val ch = HiddenAttribute(
            name = "child",
            type = "",
            isIdentifier = false,
            cardinality = AttributeCardinality(0, 0),
            position = ElementPosition(0, 0, 0, 0),
        )
        val nest = HiddenAttribute(
            name = "nested",
            type = "",
            isIdentifier = false,
            cardinality = AttributeCardinality(0, 0),
            position = ElementPosition(0, 0, 0, 0),
        )
        val root = HiddenAttribute(
            name = "root",
            type = "",
            isIdentifier = false,
            cardinality = AttributeCardinality(0, 0),
            position = ElementPosition(0, 0, 0, 0),
            children = listOf(ch),
            nestedHiddenAttributes = listOf(nest),
        )

        // Act
        val t = hiddenAttributesTooltipText(listOf(root))

        // Assert
        val expected = """
            Atributos Ocultos:
            └── root
                ├── child
                └── nested
        """.trimIndent()
        assertEquals(expected, t)
    }

    @Test
    fun `hiddenAttributesTooltipText formats multiple roots as forest`() {
        // Arrange
        val r1 = HiddenAttribute(
            name = "X",
            type = "",
            isIdentifier = false,
            cardinality = AttributeCardinality(0, 0),
            position = ElementPosition(0, 0, 0, 0),
        )
        val r2 = HiddenAttribute(
            name = "Y",
            type = "",
            isIdentifier = false,
            cardinality = AttributeCardinality(0, 0),
            position = ElementPosition(0, 0, 0, 0),
        )

        // Act
        val t = hiddenAttributesTooltipText(listOf(r1, r2))

        // Assert
        val expected = """
            Atributos Ocultos:
            ├── X
            └── Y
        """.trimIndent()
        assertEquals(expected, t)
    }
}
