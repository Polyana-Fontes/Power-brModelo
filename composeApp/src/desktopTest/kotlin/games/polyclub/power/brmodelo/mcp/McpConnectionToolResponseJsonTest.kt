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

import games.polyclub.power.brmodelo.domain.Cardinality
import games.polyclub.power.brmodelo.domain.Connection
import kotlin.test.Test
import kotlin.test.assertTrue

class McpConnectionToolResponseJsonTest {

    @Test
    fun linkObjectsToolSuccessJson_includesLinkPattern() {
        // Arrange
        val conn = Connection(
            id = 42,
            elementIdA = 9,
            elementIdB = 1,
            cardinality = Cardinality.ZERO_TO_MANY,
            showCardinality = true,
        )

        // Act
        val json = McpConnectionToolResponseJson.linkObjectsToolSuccessJson(
            resourceUri = "brmodelo://model/x.xml",
            newConnections = listOf(conn),
            newRelationship = null,
            newSelfRelationship = null,
            linkPattern = "relationship_entity_leg",
            dryRun = false,
        )

        // Assert
        assertTrue(json.contains("\"linkPattern\":\"relationship_entity_leg\""))
        assertTrue(!json.contains("\"dryRun\""))
    }

    @Test
    fun linkObjectsToolSuccessJson_dryRun_includesWouldCreate() {
        // Arrange
        val conn = Connection(id = 7, elementIdA = 2, elementIdB = 3, showCardinality = false)

        // Act
        val json = McpConnectionToolResponseJson.linkObjectsToolSuccessJson(
            resourceUri = "brmodelo://model/y.xml",
            newConnections = listOf(conn),
            newRelationship = null,
            newSelfRelationship = null,
            linkPattern = "entity_associative_outer_bridge",
            dryRun = true,
        )

        // Assert
        assertTrue(json.contains("\"dryRun\":true"))
        assertTrue(json.contains("\"wouldCreate\""))
        assertTrue(json.contains("\"newConnectionIds\":[7]"))
        assertTrue(json.contains("\"linkPattern\":\"entity_associative_outer_bridge\""))
    }
}
