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

class AnnotationBackgroundColorPresetsTest {

    @Test
    fun `labelForColorRef maps clSkyBlue decimal to Azul céu`() {
        // Arrange
        val xmlValor = 15_780_518

        // Act
        val label = AnnotationBackgroundColorPresets.labelForColorRef(xmlValor)

        // Assert
        assertEquals("Azul céu", label)
    }

    @Test
    fun `colorRefForMenuSelection round trips preset labels`() {
        // Arrange
        val entry = AnnotationBackgroundColorPresets.ENTRIES.first { it.label == "Branco" }

        // Act
        val back = AnnotationBackgroundColorPresets.colorRefForMenuSelection(entry.label)

        // Assert
        assertEquals(entry.colorRef, back)
    }

    @Test
    fun `colorRefForMenuSelection parses decimal string like XML Valor`() {
        // Act
        val ref = AnnotationBackgroundColorPresets.colorRefForMenuSelection("15780518")

        // Assert
        assertEquals(15_780_518, ref)
    }

    @Test
    fun `DEFAULT matches valores padroes XML sample`() {
        // Assert — valores-padroes.xml `<Cor Valor="15780518"/>`
        assertEquals(15_780_518, AnnotationBackgroundColorPresets.DEFAULT_COLOR_REF)
        assertNotNull(AnnotationBackgroundColorPresets.ENTRIES.find { it.colorRef == 15_780_518 })
    }

    @Test
    fun `ENTRIES align with VclTColorTable default TColorBox order and values`() {
        // Arrange
        val expected = VclTColorTable.defaultColorBoxPresets

        // Act
        val actual = AnnotationBackgroundColorPresets.ENTRIES

        // Assert
        assertEquals(expected.size, actual.size)
        expected.forEachIndexed { i, n ->
            assertEquals(n.constant, actual[i].constant, message = "constant at index $i")
            assertEquals(n.colorRef, actual[i].colorRef, message = "colorRef at index $i")
        }
    }

    @Test
    fun `default TColorBox preset count is 51`() {
        // Assert — 16 standard + 4 extended + 31 system (Lazarus graphics.pp; no clNone or clDefault)
        assertEquals(51, VclTColorTable.defaultColorBoxPresets.size)
    }
}
