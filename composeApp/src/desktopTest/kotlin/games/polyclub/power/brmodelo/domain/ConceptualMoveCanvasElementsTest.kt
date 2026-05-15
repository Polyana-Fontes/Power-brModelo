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

class ConceptualMoveCanvasElementsTest {

    @Test
    fun `expandCanvasElementMoveSet includes owned attributes recursively`() {
        // Arrange
        val e1 = SchemaElement.Entity(id = 1, name = "E", position = ElementPosition(0, 0, 100, 66))
        val a2 = SchemaElement.Attribute(
            id = 2,
            name = "A",
            position = ElementPosition(10, 10, 50, 16),
            ownerId = 1,
        )
        val a3 = SchemaElement.Attribute(
            id = 3,
            name = "Child",
            position = ElementPosition(12, 30, 40, 16),
            ownerId = 2,
        )
        val schema = ConceptualSchema(
            name = "M",
            elements = mapOf(1 to e1, 2 to a2, 3 to a3),
        )

        // Act
        val expanded = expandCanvasElementMoveSet(schema, setOf(1), moveOwnedCanvasAttributes = true)

        // Assert
        assertEquals(setOf(1, 2, 3), expanded)
    }

    @Test
    fun `applyMoveCanvasElementsByTranslation moves owner and optional owned attributes`() {
        // Arrange
        val e1 = SchemaElement.Entity(id = 1, name = "E", position = ElementPosition(0, 0, 100, 66))
        val a2 = SchemaElement.Attribute(
            id = 2,
            name = "A",
            position = ElementPosition(10, 10, 50, 16),
            ownerId = 1,
        )
        val schema = ConceptualSchema(
            name = "M",
            elements = mapOf(1 to e1, 2 to a2),
        )

        // Act
        val r = applyMoveCanvasElementsByTranslation(schema, listOf(1), 5, -3, moveOwnedCanvasAttributes = true)

        // Assert
        val ok = assertIs<ConceptualMoveCanvasElementsApplyResult.Ok>(r)
        assertEquals(setOf(1, 2), ok.movedElementIds)
        assertEquals(5, (ok.schema.elements[1] as SchemaElement.Entity).position.x)
        assertEquals(-3, (ok.schema.elements[1] as SchemaElement.Entity).position.y)
        assertEquals(15, (ok.schema.elements[2] as SchemaElement.Attribute).position.x)
        assertEquals(7, (ok.schema.elements[2] as SchemaElement.Attribute).position.y)
    }

    @Test
    fun `applyMoveCanvasElementsByTranslation returns Err on zero delta`() {
        // Arrange
        val e1 = SchemaElement.Entity(id = 1, name = "E", position = ElementPosition(0, 0, 100, 66))
        val schema = ConceptualSchema(name = "M", elements = mapOf(1 to e1))

        // Act
        val r = applyMoveCanvasElementsByTranslation(schema, listOf(1), 0, 0, moveOwnedCanvasAttributes = false)

        // Assert
        val err = assertIs<ConceptualMoveCanvasElementsApplyResult.Err>(r)
        assertEquals("delta_zero", err.code)
    }

    @Test
    fun `applyMoveCanvasElementsByTranslation does not expand attributes when disabled`() {
        // Arrange
        val e1 = SchemaElement.Entity(id = 1, name = "E", position = ElementPosition(0, 0, 100, 66))
        val a2 = SchemaElement.Attribute(
            id = 2,
            name = "A",
            position = ElementPosition(10, 10, 50, 16),
            ownerId = 1,
        )
        val schema = ConceptualSchema(name = "M", elements = mapOf(1 to e1, 2 to a2))

        // Act
        val r = applyMoveCanvasElementsByTranslation(schema, listOf(1), 1, 1, moveOwnedCanvasAttributes = false)

        // Assert
        val ok = assertIs<ConceptualMoveCanvasElementsApplyResult.Ok>(r)
        assertEquals(setOf(1), ok.movedElementIds)
        assertEquals(10, (ok.schema.elements[2] as SchemaElement.Attribute).position.x)
        assertTrue(ok.schema.elements[2]!!.position.y == 10)
    }
}
