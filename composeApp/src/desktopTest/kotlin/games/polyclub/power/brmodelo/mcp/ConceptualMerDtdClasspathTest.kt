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

package games.polyclub.power.brmodelo.mcp

import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertTrue

class ConceptualMerDtdClasspathTest {

    @Test
    fun `conceptual mer dtd is on the desktop classpath`() {
        // Arrange
        val path = "mcp/conceptual-mer.dtd"

        // Act
        val text = ConceptualMerDtdClasspathTest::class.java.classLoader
            .getResourceAsStream(path)
            ?.bufferedReader(StandardCharsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()

        // Assert
        assertTrue(text.contains("<!ELEMENT MER"), "DTD should declare root MER")
        assertTrue(text.contains("brModelo conceptual schema"), "DTD should carry documentation comments")
    }
}
