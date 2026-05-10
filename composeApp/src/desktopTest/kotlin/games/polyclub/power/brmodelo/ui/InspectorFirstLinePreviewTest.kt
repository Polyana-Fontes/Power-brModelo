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

package games.polyclub.power.brmodelo.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class InspectorFirstLinePreviewTest {

    @Test
    fun empty_returns_empty() {
        // Act & Assert
        assertEquals("", inspectorFirstLinePreview(""))
    }

    @Test
    fun no_newline_returns_full() {
        // Arrange
        val input = "hello world"

        // Act
        val result = inspectorFirstLinePreview(input)

        // Assert
        assertEquals(input, result)
    }

    @Test
    fun lf_takes_first_line() {
        // Arrange
        val input = "a\nb\nc"

        // Act
        val result = inspectorFirstLinePreview(input)

        // Assert
        assertEquals("a", result)
    }

    @Test
    fun crlf_takes_text_before_first_break() {
        // Arrange
        val input = "a\r\nb"

        // Act
        val result = inspectorFirstLinePreview(input)

        // Assert
        assertEquals("a", result)
    }

    @Test
    fun cr_only_splits() {
        // Arrange
        val input = "x\ry"

        // Act
        val result = inspectorFirstLinePreview(input)

        // Assert
        assertEquals("x", result)
    }
}
