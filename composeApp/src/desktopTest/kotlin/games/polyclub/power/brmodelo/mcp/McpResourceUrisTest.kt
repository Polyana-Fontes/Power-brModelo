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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class McpResourceUrisTest {

    @Test
    fun `modelResourceUri roundtrips through parse`() {
        // Arrange
        val uri = modelResourceUri(4)

        // Act
        val index = parseModelResourceTabIndex(uri)

        // Assert
        assertEquals(4, index)
    }

    @Test
    fun `parses tab index from canonical uri`() {
        // Arrange
        val uri = "brmodelo://model/2"

        // Act
        val index = parseModelResourceTabIndex(uri)

        // Assert
        assertEquals(2, index)
    }

    @Test
    fun `parses tab index ignoring path suffix and query`() {
        // Arrange
        val uri = "brmodelo://model/0/extra?x=1"

        // Act
        val index = parseModelResourceTabIndex(uri)

        // Assert
        assertEquals(0, index)
    }

    @Test
    fun `returns null for unrelated uri`() {
        // Arrange
        val uri = "file:///tmp/model.xml"

        // Act
        val index = parseModelResourceTabIndex(uri)

        // Assert
        assertNull(index)
    }

    @Test
    fun `conceptual mer dtd resource uri is stable`() {
        // Act
        val uri = conceptualMerDtdResourceUri()

        // Assert
        assertEquals("brmodelo://schema/conceptual-mer.dtd", uri)
    }
}
