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

package games.polyclub.power.brmodelo.domain

/**
 * Result of applying a conceptual property edit from MCP (mirrors inspector commits).
 */
sealed class ConceptualPropertyEditResult {
    data class Ok(val schema: ConceptualSchema) : ConceptualPropertyEditResult()
    data class Err(val code: String) : ConceptualPropertyEditResult()
}

private fun parseTrimmedString(v: Any?): String? =
    when (v) {
        null -> ""
        is String -> v.trim()
        else -> v.toString().trim()
    }

private fun parseRawString(v: Any?): String? =
    when (v) {
        null -> ""
        is String -> v
        else -> v.toString()
    }

private fun parseInt(v: Any?): Int? =
    when (v) {
        null -> null
        is Int -> v
        is Long -> v.toInt()
        is Double -> v.toInt()
        is Number -> v.toInt()
        else -> v.toString().toIntOrNull()
    }

private fun parseBoolean(v: Any?): Boolean? =
    when (v) {
        is Boolean -> v
        is String -> when (v.lowercase()) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> null
        }
        is Number -> v.toInt() != 0
        else -> null
    }

private fun containsKey(patch: Map<String, Any?>, key: String): Boolean = patch.containsKey(key)

private fun mergeLabelStyle(base: LabelStyle, patch: Map<String, Any?>): LabelStyle {
    var s = base
    if (containsKey(patch, "labelColorArgb")) {
        val raw = patch["labelColorArgb"]
        s = when (raw) {
            null -> s.copy(color = null)
            else -> parseInt(raw)?.let { s.copy(color = it) } ?: s
        }
    }
    parseBoolean(patch["labelBold"])?.let { s = s.copy(bold = it) }
    parseBoolean(patch["labelItalic"])?.let { s = s.copy(italic = it) }
    parseBoolean(patch["labelUnderline"])?.let { s = s.copy(underline = it) }
    parseBoolean(patch["labelStrikeThrough"])?.let { s = s.copy(strikeThrough = it) }
    if (containsKey(patch, "labelFontFamilyName")) {
        val t = parseTrimmedString(patch["labelFontFamilyName"])
        s = when {
            t == null -> s.copy(fontFamilyName = null)
            t.isEmpty() -> s.copy(fontFamilyName = null)
            else -> s.copy(fontFamilyName = t)
        }
    }
    if (containsKey(patch, "labelFontSizePoints")) {
        val raw = patch["labelFontSizePoints"]
        s = when (raw) {
            null -> s.copy(fontSizePoints = null)
            else -> parseInt(raw)?.let { s.copy(fontSizePoints = it) } ?: s
        }
    }
    if (containsKey(patch, "labelFontScript")) {
        val t = parseTrimmedString(patch["labelFontScript"])
        s = when {
            t == null -> s.copy(fontScript = null)
            t.isEmpty() -> s.copy(fontScript = null)
            else -> s.copy(fontScript = t)
        }
    }
    return s
}

private fun mergeElementPosition(base: ElementPosition, obj: Map<*, *>?): ElementPosition? {
    if (obj == null) return null
    @Suppress("UNCHECKED_CAST")
    val m = obj as Map<String, Any?>
    var p = base
    parseInt(m["x"])?.let { p = p.copy(x = it) }
    parseInt(m["y"])?.let { p = p.copy(y = it) }
    parseInt(m["width"])?.let { p = p.copy(width = it) }
    parseInt(m["height"])?.let { p = p.copy(height = it) }
    return p
}

@Suppress("CyclomaticComplexMethod")
private fun applyCommonElementPatches(el: SchemaElement, patch: Map<String, Any?>): SchemaElement? {
    var out = el
    if (containsKey(patch, "name")) {
        val n = parseTrimmedString(patch["name"]) ?: return null
        out = when (out) {
            is SchemaElement.Entity -> out.copy(name = n)
            is SchemaElement.Relationship -> out.copy(name = n)
            is SchemaElement.AssociativeEntity -> out.copy(name = n)
            is SchemaElement.Attribute -> out.copy(name = n)
            is SchemaElement.Specialization -> out.copy(name = n)
            is SchemaElement.SelfRelationship -> out.copy(name = n)
            is SchemaElement.Annotation -> out.copy(name = n)
        }
    }
    if (containsKey(patch, "observations")) {
        val o = parseRawString(patch["observations"]) ?: return null
        out = when (out) {
            is SchemaElement.Entity -> out.copy(observations = o)
            is SchemaElement.Relationship -> out.copy(observations = o)
            is SchemaElement.AssociativeEntity -> out.copy(observations = o)
            is SchemaElement.Attribute -> out.copy(observations = o)
            is SchemaElement.Specialization -> out.copy(observations = o)
            is SchemaElement.SelfRelationship -> out.copy(observations = o)
            is SchemaElement.Annotation -> out.copy(observations = o)
        }
    }
    if (containsKey(patch, "dictionary")) {
        val d = parseRawString(patch["dictionary"]) ?: return null
        out = when (out) {
            is SchemaElement.Entity -> out.copy(dictionary = d)
            is SchemaElement.Relationship -> out.copy(dictionary = d)
            is SchemaElement.AssociativeEntity -> out.copy(dictionary = d)
            is SchemaElement.Attribute -> out.copy(dictionary = d)
            is SchemaElement.Specialization -> out.copy(dictionary = d)
            is SchemaElement.SelfRelationship -> out.copy(dictionary = d)
            is SchemaElement.Annotation -> out.copy(dictionary = d)
        }
    }
    if (patch["position"] != null) {
        val posObj = patch["position"] as? Map<*, *> ?: return null
        val merged = mergeElementPosition(out.position, posObj) ?: return null
        out = when (out) {
            is SchemaElement.Entity -> out.copy(position = merged)
            is SchemaElement.Relationship -> out.copy(position = merged)
            is SchemaElement.AssociativeEntity -> out.copy(position = merged)
            is SchemaElement.Attribute -> out.copy(position = merged)
            is SchemaElement.Specialization -> out.copy(position = merged)
            is SchemaElement.SelfRelationship -> out.copy(position = merged)
            is SchemaElement.Annotation -> out.copy(position = merged)
        }
    }
    if (containsKey(patch, "labelColorArgb") || containsKey(patch, "labelBold") || containsKey(patch, "labelItalic") ||
        containsKey(patch, "labelUnderline") || containsKey(patch, "labelStrikeThrough") ||
        containsKey(patch, "labelFontFamilyName") || containsKey(patch, "labelFontSizePoints") ||
        containsKey(patch, "labelFontScript")
    ) {
        val ls = mergeLabelStyle(out.labelStyle, patch)
        out = when (out) {
            is SchemaElement.Entity -> out.copy(labelStyle = ls)
            is SchemaElement.Relationship -> out.copy(labelStyle = ls)
            is SchemaElement.AssociativeEntity -> out.copy(labelStyle = ls)
            is SchemaElement.Attribute -> out.copy(labelStyle = ls)
            is SchemaElement.Specialization -> out.copy(labelStyle = ls)
            is SchemaElement.SelfRelationship -> out.copy(labelStyle = ls)
            is SchemaElement.Annotation -> out.copy(labelStyle = ls)
        }
    }
    return out
}

private fun commonElementPatchKeys(): Set<String> =
    setOf(
        "name",
        "observations",
        "dictionary",
        "position",
        "labelColorArgb",
        "labelBold",
        "labelItalic",
        "labelUnderline",
        "labelStrikeThrough",
        "labelFontFamilyName",
        "labelFontSizePoints",
        "labelFontScript",
    )

private fun entityExtraKeys(): Set<String> = emptySet()

private fun relationshipExtraKeys(): Set<String> =
    setOf("arrowDirectionCode", "showName")

private fun associativeExtraKeys(): Set<String> =
    setOf(
        "relationshipName",
        "relationshipObservations",
        "relationshipDictionary",
        "arrowDirectionCode",
    )

private fun attributeExtraKeys(): Set<String> =
    setOf(
        "autoSize",
        "isIdentifier",
        "isOptional",
        "isMultiValued",
        "cardinalityMin",
        "cardinalityMax",
        "valueType",
        "complement",
    )

private fun specializationExtraKeys(): Set<String> = setOf("isPartial")

private fun selfRelationshipExtraKeys(): Set<String> = setOf("arrowDirectionCode")

private fun annotationExtraKeys(): Set<String> =
    setOf("annotationColorArgb", "annotationTypeCode", "alignmentCode", "autoSize")

private fun allowedKeysFor(kind: SchemaElement): Set<String> =
    commonElementPatchKeys() + when (kind) {
        is SchemaElement.Entity -> entityExtraKeys()
        is SchemaElement.Relationship -> relationshipExtraKeys()
        is SchemaElement.AssociativeEntity -> associativeExtraKeys()
        is SchemaElement.Attribute -> attributeExtraKeys()
        is SchemaElement.Specialization -> specializationExtraKeys()
        is SchemaElement.SelfRelationship -> selfRelationshipExtraKeys()
        is SchemaElement.Annotation -> annotationExtraKeys()
    }

/**
 * Applies model-level fields editable in the inspector (name, author, observations). [version] is ignored.
 */
fun applyEditConceptualModel(
    schema: ConceptualSchema,
    patch: Map<String, Any?>,
): ConceptualPropertyEditResult {
    val allowed = setOf("name", "author", "observations")
    val extra = patch.keys - allowed
    if (extra.isNotEmpty()) {
        return ConceptualPropertyEditResult.Err("field_not_applicable_to_model:${extra.first()}")
    }
    if (patch.isEmpty()) {
        return ConceptualPropertyEditResult.Err("patch_empty")
    }
    var s = schema
    if (containsKey(patch, "name")) {
        s = s.copy(name = parseTrimmedString(patch["name"]) ?: "")
    }
    if (containsKey(patch, "author")) {
        s = s.copy(author = parseRawString(patch["author"]) ?: "")
    }
    if (containsKey(patch, "observations")) {
        s = s.copy(observations = parseRawString(patch["observations"]) ?: "")
    }
    return ConceptualPropertyEditResult.Ok(s)
}

/**
 * Applies canvas element property patches for [elementId]. Caller runs attribute auto-size / composite relayout on the UI thread when needed.
 */
@Suppress("CyclomaticComplexMethod", "LongMethod")
fun applyEditCanvasElement(
    schema: ConceptualSchema,
    elementId: Int,
    patch: Map<String, Any?>,
): ConceptualPropertyEditResult {
    if (patch.isEmpty()) {
        return ConceptualPropertyEditResult.Err("patch_empty")
    }
    val base = schema.elements[elementId] ?: return ConceptualPropertyEditResult.Err("element_not_found")
    val allowed = allowedKeysFor(base)
    val unknown = patch.keys - allowed
    if (unknown.isNotEmpty()) {
        return ConceptualPropertyEditResult.Err("field_not_applicable_to_element_kind:${unknown.first()}")
    }

    var el = base
    applyCommonElementPatches(el, patch)?.let { el = it } ?: return ConceptualPropertyEditResult.Err("invalid_position_patch")

    when (el) {
        is SchemaElement.Entity -> Unit
        is SchemaElement.Relationship -> {
            if (containsKey(patch, "arrowDirectionCode")) {
                val code = parseInt(patch["arrowDirectionCode"])
                    ?: return ConceptualPropertyEditResult.Err("invalid_arrow_direction_code")
                el = el.copy(arrowDirection = ArrowDirection.fromCode(code))
            }
            if (containsKey(patch, "showName")) {
                val sh = parseBoolean(patch["showName"])
                    ?: return ConceptualPropertyEditResult.Err("invalid_boolean:showName")
                el = el.copy(showName = sh)
            }
        }
        is SchemaElement.AssociativeEntity -> {
            if (containsKey(patch, "relationshipName")) {
                el = el.copy(relationshipName = parseTrimmedString(patch["relationshipName"]) ?: "")
            }
            if (containsKey(patch, "relationshipObservations")) {
                el = el.copy(relationshipObservations = parseRawString(patch["relationshipObservations"]) ?: "")
            }
            if (containsKey(patch, "relationshipDictionary")) {
                el = el.copy(relationshipDictionary = parseRawString(patch["relationshipDictionary"]) ?: "")
            }
            if (containsKey(patch, "arrowDirectionCode")) {
                val code = parseInt(patch["arrowDirectionCode"])
                    ?: return ConceptualPropertyEditResult.Err("invalid_arrow_direction_code")
                el = el.copy(arrowDirection = ArrowDirection.fromCode(code))
            }
        }
        is SchemaElement.Attribute -> {
            if (containsKey(patch, "isMultiValued")) {
                val v = parseBoolean(patch["isMultiValued"])
                    ?: return ConceptualPropertyEditResult.Err("invalid_boolean:isMultiValued")
                el = el.copy(isMultiValued = v)
            }
            val wantsCard = containsKey(patch, "cardinalityMin") || containsKey(patch, "cardinalityMax")
            if (wantsCard && !el.isMultiValued) {
                return ConceptualPropertyEditResult.Err("cardinality_not_applicable_when_not_multi_valued")
            }
            if (containsKey(patch, "cardinalityMin") || containsKey(patch, "cardinalityMax")) {
                var min = el.cardinality.minCardinality
                var max = el.cardinality.maxCardinality
                parseInt(patch["cardinalityMin"])?.let { min = it }
                parseInt(patch["cardinalityMax"])?.let { max = it }
                el = el.copy(cardinality = AttributeCardinality(min, max))
            }
            if (containsKey(patch, "autoSize")) {
                val v = parseBoolean(patch["autoSize"])
                    ?: return ConceptualPropertyEditResult.Err("invalid_boolean:autoSize")
                el = el.copy(autoSize = v)
            }
            if (containsKey(patch, "isIdentifier")) {
                val v = parseBoolean(patch["isIdentifier"])
                    ?: return ConceptualPropertyEditResult.Err("invalid_boolean:isIdentifier")
                el = el.copy(isIdentifier = v)
            }
            if (containsKey(patch, "isOptional")) {
                val v = parseBoolean(patch["isOptional"])
                    ?: return ConceptualPropertyEditResult.Err("invalid_boolean:isOptional")
                el = el.copy(isOptional = v)
            }
            if (containsKey(patch, "valueType")) {
                el = el.copy(valueType = parseTrimmedString(patch["valueType"]) ?: "")
            }
            if (containsKey(patch, "complement")) {
                el = el.copy(complement = parseTrimmedString(patch["complement"]) ?: "")
            }
        }
        is SchemaElement.Specialization -> {
            if (containsKey(patch, "isPartial")) {
                val v = parseBoolean(patch["isPartial"])
                    ?: return ConceptualPropertyEditResult.Err("invalid_boolean:isPartial")
                el = el.copy(isPartial = v)
            }
        }
        is SchemaElement.SelfRelationship -> {
            if (containsKey(patch, "arrowDirectionCode")) {
                val code = parseInt(patch["arrowDirectionCode"])
                    ?: return ConceptualPropertyEditResult.Err("invalid_arrow_direction_code")
                el = el.copy(arrowDirection = ArrowDirection.fromCode(code))
            }
        }
        is SchemaElement.Annotation -> {
            if (containsKey(patch, "annotationColorArgb")) {
                val raw = patch["annotationColorArgb"]
                el = when (raw) {
                    null -> el.copy(color = null)
                    else -> {
                        val c = parseInt(raw) ?: return ConceptualPropertyEditResult.Err("invalid_annotation_color")
                        el.copy(color = c)
                    }
                }
            }
            if (containsKey(patch, "annotationTypeCode")) {
                val code = parseInt(patch["annotationTypeCode"])
                    ?: return ConceptualPropertyEditResult.Err("invalid_annotation_type_code")
                el = el.copy(annotationType = AnnotationType.fromCode(code))
            }
            if (containsKey(patch, "alignmentCode")) {
                val code = parseInt(patch["alignmentCode"])
                    ?: return ConceptualPropertyEditResult.Err("invalid_text_alignment_code")
                el = el.copy(alignment = TextAlignment.fromCode(code))
            }
            if (containsKey(patch, "autoSize")) {
                val v = parseBoolean(patch["autoSize"])
                    ?: return ConceptualPropertyEditResult.Err("invalid_boolean:autoSize")
                el = el.copy(autoSize = v)
            }
        }
    }

    return ConceptualPropertyEditResult.Ok(schema.withElement(el.withCoercedMinimumDimensions()))
}

private val connectionPatchKeys = setOf(
    "cardinalityCode",
    "showCardinality",
    "orientationCode",
    "cardinalityFixed",
    "isWeak",
    "cardinalityRole",
    "cardinalityObservations",
    "cardinalityDictionary",
    "cardinalityAutoSize",
    "cardinalityPosition",
    "useAssociativeOuterForEndA",
    "useAssociativeOuterForEndB",
)

/**
 * Applies connection property patches for [connectionId] (cardinality inspector fields).
 * When toggling fixed/floating cardinality, auto-size, or visibility, the UI layer should call
 * `withConnectionCardinalityInspectorParity` (see ui.canvas package) with the same TextMeasurer as the inspector.
 */
fun applyEditConnection(
    schema: ConceptualSchema,
    connectionId: Int,
    patch: Map<String, Any?>,
): ConceptualPropertyEditResult {
    if (patch.isEmpty()) {
        return ConceptualPropertyEditResult.Err("patch_empty")
    }
    val unknown = patch.keys - connectionPatchKeys
    if (unknown.isNotEmpty()) {
        return ConceptualPropertyEditResult.Err("field_not_applicable_to_connection:${unknown.first()}")
    }
    val idx = schema.connections.indexOfFirst { it.id == connectionId }
    if (idx < 0) {
        return ConceptualPropertyEditResult.Err("connection_not_found")
    }
    var c = schema.connections[idx]
    if (containsKey(patch, "cardinalityCode")) {
        val raw = patch["cardinalityCode"]
        c = when (raw) {
            null -> c.copy(cardinality = null)
            else -> {
                val code = parseInt(raw) ?: return ConceptualPropertyEditResult.Err("invalid_cardinality_code")
                val card = Cardinality.fromCode(code)
                    ?: return ConceptualPropertyEditResult.Err("invalid_cardinality_code")
                c.copy(cardinality = card)
            }
        }
    }
    if (containsKey(patch, "showCardinality")) {
        val v = parseBoolean(patch["showCardinality"])
            ?: return ConceptualPropertyEditResult.Err("invalid_boolean:showCardinality")
        c = c.copy(showCardinality = v)
    }
    if (containsKey(patch, "orientationCode")) {
        val code = parseInt(patch["orientationCode"])
            ?: return ConceptualPropertyEditResult.Err("invalid_orientation_code")
        c = c.copy(orientation = LineOrientation.fromCode(code))
    }
    if (containsKey(patch, "cardinalityFixed")) {
        val v = parseBoolean(patch["cardinalityFixed"])
            ?: return ConceptualPropertyEditResult.Err("invalid_boolean:cardinalityFixed")
        c = c.copy(cardinalityFixed = v)
    }
    if (containsKey(patch, "isWeak")) {
        val v = parseBoolean(patch["isWeak"])
            ?: return ConceptualPropertyEditResult.Err("invalid_boolean:isWeak")
        c = c.copy(isWeak = v)
    }
    if (containsKey(patch, "cardinalityRole")) {
        c = c.copy(cardinalityRole = parseTrimmedString(patch["cardinalityRole"]) ?: "")
    }
    if (containsKey(patch, "cardinalityObservations")) {
        c = c.copy(cardinalityObservations = parseRawString(patch["cardinalityObservations"]) ?: "")
    }
    if (containsKey(patch, "cardinalityDictionary")) {
        c = c.copy(cardinalityDictionary = parseRawString(patch["cardinalityDictionary"]) ?: "")
    }
    if (containsKey(patch, "cardinalityAutoSize")) {
        val v = parseBoolean(patch["cardinalityAutoSize"])
            ?: return ConceptualPropertyEditResult.Err("invalid_boolean:cardinalityAutoSize")
        c = c.copy(cardinalityAutoSize = v)
    }
    if (patch["cardinalityPosition"] != null) {
        val posObj = patch["cardinalityPosition"] as? Map<*, *>
            ?: return ConceptualPropertyEditResult.Err("invalid_cardinality_position_patch")
        val base = c.cardinalityPosition ?: ElementPosition(0, 0, Connection.DEFAULT_LABEL_WIDTH, Connection.DEFAULT_LABEL_HEIGHT)
        val merged = mergeElementPosition(base, posObj) ?: return ConceptualPropertyEditResult.Err("invalid_cardinality_position_patch")
        c = c.copy(cardinalityPosition = merged)
    }
    if (containsKey(patch, "useAssociativeOuterForEndA")) {
        val v = parseBoolean(patch["useAssociativeOuterForEndA"])
            ?: return ConceptualPropertyEditResult.Err("invalid_boolean:useAssociativeOuterForEndA")
        c = c.copy(useAssociativeOuterForEndA = v)
    }
    if (containsKey(patch, "useAssociativeOuterForEndB")) {
        val v = parseBoolean(patch["useAssociativeOuterForEndB"])
            ?: return ConceptualPropertyEditResult.Err("invalid_boolean:useAssociativeOuterForEndB")
        c = c.copy(useAssociativeOuterForEndB = v)
    }
    val nextConnections = schema.connections.toMutableList()
    nextConnections[idx] = c
    return ConceptualPropertyEditResult.Ok(schema.copy(connections = nextConnections))
}

private val hiddenAttributePatchKeys = setOf(
    "name",
    "type",
    "isIdentifier",
    "isOptional",
    "isMultiValued",
    "cardinalityMin",
    "cardinalityMax",
    "observations",
    "dictionary",
    "position",
)

/**
 * Replaces the hidden-attribute node at [path] on [holderElementId] with a merge of [patch] into the existing node.
 */
@Suppress("CyclomaticComplexMethod")
fun applyEditHiddenAttributeAtPath(
    schema: ConceptualSchema,
    holderElementId: Int,
    path: List<Int>,
    patch: Map<String, Any?>,
): ConceptualPropertyEditResult {
    if (patch.isEmpty()) {
        return ConceptualPropertyEditResult.Err("patch_empty")
    }
    val unknown = patch.keys - hiddenAttributePatchKeys
    if (unknown.isNotEmpty()) {
        return ConceptualPropertyEditResult.Err("field_not_applicable_to_hidden_attribute:${unknown.first()}")
    }
    val holder = schema.elements[holderElementId] ?: return ConceptualPropertyEditResult.Err("holder_element_not_found")
    val roots = when (holder) {
        is SchemaElement.Entity -> holder.hiddenAttributes
        is SchemaElement.Relationship -> holder.hiddenAttributes
        is SchemaElement.AssociativeEntity -> holder.hiddenAttributes
        is SchemaElement.Attribute -> holder.hiddenAttributes
        is SchemaElement.SelfRelationship -> holder.hiddenAttributes
        is SchemaElement.Specialization -> holder.hiddenAttributes
        is SchemaElement.Annotation -> holder.hiddenAttributes
    }
    val current = hiddenAttributeAtPath(roots, path) ?: return ConceptualPropertyEditResult.Err("hidden_attribute_path_not_found")
    var h = current.deepCopy()
    if (containsKey(patch, "isMultiValued")) {
        val mv = parseBoolean(patch["isMultiValued"])
            ?: return ConceptualPropertyEditResult.Err("invalid_boolean:isMultiValued")
        h = if (mv) {
            val max = if (h.cardinality.maxCardinality <= 0) 21 else h.cardinality.maxCardinality
            h.copy(cardinality = AttributeCardinality(h.cardinality.minCardinality.coerceAtLeast(0), max))
        } else {
            h.copy(cardinality = AttributeCardinality(0, 0))
        }
    }
    val wantsCard = containsKey(patch, "cardinalityMin") || containsKey(patch, "cardinalityMax")
    if (wantsCard && !h.isMultiValued) {
        return ConceptualPropertyEditResult.Err("cardinality_not_applicable_when_not_multi_valued")
    }
    if (containsKey(patch, "cardinalityMin") || containsKey(patch, "cardinalityMax")) {
        var min = h.cardinality.minCardinality
        var max = h.cardinality.maxCardinality
        parseInt(patch["cardinalityMin"])?.let { min = it }
        parseInt(patch["cardinalityMax"])?.let { max = it }
        h = h.copy(cardinality = AttributeCardinality(min, max))
    }
    if (containsKey(patch, "name")) {
        h = h.copy(name = parseTrimmedString(patch["name"]) ?: "")
    }
    if (containsKey(patch, "type")) {
        h = h.copy(type = parseTrimmedString(patch["type"]) ?: "")
    }
    if (containsKey(patch, "isIdentifier")) {
        val v = parseBoolean(patch["isIdentifier"])
            ?: return ConceptualPropertyEditResult.Err("invalid_boolean:isIdentifier")
        h = h.copy(isIdentifier = v)
    }
    if (containsKey(patch, "isOptional")) {
        val v = parseBoolean(patch["isOptional"])
            ?: return ConceptualPropertyEditResult.Err("invalid_boolean:isOptional")
        h = h.copy(isOptional = v)
    }
    if (containsKey(patch, "observations")) {
        h = h.copy(observations = parseRawString(patch["observations"]) ?: "")
    }
    if (containsKey(patch, "dictionary")) {
        h = h.copy(dictionary = parseRawString(patch["dictionary"]) ?: "")
    }
    if (patch["position"] != null) {
        val posObj = patch["position"] as? Map<*, *>
            ?: return ConceptualPropertyEditResult.Err("invalid_position_patch")
        val merged = mergeElementPosition(h.position, posObj)
            ?: return ConceptualPropertyEditResult.Err("invalid_position_patch")
        h = h.copy(position = merged)
    }
    val forest = replaceHiddenAttributeAtPath(roots, path, h)
        ?: return ConceptualPropertyEditResult.Err("hidden_attribute_path_not_found")
    if (!hiddenAttributeForestNamesValid(forest)) {
        return ConceptualPropertyEditResult.Err("hidden_attribute_names_invalid")
    }
    val updated = applyReplaceHiddenAttribute(schema, holderElementId, path, h)
        ?: return ConceptualPropertyEditResult.Err("hidden_attribute_apply_failed")
    return ConceptualPropertyEditResult.Ok(updated)
}
