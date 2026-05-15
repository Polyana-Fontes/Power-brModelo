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

import games.polyclub.power.brmodelo.domain.ConceptualLinkConnectionOverridePatch
import games.polyclub.power.brmodelo.domain.ConceptualLinkPick
import games.polyclub.power.brmodelo.domain.ConceptualProceduralToolOverrides

internal object McpLinkObjectsToolArgs {

    fun parseEndPick(raw: Any?): ConceptualLinkPick? {
        if (raw !is Map<*, *>) return null
        @Suppress("UNCHECKED_CAST")
        val m = raw as Map<String, Any?>
        val id = intFromAny(m["elementId"]) ?: return null
        if (id < 0) return null
        val outer = boolFromAny(m["associativeOuterEntitySide"]) == true
        return ConceptualLinkPick(elementId = id, isAssociativeOuterEntitySide = outer)
    }

    /**
     * Returns null when absent or when every field is unset (all-null overrides).
     */
    fun parseRelationshipOverrides(raw: Any?): ConceptualProceduralToolOverrides? {
        if (raw !is Map<*, *>) return null
        @Suppress("UNCHECKED_CAST")
        val m = raw as Map<String, Any?>
        val o = ConceptualProceduralToolOverrides(
            name = trimmedStringOrNull(m["name"]),
            observations = rawStringOrNull(m["observations"]),
            dictionary = rawStringOrNull(m["dictionary"]),
            labelColorArgb = intFromAny(m["labelColorArgb"]),
            labelBold = boolFromAny(m["labelBold"]),
            labelItalic = boolFromAny(m["labelItalic"]),
            arrowDirectionCode = intFromAny(m["arrowDirectionCode"]),
            showName = boolFromAny(m["showName"]),
            allowDuplicateCanvasLabels = boolFromAny(m["allowDuplicateCanvasLabels"]),
        )
        return if (o == ConceptualProceduralToolOverrides()) null else o
    }

    /**
     * Returns a pair of (error code, list) when arguments are inconsistent; list is null on error.
     */
    fun parseConnectionOverrides(
        connectionOverridesRaw: Any?,
        connectionRaw: Any?,
    ): Pair<String?, List<ConceptualLinkConnectionOverridePatch>?> {
        if (connectionOverridesRaw != null && connectionRaw != null) {
            return "connection_and_connectionOverrides_mutually_exclusive" to null
        }
        val source: List<*> = when {
            connectionOverridesRaw is List<*> -> connectionOverridesRaw
            connectionRaw is Map<*, *> -> listOf(connectionRaw)
            connectionRaw != null -> return "connection_invalid_type" to null
            else -> return null to null
        }
        val out = ArrayList<ConceptualLinkConnectionOverridePatch>(source.size)
        for (item in source) {
            if (item !is Map<*, *>) return "connectionOverrides_item_invalid" to null
            @Suppress("UNCHECKED_CAST")
            out += connectionPatchFromMap(item as Map<String, Any?>)
        }
        return null to out
    }

    private fun connectionPatchFromMap(m: Map<String, Any?>): ConceptualLinkConnectionOverridePatch =
        ConceptualLinkConnectionOverridePatch(
            cardinalityCode = intFromAny(m["cardinalityCode"]),
            showCardinality = boolFromAny(m["showCardinality"]),
            orientationCode = intFromAny(m["orientationCode"]),
            cardinalityFixed = boolFromAny(m["cardinalityFixed"]),
            isWeak = boolFromAny(m["isWeak"]),
            cardinalityRole = rawStringOrNull(m["cardinalityRole"]),
            cardinalityObservations = rawStringOrNull(m["cardinalityObservations"]),
            cardinalityDictionary = rawStringOrNull(m["cardinalityDictionary"]),
            cardinalityAutoSize = boolFromAny(m["cardinalityAutoSize"]),
        )

    private fun intFromAny(v: Any?): Int? = when (v) {
        null -> null
        is Number -> v.toInt()
        is String -> v.toIntOrNull()
        else -> null
    }

    private fun boolFromAny(v: Any?): Boolean? = when (v) {
        null -> null
        is Boolean -> v
        is String -> v.toBooleanStrictOrNull() ?: v.toBoolean()
        else -> null
    }

    private fun trimmedStringOrNull(v: Any?): String? = when (v) {
        null -> null
        is String -> v.trim().ifEmpty { null }
        else -> v.toString().trim().ifEmpty { null }
    }

    private fun rawStringOrNull(v: Any?): String? = when (v) {
        null -> null
        is String -> v
        else -> v.toString()
    }
}
