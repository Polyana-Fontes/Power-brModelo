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

class McpLinkObjectsToolArgsTest {

    @Test
    fun `parseConnectionOverrides returns error when both connection and connectionOverrides set`() {
        // Arrange
        val list = listOf(mapOf("cardinalityCode" to 1))
        val single = mapOf("cardinalityCode" to 2)

        // Act
        val (err, patches) = McpLinkObjectsToolArgs.parseConnectionOverrides(list, single)

        // Assert
        assertEquals("connection_and_connectionOverrides_mutually_exclusive", err)
        assertNull(patches)
    }

    @Test
    fun `parseConnectionOverrides wraps single connection object as one patch`() {
        // Arrange
        val single = mapOf<String, Any?>("cardinalityCode" to 3, "showCardinality" to false)

        // Act
        val (err, patches) = McpLinkObjectsToolArgs.parseConnectionOverrides(null, single)

        // Assert
        assertNull(err)
        assertEquals(1, patches?.size)
        assertEquals(3, patches!![0].cardinalityCode)
        assertEquals(false, patches[0].showCardinality)
    }
}
