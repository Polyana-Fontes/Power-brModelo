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

class McpModelXmlPatchTest {

    @Test
    fun `single occurrence patch succeeds`() {
        // Arrange
        val xml = "<MER><a>1</a></MER>"

        // Act
        val (out, err) = McpModelXmlPatch.applyXmlStringPatch(xml, "<a>1</a>", "<a>2</a>", replaceAll = false)

        // Assert
        assertNull(err)
        assertEquals("<MER><a>2</a></MER>", out)
    }

    @Test
    fun `non unique old_string fails when replaceAll false`() {
        // Arrange
        val xml = "<x>aa</x>"

        // Act
        val (out, err) = McpModelXmlPatch.applyXmlStringPatch(xml, "a", "b", replaceAll = false)

        // Assert
        assertNull(out)
        assertEquals("old_string_not_unique", err)
    }

    @Test
    fun `replace all replaces every occurrence`() {
        // Arrange
        val xml = "<t>a|a</t>"

        // Act
        val (out, err) = McpModelXmlPatch.applyXmlStringPatch(xml, "a", "z", replaceAll = true)

        // Assert
        assertNull(err)
        assertEquals("<t>z|z</t>", out)
    }
}
