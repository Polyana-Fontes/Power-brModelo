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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class McpResourceTextOpsTest {

    @Test
    fun `indexToLineColumn1 maps newline boundaries`() {
        // Arrange
        val text = "a\nb"

        // Act
        val atB = McpResourceTextOps.indexToLineColumn1(text, 2)

        // Assert
        assertEquals(2, atB.line1)
        assertEquals(1, atB.column1)
    }

    @Test
    fun `sliceLines1Based returns inclusive line window`() {
        // Arrange
        val text = "L1\nL2\nL3"

        // Act
        val (slice, err) = McpResourceTextOps.sliceLines1Based(text, 2, 3)

        // Assert
        assertNull(err)
        assertEquals("L2\nL3", slice)
    }

    @Test
    fun `sliceByCharRange uses exclusive end index`() {
        // Arrange
        val text = "abcdef"

        // Act
        val (slice, err) = McpResourceTextOps.sliceByCharRange(text, 1, 4)

        // Assert
        assertNull(err)
        assertEquals("bcd", slice)
    }

    @Test
    fun `findAllLiteral is non overlapping`() {
        // Arrange
        val text = "aaaa"

        // Act
        val (matches, err) = McpResourceTextOps.findAllLiteral(text, "aa")

        // Assert
        assertNull(err)
        assertEquals(2, matches.size)
        assertEquals(0, matches[0].startIndex)
        assertEquals(2, matches[0].endIndexExclusive)
        assertEquals(2, matches[1].startIndex)
        assertEquals(4, matches[1].endIndexExclusive)
    }

    @Test
    fun `findAllLiteral reports line and column for each match`() {
        // Arrange
        val text = "x\nfoo\n"

        // Act
        val (matches, err) = McpResourceTextOps.findAllLiteral(text, "foo")

        // Assert
        assertNull(err)
        val m = assertNotNull(matches.single())
        assertEquals(2, m.startLine1)
        assertEquals(2, m.endLine1)
        assertEquals("foo", m.match)
        assertTrue(m.startColumn1 >= 1)
        assertTrue(m.endColumn1 >= 1)
    }

    @Test
    fun `findAllRegex returns spans and errors on bad pattern`() {
        // Arrange
        val text = "a1\nb2"

        // Act
        val (matches, err) = McpResourceTextOps.findAllRegex(text, """\d+""", dotMatchesAll = false)

        // Assert
        assertNull(err)
        assertEquals(2, matches.size)
        assertEquals("1", matches[0].match)
        assertEquals("2", matches[1].match)

        // Act & Assert
        val (_, bad) = McpResourceTextOps.findAllRegex(text, "(unclosed", dotMatchesAll = false)
        assertNotNull(bad)
        assertTrue(bad!!.startsWith("invalid_regex:"))
    }

    @Test
    fun `findAllRegex dotMatchesAll spans newline`() {
        // Arrange
        val text = "start\nend"

        // Act
        val (matches, err) = McpResourceTextOps.findAllRegex(text, "start.end", dotMatchesAll = true)

        // Assert
        assertNull(err)
        val m = assertNotNull(matches.single())
        assertEquals("start\nend", m.match)
        assertEquals(0, m.startIndex)
        assertEquals(9, m.endIndexExclusive)
    }
}
