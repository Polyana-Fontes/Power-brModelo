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

package games.polyclub.power.brmodelo.ui.canvas

import androidx.compose.ui.geometry.Offset
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.SchemaElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ConceptualLinkPickHitTestTest {

    @Test
    fun hitTestConceptualLinkPick_associative_inner_vs_outer() {
        // Arrange
        val pos = ElementPosition(0, 0, 100, 80)
        val assoc = SchemaElement.AssociativeEntity(id = 7, name = "EA", position = pos)
        val schema = ConceptualSchema(elements = mutableMapOf(7 to assoc))
        val inner = ElementPosition(
            x = pos.x + 15,
            y = pos.y + 15,
            width = (pos.width - 30).coerceAtLeast(10),
            height = (pos.height - 30).coerceAtLeast(10),
        )
        val cx = inner.x + inner.width / 2f
        val cy = inner.y + inner.height / 2f

        // Act
        val innerPick = hitTestConceptualLinkPick(schema, Offset(cx, cy))

        // Assert
        assertNotNull(innerPick)
        assertEquals(7, innerPick.elementId)
        assertEquals(false, innerPick.isAssociativeOuterEntitySide)

        // Act — corner of outer frame, outside inner rhombus
        val outerPick = hitTestConceptualLinkPick(schema, Offset(pos.x + 2f, pos.y + 2f))

        // Assert
        assertNotNull(outerPick)
        assertEquals(7, outerPick.elementId)
        assertEquals(true, outerPick.isAssociativeOuterEntitySide)
    }

    @Test
    fun relationshipDiamondContains_axisAlignedRhombus() {
        // Arrange
        val inner = ElementPosition(0, 0, 40, 40)

        // Act
        val centreInside = relationshipDiamondContains(inner, Offset(20f, 20f))
        val outsideTop = relationshipDiamondContains(inner, Offset(20f, -1f))

        // Assert
        assertEquals(true, centreInside)
        assertEquals(false, outsideTop)
    }

    @Test
    fun hitTestConceptualLinkPick_empty_returns_null() {
        // Arrange
        val schema = ConceptualSchema()

        // Act
        val pick = hitTestConceptualLinkPick(schema, Offset(10f, 10f))

        // Assert
        assertNull(pick)
    }
}
