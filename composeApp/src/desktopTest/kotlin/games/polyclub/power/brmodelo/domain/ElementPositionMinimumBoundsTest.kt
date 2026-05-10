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

class ElementPositionMinimumBoundsTest {

    @Test
    fun coercedToMinimumDimensions_clampsTinyLegacySizes() {
        // Arrange
        val tiny = ElementPosition(x = 0, y = 0, width = 2, height = 3)

        // Act
        val coerced = tiny.coercedToMinimumDimensions()

        // Assert
        assertEquals(ElementPosition.MIN_WIDTH_PX, coerced.width)
        assertEquals(ElementPosition.MIN_HEIGHT_PX, coerced.height)
        assertEquals(0, coerced.x)
        assertEquals(0, coerced.y)
    }

    @Test
    fun withCoercedMinimumDimensions_normalizesLoadedSchema() {
        // Arrange
        val ent = SchemaElement.Entity(
            id = 1,
            name = "E",
            position = ElementPosition(10, 10, width = 1, height = 4),
        )
        val raw = ConceptualSchema(elements = mapOf(1 to ent), nextId = 2)

        // Act
        val normalized = raw.withCoercedMinimumDimensions()

        // Assert
        assertEquals(ElementPosition.MIN_WIDTH_PX, normalized.elements[1]!!.position.width)
        assertEquals(ElementPosition.MIN_HEIGHT_PX, normalized.elements[1]!!.position.height)
    }
}
