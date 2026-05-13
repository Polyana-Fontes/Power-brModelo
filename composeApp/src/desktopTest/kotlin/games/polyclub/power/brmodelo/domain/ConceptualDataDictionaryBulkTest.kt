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
import kotlin.test.assertTrue

class ConceptualDataDictionaryBulkTest {

    @Test
    fun `canOpenBulkDataDictionaryForSelection requires at least one pick`() {
        // Act & Assert
        assertFalse(canOpenBulkDataDictionaryForSelection(CanvasSelection.None))
        assertTrue(canOpenBulkDataDictionaryForSelection(CanvasSelection.Element(1)))
        assertTrue(
            canOpenBulkDataDictionaryForSelection(
                CanvasSelection.Multiple(elementIds = setOf(1, 2), cardinalityConnectionIds = emptySet()),
            ),
        )
        assertTrue(canOpenBulkDataDictionaryForSelection(CanvasSelection.Cardinality(9)))
    }

    @Test
    fun `collectDictionarySlots includes main associative inner and nested hidden`() {
        // Arrange
        val pos = ElementPosition(0, 0, 40, 30)
        val filho = HiddenAttribute(
            name = "filho",
            type = "INT",
            isIdentifier = false,
            cardinality = AttributeCardinality(1, 0),
            position = ElementPosition(0, 0, 0, 0),
            dictionary = "dic-filho",
        )
        val pai = HiddenAttribute(
            name = "pai",
            type = "COMP",
            isIdentifier = false,
            cardinality = AttributeCardinality(1, 0),
            position = ElementPosition(0, 0, 0, 0),
            children = listOf(filho),
            dictionary = "dic-pai",
        )
        val avo = HiddenAttribute(
            name = "avô",
            type = "COMP",
            isIdentifier = false,
            cardinality = AttributeCardinality(1, 0),
            position = ElementPosition(0, 0, 0, 0),
            nestedHiddenAttributes = listOf(pai),
            dictionary = "dic-avô",
        )
        val ent = SchemaElement.Entity(
            id = 1,
            name = "Familia",
            position = pos,
            dictionary = "dic-ent",
            hiddenAttributes = listOf(avo),
        )
        val assoc = SchemaElement.AssociativeEntity(
            id = 2,
            name = "Ligacao",
            position = pos,
            dictionary = "outer",
            relationshipName = "Interno",
            relationshipDictionary = "inner-dic",
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to assoc),
            nextId = 10,
        )
        val selection = CanvasSelection.Multiple(elementIds = setOf(1, 2), cardinalityConnectionIds = emptySet())

        // Act
        val rows = collectDictionarySlotsForSelection(schema, selection)

        // Assert
        val keys = rows.map { it.key }
        assertTrue(keys.contains(ConceptualDictionarySlotKey.SchemaElementMain(1)))
        assertTrue(keys.contains(ConceptualDictionarySlotKey.SchemaElementMain(2)))
        assertTrue(keys.contains(ConceptualDictionarySlotKey.AssociativeInnerRelationship(2)))
        assertTrue(
            keys.contains(
                ConceptualDictionarySlotKey.HiddenAttributeNode(1, 0, emptyList()),
            ),
        )
        assertTrue(
            keys.contains(
                ConceptualDictionarySlotKey.HiddenAttributeNode(1, 0, listOf(0)),
            ),
        )
        assertTrue(
            keys.contains(
                ConceptualDictionarySlotKey.HiddenAttributeNode(1, 0, listOf(0, 0)),
            ),
        )
        val filhoRow = rows.first {
            it.key == ConceptualDictionarySlotKey.HiddenAttributeNode(1, 0, listOf(0, 0))
        }
        assertEquals("dic-filho", filhoRow.initialText)
        assertTrue("dentro de" in filhoRow.subtitle)
    }

    @Test
    fun `applyDictionarySlots updates main inner hidden and cardinality in one chain`() {
        // Arrange
        val pos = ElementPosition(0, 0, 40, 30)
        val hid = HiddenAttribute(
            name = "h",
            type = "INT",
            isIdentifier = false,
            cardinality = AttributeCardinality(1, 0),
            position = ElementPosition(0, 0, 0, 0),
            dictionary = "old-h",
        )
        val ent = SchemaElement.Entity(
            id = 1,
            name = "E",
            position = pos,
            dictionary = "e0",
            hiddenAttributes = listOf(hid),
        )
        val assoc = SchemaElement.AssociativeEntity(
            id = 2,
            name = "A",
            position = pos,
            dictionary = "a0",
            relationshipDictionary = "r0",
        )
        val conn = Connection(
            id = 3,
            elementIdA = 1,
            elementIdB = 2,
            cardinalityDictionary = "c0",
        )
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to assoc),
            connections = listOf(conn),
            nextId = 10,
        )
        val writes = listOf(
            ConceptualDictionarySlotKey.SchemaElementMain(1) to "e1",
            ConceptualDictionarySlotKey.AssociativeInnerRelationship(2) to "r1",
            ConceptualDictionarySlotKey.HiddenAttributeNode(1, 0, emptyList()) to "h1",
            ConceptualDictionarySlotKey.ConnectionCardinality(3) to "c1",
        )

        // Act
        val next = applyDictionarySlots(schema, writes)

        // Assert
        val out = assertNotNull(next)
        assertEquals("e1", (out.elements[1] as SchemaElement.Entity).dictionary)
        assertEquals("r1", (out.elements[2] as SchemaElement.AssociativeEntity).relationshipDictionary)
        assertEquals("h1", (out.elements[1] as SchemaElement.Entity).hiddenAttributes.single().dictionary)
        assertEquals("c1", out.connections.single { it.id == 3 }.cardinalityDictionary)
    }

    @Test
    fun `collect includes cardinality selection`() {
        // Arrange
        val pos = ElementPosition(0, 0, 40, 30)
        val e1 = SchemaElement.Entity(1, "A", pos)
        val e2 = SchemaElement.Entity(2, "B", pos)
        val conn = Connection(id = 5, elementIdA = 1, elementIdB = 2, cardinalityDictionary = "cd")
        val schema = ConceptualSchema(
            elements = mapOf(1 to e1, 2 to e2),
            connections = listOf(conn),
            nextId = 10,
        )
        val sel = CanvasSelection.Multiple(
            elementIds = emptySet(),
            cardinalityConnectionIds = setOf(5),
        )

        // Act
        val rows = collectDictionarySlotsForSelection(schema, sel)

        // Assert
        assertEquals(1, rows.size)
        assertEquals(
            ConceptualDictionarySlotKey.ConnectionCardinality(5),
            rows.single().key,
        )
    }
}
