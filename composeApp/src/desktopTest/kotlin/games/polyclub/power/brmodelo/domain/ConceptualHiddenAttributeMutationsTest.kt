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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConceptualHiddenAttributeMutationsTest {

    @Test
    fun `hiddenAttributeAtPath resolves nested merged indices`() {
        // Arrange
        val leaf = HiddenAttribute(
            name = "inner",
            type = "INT",
            isIdentifier = false,
            cardinality = AttributeCardinality(1, 0),
            position = ElementPosition(0, 0, 0, 0),
        )
        val root = HiddenAttribute(
            name = "root",
            type = "TEXT",
            isIdentifier = false,
            cardinality = AttributeCardinality(1, 0),
            position = ElementPosition(0, 0, 0, 0),
            children = listOf(leaf),
        )
        val roots = listOf(root)

        // Act
        val at0 = hiddenAttributeAtPath(roots, listOf(0))
        val at01 = hiddenAttributeAtPath(roots, listOf(0, 0))

        // Assert
        assertEquals("root", at0?.name)
        assertEquals("inner", at01?.name)
    }

    @Test
    fun `replaceHiddenAttributeAtPath swaps root and deep branch`() {
        // Arrange
        val inner = HiddenAttribute(
            name = "a",
            type = "INT",
            isIdentifier = false,
            cardinality = AttributeCardinality(1, 0),
            position = ElementPosition(0, 0, 0, 0),
        )
        val root = HiddenAttribute(
            name = "r",
            type = "TEXT",
            isIdentifier = false,
            cardinality = AttributeCardinality(1, 0),
            position = ElementPosition(0, 0, 0, 0),
            children = listOf(inner),
        )
        val replacementRoot = root.copy(name = "R2")
        val replacementInner = inner.copy(name = "A2")

        // Act
        val list1 = replaceHiddenAttributeAtPath(listOf(root), listOf(0), replacementRoot)
        val list2 = replaceHiddenAttributeAtPath(listOf(root), listOf(0, 0), replacementInner)

        // Assert
        assertEquals("R2", list1?.single()?.name)
        assertEquals("A2", list2?.single()?.children?.single()?.name)
    }

    @Test
    fun `hiddenAttributeForestNamesValid rejects blank and duplicate sibling names`() {
        // Arrange
        val ok = HiddenAttribute(
            name = "a",
            type = "T",
            isIdentifier = false,
            cardinality = AttributeCardinality(1, 0),
            position = ElementPosition(0, 0, 0, 0),
            children = listOf(
                HiddenAttribute(
                    name = "b",
                    type = "T",
                    isIdentifier = false,
                    cardinality = AttributeCardinality(1, 0),
                    position = ElementPosition(0, 0, 0, 0),
                ),
            ),
        )
        val dup = ok.copy(
            children = listOf(
                ok.children[0],
                ok.children[0].copy(name = "b"),
            ),
        )
        val blankRoot = ok.copy(name = "  ")

        // Act & Assert
        assertTrue(hiddenAttributeForestNamesValid(listOf(ok)))
        assertTrue(!hiddenAttributeForestNamesValid(listOf(dup)))
        assertTrue(!hiddenAttributeForestNamesValid(listOf(blankRoot)))
    }

    @Test
    fun `applyRemoveHiddenAttribute removes root oculto from entity`() {
        // Arrange
        val oculto = HiddenAttribute(
            name = "x",
            type = "T",
            isIdentifier = false,
            cardinality = AttributeCardinality(1, 0),
            position = ElementPosition(-1, -1, 0, 0),
        )
        val ent = SchemaElement.Entity(
            id = 1,
            name = "E",
            position = ElementPosition(0, 0, 100, 40),
            hiddenAttributes = listOf(oculto),
        )
        val schema = ConceptualSchema(elements = mapOf(1 to ent))

        // Act
        val next = applyRemoveHiddenAttribute(schema, 1, listOf(0))

        // Assert
        assertNotNull(next)
        assertTrue((next.elements[1] as SchemaElement.Entity).hiddenAttributes.isEmpty())
    }

    @Test
    fun `applyAppendHiddenAttribute then applyReplaceHiddenAttribute round trip`() {
        // Arrange
        val ent = SchemaElement.Entity(
            id = 1,
            name = "E",
            position = ElementPosition(0, 0, 100, 40),
            hiddenAttributes = emptyList(),
        )
        val schema = ConceptualSchema(elements = mapOf(1 to ent))
        val added = HiddenAttribute(
            name = "n1",
            type = "VARCHAR( )",
            isIdentifier = false,
            cardinality = AttributeCardinality(1, 0),
            position = ElementPosition(-1, -1, 0, 0),
            observations = "obs",
            dictionary = "dict",
        )

        // Act
        val withOne = applyAppendHiddenAttribute(schema, 1, added)!!
        val updated = applyReplaceHiddenAttribute(withOne, 1, listOf(0), added.copy(name = "n2"))!!

        // Assert
        val hid = (updated.elements[1] as SchemaElement.Entity).hiddenAttributes.single()
        assertEquals("n2", hid.name)
        assertEquals("obs", hid.observations)
        assertEquals("dict", hid.dictionary)
    }

    @Test
    fun `applyRemoveHiddenAttribute returns null for invalid path`() {
        // Arrange
        val ent = SchemaElement.Entity(
            id = 1,
            name = "E",
            position = ElementPosition(0, 0, 100, 40),
            hiddenAttributes = emptyList(),
        )
        val schema = ConceptualSchema(elements = mapOf(1 to ent))

        // Act
        val next = applyRemoveHiddenAttribute(schema, 1, listOf(0))

        // Assert
        assertNull(next)
    }
}
