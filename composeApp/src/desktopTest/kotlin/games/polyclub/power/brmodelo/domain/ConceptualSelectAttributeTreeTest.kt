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
import kotlin.test.assertTrue

class ConceptualSelectAttributeTreeTest {

    @Test
    fun expandSelection_addsAllAttributesUnderEntity() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 50, 50))
        val a1 = SchemaElement.Attribute(2, "A1", ElementPosition(100, 0, 40, 16), ownerId = 1)
        val a2 = SchemaElement.Attribute(3, "A2", ElementPosition(100, 30, 40, 16), ownerId = 1)
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to a1, 3 to a2),
            connections = listOf(
                Connection(10, 2, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(11, 3, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 20,
        )
        val sel = CanvasSelection.Element(1)

        // Act
        val next = expandCanvasSelectionWithAttributeTrees(schema, sel)

        // Assert
        val (e, _) = next.toMultiPickSets()
        assertEquals(setOf(1, 2, 3), e)
    }

    @Test
    fun canSelectAttributeTreeMenu_falseWhenTreeAlreadyFullySelected() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 50, 50))
        val a1 = SchemaElement.Attribute(2, "A1", ElementPosition(100, 0, 40, 16), ownerId = 1)
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to a1),
            connections = listOf(Connection(10, 2, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL)),
            nextId = 20,
        )
        val sel = CanvasSelection.Multiple(elementIds = setOf(1, 2))

        // Act & Assert
        assertFalse(canSelectAttributeTreeMenu(schema, sel))
    }

    @Test
    fun compositeSubtree_includedWhenSelectingComposite() {
        // Arrange
        val ent = SchemaElement.Entity(1, "E", ElementPosition(0, 0, 50, 50))
        val comp = SchemaElement.Attribute(
            id = 2,
            name = "C",
            position = ElementPosition(100, 0, 40, 16),
            ownerId = 1,
            childAttributeIds = listOf(3),
        )
        val child = SchemaElement.Attribute(3, "L", ElementPosition(200, 0, 30, 16), ownerId = 2)
        val schema = ConceptualSchema(
            elements = mapOf(1 to ent, 2 to comp, 3 to child),
            connections = listOf(
                Connection(10, 2, 1, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
                Connection(11, 3, 2, null, showCardinality = false, orientation = LineOrientation.VERTICAL),
            ),
            nextId = 20,
        )
        val sel = CanvasSelection.Element(2)

        // Act
        val next = expandCanvasSelectionWithAttributeTrees(schema, sel)

        // Assert
        val (e, _) = next.toMultiPickSets()
        assertEquals(setOf(2, 3), e)
        assertTrue(canSelectAttributeTreeMenu(schema, CanvasSelection.Element(1)))
    }
}
