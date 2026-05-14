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

import games.polyclub.power.brmodelo.domain.SchemaElement

internal object McpConceptualToolElementResponseJson {

    fun elementSummary(element: SchemaElement): String {
        val pos = element.position
        val posJson =
            """{"x":${pos.x},"y":${pos.y},"width":${pos.width},"height":${pos.height}}"""
        val style = element.labelStyle
        val styleJson =
            """{"color":${style.color?.let { "$it" } ?: "null"},"bold":${style.bold},"italic":${style.italic}}"""
        val hiddenCount = element.hiddenAttributes.size
        val base =
            """"id":${element.id},"name":${jsonString(element.name)},"position":$posJson,"observations":${jsonString(element.observations)},"dictionary":${jsonString(element.dictionary)},"labelStyle":$styleJson,"hiddenAttributeCount":$hiddenCount"""
        return when (element) {
            is SchemaElement.Entity -> {
                """{$base,"elementKind":"entity","isWeak":${element.isWeak},"specializationId":${element.specializationId?.let { "$it" } ?: "null"},"parentSpecializationIds":${intListJson(element.parentSpecializationIds)}}"""
            }
            is SchemaElement.Relationship -> {
                """{$base,"elementKind":"relationship","arrowDirectionCode":${element.arrowDirection.code},"showName":${element.showName}}"""
            }
            is SchemaElement.AssociativeEntity -> {
                """{$base,"elementKind":"associativeEntity","relationshipName":${jsonString(element.relationshipName)},"relationshipObservations":${jsonString(element.relationshipObservations)},"relationshipDictionary":${jsonString(element.relationshipDictionary)},"arrowDirectionCode":${element.arrowDirection.code}}"""
            }
            is SchemaElement.Specialization -> {
                """{$base,"elementKind":"specialization","baseEntityId":${element.baseEntityId},"specializationTypeCode":${element.type.code},"isPartial":${element.isPartial}}"""
            }
            else -> """{$base,"elementKind":"other"}"""
        }
    }

    private fun intListJson(values: List<Int>): String =
        values.joinToString(prefix = "[", postfix = "]", separator = ",")

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
