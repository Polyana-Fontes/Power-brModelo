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
 * Optional per-connection field overrides after [validateAndBuildConceptualLink]
 * (MCP `tools__link_objects` / `tools__link_existing_endpoints`).
 * Patches are applied in **ascending new connection id** order, which matches creation order for two
 * relationship–entity legs from entity–entity linking (endA leg, then endB leg).
 */
data class ConceptualLinkConnectionOverridePatch(
    val cardinalityCode: Int? = null,
    val showCardinality: Boolean? = null,
    val orientationCode: Int? = null,
    val cardinalityFixed: Boolean? = null,
    val isWeak: Boolean? = null,
    val cardinalityRole: String? = null,
    val cardinalityObservations: String? = null,
    val cardinalityDictionary: String? = null,
    val cardinalityAutoSize: Boolean? = null,
)

sealed class ConceptualLinkObjectsMcpApplyResult {
    data class Ok(val schema: ConceptualSchema) : ConceptualLinkObjectsMcpApplyResult()

    data class Err(val code: String) : ConceptualLinkObjectsMcpApplyResult()
}

internal fun relationshipRhombusNameTaken(schema: ConceptualSchema, name: String, excludeId: Int): Boolean =
    schema.relationships.any { it.id != excludeId && it.name == name } ||
        schema.associativeEntities.any { it.id != excludeId && it.relationshipName == name } ||
        schema.selfRelationships.any { it.id != excludeId && it.name == name }

private fun mergeLabelStyleFromProceduralOverrides(base: LabelStyle, o: ConceptualProceduralToolOverrides): LabelStyle =
    base.copy(
        color = o.labelColorArgb ?: base.color,
        bold = o.labelBold ?: base.bold,
        italic = o.labelItalic ?: base.italic,
    )

private fun Connection.withOverridePatch(patch: ConceptualLinkConnectionOverridePatch): Pair<String?, Connection> {
    var c = this
    patch.cardinalityCode?.let { code ->
        val card = Cardinality.fromCode(code) ?: return "invalid_cardinality_code" to this
        c = c.copy(cardinality = card)
    }
    patch.showCardinality?.let { c = c.copy(showCardinality = it) }
    patch.orientationCode?.let { code ->
        c = c.copy(orientation = LineOrientation.fromCode(code))
    }
    patch.cardinalityFixed?.let { c = c.copy(cardinalityFixed = it) }
    patch.isWeak?.let { c = c.copy(isWeak = it) }
    patch.cardinalityRole?.let { c = c.copy(cardinalityRole = it) }
    patch.cardinalityObservations?.let { c = c.copy(cardinalityObservations = it) }
    patch.cardinalityDictionary?.let { c = c.copy(cardinalityDictionary = it) }
    patch.cardinalityAutoSize?.let { c = c.copy(cardinalityAutoSize = it) }
    return null to c
}

/**
 * Applies optional MCP overrides on top of a schema already produced by [validateAndBuildConceptualLink].
 *
 * @param connectionPatches When non-null, length must equal the number of new connections vs [schemaBefore]
 * (sorted by connection id ascending).
 */
fun applyConceptualLinkObjectsMcpPatches(
    schemaBefore: ConceptualSchema,
    linkedSchema: ConceptualSchema,
    relationshipOverrides: ConceptualProceduralToolOverrides?,
    connectionPatches: List<ConceptualLinkConnectionOverridePatch>?,
): ConceptualLinkObjectsMcpApplyResult {
    val oldConnIds = schemaBefore.connections.map { it.id }.toSet()
    val newConns = linkedSchema.connections.filter { it.id !in oldConnIds }.sortedBy { it.id }
    when {
        connectionPatches == null -> { }
        connectionPatches.size != newConns.size ->
            return ConceptualLinkObjectsMcpApplyResult.Err("connectionOverrides_length_mismatch")
        else -> {
            val replacements = mutableMapOf<Int, Connection>()
            for ((conn, patch) in newConns.zip(connectionPatches)) {
                val (err, updated) = conn.withOverridePatch(patch)
                if (err != null) return ConceptualLinkObjectsMcpApplyResult.Err(err)
                replacements[conn.id] = updated
            }
            val work = linkedSchema.copy(
                connections = linkedSchema.connections.map { replacements[it.id] ?: it },
            )
            return applyRelationshipElementOverrides(schemaBefore, work, relationshipOverrides)
        }
    }
    return applyRelationshipElementOverrides(schemaBefore, linkedSchema, relationshipOverrides)
}

private fun applyRelationshipElementOverrides(
    schemaBefore: ConceptualSchema,
    workIn: ConceptualSchema,
    relationshipOverrides: ConceptualProceduralToolOverrides?,
): ConceptualLinkObjectsMcpApplyResult {
    if (relationshipOverrides == null) {
        return ConceptualLinkObjectsMcpApplyResult.Ok(workIn)
    }
    val newElemIds = workIn.elements.keys.filter { it !in schemaBefore.elements.keys }
    var work = workIn
    val arrow = relationshipOverrides.arrowDirectionCode?.let { code ->
        ArrowDirection.fromCode(code).takeIf { it.code == code }
            ?: return ConceptualLinkObjectsMcpApplyResult.Err("invalid_arrow_direction_code")
    }
    for (id in newElemIds) {
        when (val el = work.elements[id]) {
            is SchemaElement.Relationship -> {
                val name = when (val o = relationshipOverrides.name) {
                    null -> el.name
                    else -> {
                        val t = o.trim()
                        if (t.isEmpty()) {
                            return ConceptualLinkObjectsMcpApplyResult.Err("name_blank")
                        }
                        t
                    }
                }
                if (relationshipRhombusNameTaken(work, name, id)) {
                    return ConceptualLinkObjectsMcpApplyResult.Err("name_conflict")
                }
                work = work.withElement(
                    el.copy(
                        name = name,
                        observations = relationshipOverrides.observations ?: el.observations,
                        dictionary = relationshipOverrides.dictionary ?: el.dictionary,
                        labelStyle = mergeLabelStyleFromProceduralOverrides(el.labelStyle, relationshipOverrides),
                        arrowDirection = arrow ?: el.arrowDirection,
                        showName = relationshipOverrides.showName ?: el.showName,
                    ),
                )
            }
            is SchemaElement.SelfRelationship -> {
                val name = when (val o = relationshipOverrides.name) {
                    null -> el.name
                    else -> {
                        val t = o.trim()
                        if (t.isEmpty()) {
                            return ConceptualLinkObjectsMcpApplyResult.Err("name_blank")
                        }
                        t
                    }
                }
                if (relationshipRhombusNameTaken(work, name, id)) {
                    return ConceptualLinkObjectsMcpApplyResult.Err("name_conflict")
                }
                work = work.withElement(
                    el.copy(
                        name = name,
                        observations = relationshipOverrides.observations ?: el.observations,
                        dictionary = relationshipOverrides.dictionary ?: el.dictionary,
                        labelStyle = mergeLabelStyleFromProceduralOverrides(el.labelStyle, relationshipOverrides),
                        arrowDirection = arrow ?: el.arrowDirection,
                    ),
                )
            }
            else -> { }
        }
    }
    return ConceptualLinkObjectsMcpApplyResult.Ok(work)
}
