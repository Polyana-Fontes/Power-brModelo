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
import kotlin.test.assertTrue

class ExpandElementIdsForSubsetRasterExportTest {

    @Test
    fun `expandElementIdsForSubsetRasterExport includes attribute tree for entity seed`() {
        // Arrange
        val entityPos = ElementPosition(0, 0, 80, 40)
        val attrPos = ElementPosition(0, 50, 60, 20)
        val entity = SchemaElement.Entity(id = 1, name = "E1", position = entityPos)
        val attr = SchemaElement.Attribute(id = 2, name = "A1", position = attrPos, ownerId = 1)
        val schema = ConceptualSchema(
            elements = linkedMapOf(1 to entity, 2 to attr),
            nextId = 3,
        )

        // Act
        val expanded = expandElementIdsForSubsetRasterExport(schema, listOf(1))

        // Assert
        assertEquals(setOf(1, 2), expanded)
    }

    @Test
    fun `expandElementIdsForSubsetRasterExport drops unknown ids`() {
        // Arrange
        val entityPos = ElementPosition(0, 0, 80, 40)
        val entity = SchemaElement.Entity(id = 1, name = "E1", position = entityPos)
        val schema = ConceptualSchema(
            elements = linkedMapOf(1 to entity),
            nextId = 2,
        )

        // Act
        val expanded = expandElementIdsForSubsetRasterExport(schema, listOf(1, 999))

        // Assert
        assertEquals(setOf(1), expanded)
    }

    @Test
    fun `expandElementIdsForSubsetRasterExport returns empty when no valid seeds`() {
        // Arrange
        val entityPos = ElementPosition(0, 0, 80, 40)
        val entity = SchemaElement.Entity(id = 1, name = "E1", position = entityPos)
        val schema = ConceptualSchema(
            elements = linkedMapOf(1 to entity),
            nextId = 2,
        )

        // Act
        val expanded = expandElementIdsForSubsetRasterExport(schema, listOf(42))

        // Assert
        assertTrue(expanded.isEmpty())
    }
}
