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
import games.polyclub.power.brmodelo.domain.SchemaElement

internal object McpConnectionToolResponseJson {

    fun connectionSummary(conn: Connection): String {
        val cardCode = conn.cardinality?.let { c ->
            when (c) {
                Cardinality.ONE_TO_ONE -> 1
                Cardinality.ZERO_TO_ONE -> 2
                Cardinality.ONE_TO_MANY -> 3
                Cardinality.ZERO_TO_MANY -> 4
            }
        }
        val cardJson = cardCode?.toString() ?: "null"
        val cardLabel = conn.cardinality?.let { jsonString(it.label) } ?: "null"
        return """{"id":${conn.id},"elementIdA":${conn.elementIdA},"elementIdB":${conn.elementIdB},"cardinalityCode":$cardJson,"cardinalityLabel":$cardLabel,"showCardinality":${conn.showCardinality},"cardinalityFixed":${conn.cardinalityFixed},"isWeak":${conn.isWeak},"orientationCode":${conn.orientation.code},"cardinalityRole":${jsonString(conn.cardinalityRole)},"cardinalityObservations":${jsonString(conn.cardinalityObservations)},"cardinalityDictionary":${jsonString(conn.cardinalityDictionary)},"cardinalityAutoSize":${conn.cardinalityAutoSize},"useAssociativeOuterForEndA":${conn.useAssociativeOuterForEndA},"useAssociativeOuterForEndB":${conn.useAssociativeOuterForEndB}}"""
    }

    fun linkObjectsToolSuccessJson(
        resourceUri: String,
        newConnections: List<Connection>,
        newRelationship: SchemaElement.Relationship?,
        newSelfRelationship: SchemaElement.SelfRelationship?,
    ): String {
        val connsJson = newConnections.joinToString(prefix = "[", postfix = "]") { connectionSummary(it) }
        val relJson = newRelationship?.let { """{"element":${McpConceptualToolElementResponseJson.elementSummary(it)}}""" } ?: "null"
        val selfJson = newSelfRelationship?.let { """{"element":${McpConceptualToolElementResponseJson.elementSummary(it)}}""" } ?: "null"
        return """{"ok":true,"resourceUri":${jsonString(resourceUri)},"newConnections":$connsJson,"newRelationship":$relJson,"newSelfRelationship":$selfJson}"""
    }

    private fun jsonString(s: String): String {
        val escaped = buildString(s.length + 8) {
            append('"')
            for (ch in s) {
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (ch.code < 32) {
                        append("\\u")
                        append(ch.code.and(0xffff).toString(16).padStart(4, '0'))
                    } else {
                        append(ch)
                    }
                }
            }
            append('"')
        }
        return escaped
    }
}
