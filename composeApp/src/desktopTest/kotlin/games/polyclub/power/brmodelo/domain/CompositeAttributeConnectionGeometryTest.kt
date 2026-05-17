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

class CompositeAttributeConnectionGeometryTest {

    @Test
    fun `composite bar on right attaches child link on child left edge`() {
        // Arrange — composite uses ellipse-left layout (bar on physical right); child box to the right of the bar
        val child = ElementPosition(x = 50, y = 10, width = 80, height = 16)

        // Act
        val x = compositeChildBarConnectionX(child, compositeEllipseOnLeft = true)

        // Assert — segment meets the left edge, not through the label to the right of the ellipse
        assertEquals(50f, x)
    }

    @Test
    fun `composite bar on left OrientacaoD attaches child link on child right edge`() {
        // Arrange — bullet-right / "Direito": bar on composite's left; children sit to the left
        val child = ElementPosition(x = 10, y = 10, width = 72, height = 16)

        // Act
        val x = compositeChildBarConnectionX(child, compositeEllipseOnLeft = false)

        // Assert — right edge (ellipse side), facing the bar
        assertEquals(82f, x)
    }
}
