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

import games.polyclub.power.brmodelo.domain.AttributeCardinality
import games.polyclub.power.brmodelo.domain.ConceptualAttributeAttachPonto
import games.polyclub.power.brmodelo.domain.ConceptualAttributeToolVariant
import games.polyclub.power.brmodelo.domain.ConceptualSimpleAttributePlacementOverrides
import games.polyclub.power.brmodelo.domain.ConceptualCompositeLeafSpec
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.HiddenAttribute

internal object McpAttributeToolArgs {

    fun parseAttachSide(raw: Any?): ConceptualAttributeAttachPonto? {
        if (raw == null) return null
        val s = raw.toString().trim().lowercase()
        return when (s) {
            "left", "1" -> ConceptualAttributeAttachPonto.LEFT
            "top", "2" -> ConceptualAttributeAttachPonto.TOP
            "right", "3" -> ConceptualAttributeAttachPonto.RIGHT
            "bottom", "4" -> ConceptualAttributeAttachPonto.BOTTOM
            else -> null
        }
    }

    fun parseSimpleVariant(raw: Any?): ConceptualAttributeToolVariant? {
        if (raw == null) return null
        return when (raw.toString().trim().lowercase()) {
            "basic" -> ConceptualAttributeToolVariant.Basic
            "identifier", "id" -> ConceptualAttributeToolVariant.Identifier
            "multivalued", "multi" -> ConceptualAttributeToolVariant.MultiValued
            "optional", "opc" -> ConceptualAttributeToolVariant.Optional
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun parseSimpleOverrides(raw: Any?): ConceptualSimpleAttributePlacementOverrides? {
        if (raw !is Map<*, *>) return null
        val m = raw as Map<String, Any?>
        fun intOrNull(key: String): Int? = when (val v = m[key]) {
            null -> null
            is Int -> v
            is Long -> v.toInt()
            is Number -> v.toInt()
            else -> v.toString().toIntOrNull()
        }
        fun boolOrNull(key: String): Boolean? = when (val v = m[key]) {
            null -> null
            is Boolean -> v
            else -> v.toString().lowercase() == "true"
        }
        return ConceptualSimpleAttributePlacementOverrides(
            name = (m["name"] as? String)?.trim()?.takeIf { it.isNotEmpty() },
            observations = m["observations"]?.toString(),
            dictionary = m["dictionary"]?.toString(),
            valueType = (m["valueType"] as? String)?.trim()?.takeIf { it.isNotEmpty() },
            complement = m["complement"]?.toString(),
            minCardinality = intOrNull("minCardinality"),
            maxCardinality = intOrNull("maxCardinality"),
            isIdentifier = boolOrNull("isIdentifier"),
            isOptional = boolOrNull("isOptional"),
            isMultiValued = boolOrNull("isMultiValued"),
        ).takeIf { o ->
            o.name != null || o.observations != null || o.dictionary != null || o.valueType != null ||
                o.complement != null || o.minCardinality != null || o.maxCardinality != null ||
                o.isIdentifier != null || o.isOptional != null || o.isMultiValued != null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun parseCompositeLeafSpecs(raw: Any?): List<ConceptualCompositeLeafSpec>? {
        if (raw !is List<*>) return null
        val out = ArrayList<ConceptualCompositeLeafSpec>(raw.size)
        for (item in raw) {
            if (item !is Map<*, *>) return null
            val m = item as Map<String, Any?>
            out.add(
                ConceptualCompositeLeafSpec(
                    name = (m["name"] as? String)?.trim()?.takeIf { it.isNotEmpty() },
                    observations = m["observations"]?.toString(),
                    dictionary = m["dictionary"]?.toString(),
                    valueType = (m["valueType"] as? String)?.trim()?.takeIf { it.isNotEmpty() },
                    complement = m["complement"]?.toString(),
                ),
            )
        }
        return out
    }

    @Suppress("UNCHECKED_CAST")
    fun parseHiddenAttributeTree(raw: Any?): HiddenAttribute? {
        if (raw !is Map<*, *>) return null
        return parseHiddenAttributeMap(raw as Map<String, Any?>)
    }

    @Suppress("UNCHECKED_CAST")
    fun parseHiddenRoots(raw: Any?): List<HiddenAttribute>? {
        if (raw !is List<*>) return null
        val out = ArrayList<HiddenAttribute>(raw.size)
        for (item in raw) {
            if (item !is Map<*, *>) return null
            val h = parseHiddenAttributeMap(item as Map<String, Any?>) ?: return null
            out.add(h)
        }
        return out
    }

    private fun parseHiddenAttributeMap(m: Map<String, Any?>): HiddenAttribute? {
        val name = (m["name"] as? String)?.trim() ?: return null
        val type = (m["type"] as? String)?.trim().orEmpty()
        val isIdentifier = m["isIdentifier"] as? Boolean == true
        val isOptional = m["isOptional"] as? Boolean == true
        val minC = intFrom(m["minCardinality"]) ?: 0
        val maxC = intFrom(m["maxCardinality"]) ?: 0
        val position = parseElementPosition(m["position"]) ?: ElementPosition(-1, -1, 0, 0)
        val children = parseHiddenChildrenList(m["children"])
        val nested = parseHiddenChildrenList(m["nestedHiddenAttributes"])
        if (children == null || nested == null) return null
        return HiddenAttribute(
            name = name,
            type = type,
            isIdentifier = isIdentifier,
            cardinality = AttributeCardinality(minC, maxC),
            position = position,
            children = children,
            nestedHiddenAttributes = nested,
            isOptional = isOptional,
            observations = m["observations"]?.toString().orEmpty(),
            dictionary = m["dictionary"]?.toString().orEmpty(),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseHiddenChildrenList(raw: Any?): List<HiddenAttribute>? {
        if (raw == null) return emptyList()
        if (raw !is List<*>) return null
        val out = ArrayList<HiddenAttribute>(raw.size)
        for (item in raw) {
            if (item !is Map<*, *>) return null
            out.add(parseHiddenAttributeMap(item as Map<String, Any?>) ?: return null)
        }
        return out
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseElementPosition(raw: Any?): ElementPosition? {
        if (raw !is Map<*, *>) return null
        val m = raw as Map<String, Any?>
        val x = intFrom(m["x"]) ?: return null
        val y = intFrom(m["y"]) ?: return null
        val w = intFrom(m["width"]) ?: 0
        val h = intFrom(m["height"]) ?: 0
        return ElementPosition(x, y, w, h)
    }

    private fun intFrom(raw: Any?): Int? = when (raw) {
        null -> null
        is Int -> raw
        is Long -> raw.toInt()
        is Number -> raw.toInt()
        else -> raw.toString().toIntOrNull()
    }
}
