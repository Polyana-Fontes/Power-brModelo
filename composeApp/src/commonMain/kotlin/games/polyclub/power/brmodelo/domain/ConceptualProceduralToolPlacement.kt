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
 * Which conceptual canvas object to insert using the same placement rules as the editor tools,
 * without driving the interactive tool state machine.
 */
enum class ConceptualProceduralToolKind {
    ENTITY,
    RELATIONSHIP,
    ASSOCIATIVE_ENTITY,
}

/**
 * Optional field overrides applied after the automatic name / geometry allocation from [placeConceptualItem].
 * A property is `null` when the caller wants to keep the freshly allocated default.
 */
data class ConceptualProceduralToolOverrides(
    val name: String? = null,
    val observations: String? = null,
    val dictionary: String? = null,
    val isWeak: Boolean? = null,
    val labelColorArgb: Int? = null,
    val labelBold: Boolean? = null,
    val labelItalic: Boolean? = null,
    val relationshipName: String? = null,
    val relationshipObservations: String? = null,
    val relationshipDictionary: String? = null,
    val arrowDirectionCode: Int? = null,
    val showName: Boolean? = null,
)

sealed class ConceptualProceduralToolPlacementResult {
    data class Ok(
        val schema: ConceptualSchema,
        val element: SchemaElement,
    ) : ConceptualProceduralToolPlacementResult()

    data class Err(val code: String) : ConceptualProceduralToolPlacementResult()
}

private fun entityLikeDisplayNameTaken(schema: ConceptualSchema, name: String, excludeId: Int): Boolean =
    schema.entities.any { it.id != excludeId && it.name == name } ||
        schema.associativeEntities.any { it.id != excludeId && it.name == name }

private fun relationshipStyleNameTaken(schema: ConceptualSchema, name: String, excludeId: Int): Boolean =
    schema.relationships.any { it.id != excludeId && it.name == name } ||
        schema.associativeEntities.any { it.id != excludeId && it.relationshipName == name } ||
        schema.selfRelationships.any { it.id != excludeId && it.name == name }

private fun mergeLabelStyle(base: LabelStyle, overrides: ConceptualProceduralToolOverrides): LabelStyle =
    base.copy(
        color = overrides.labelColorArgb ?: base.color,
        bold = overrides.labelBold ?: base.bold,
        italic = overrides.labelItalic ?: base.italic,
    )

/**
 * Inserts an entity, relationship, or associative entity using [placeConceptualItem], then applies
 * [ConceptualProceduralToolOverrides]. Does not create connections — agents link objects in later steps.
 */
fun ConceptualSchema.placeProceduralConceptualTool(
    kind: ConceptualProceduralToolKind,
    topLeftX: Int,
    topLeftY: Int,
    overrides: ConceptualProceduralToolOverrides,
): ConceptualProceduralToolPlacementResult {
    val placementKind = when (kind) {
        ConceptualProceduralToolKind.ENTITY -> ConceptualPlacementKind.PlainEntity
        ConceptualProceduralToolKind.RELATIONSHIP -> ConceptualPlacementKind.Relationship
        ConceptualProceduralToolKind.ASSOCIATIVE_ENTITY -> ConceptualPlacementKind.AssociativeEntity
    }
    val (schemaPlaced, id) = placeConceptualItem(placementKind, topLeftX, topLeftY)
    val baseEl = schemaPlaced.elements[id]
        ?: return ConceptualProceduralToolPlacementResult.Err("placement_internal_missing_element")

    val arrow = overrides.arrowDirectionCode?.let { code ->
        ArrowDirection.fromCode(code).takeIf { it.code == code }
            ?: return ConceptualProceduralToolPlacementResult.Err("invalid_arrow_direction_code")
    }

    val updated: SchemaElement = when (baseEl) {
        is SchemaElement.Entity -> {
            val name = when (val o = overrides.name) {
                null -> baseEl.name
                else -> {
                    val t = o.trim()
                    if (t.isEmpty()) {
                        return ConceptualProceduralToolPlacementResult.Err("name_blank")
                    }
                    t
                }
            }
            if (entityLikeDisplayNameTaken(schemaPlaced, name, id)) {
                return ConceptualProceduralToolPlacementResult.Err("name_conflict")
            }
            baseEl.copy(
                name = name,
                observations = overrides.observations ?: baseEl.observations,
                dictionary = overrides.dictionary ?: baseEl.dictionary,
                isWeak = overrides.isWeak ?: baseEl.isWeak,
                labelStyle = mergeLabelStyle(baseEl.labelStyle, overrides),
            )
        }
        is SchemaElement.Relationship -> {
            val name = when (val o = overrides.name) {
                null -> baseEl.name
                else -> {
                    val t = o.trim()
                    if (t.isEmpty()) {
                        return ConceptualProceduralToolPlacementResult.Err("name_blank")
                    }
                    t
                }
            }
            if (relationshipStyleNameTaken(schemaPlaced, name, id)) {
                return ConceptualProceduralToolPlacementResult.Err("name_conflict")
            }
            baseEl.copy(
                name = name,
                observations = overrides.observations ?: baseEl.observations,
                dictionary = overrides.dictionary ?: baseEl.dictionary,
                labelStyle = mergeLabelStyle(baseEl.labelStyle, overrides),
                arrowDirection = arrow ?: baseEl.arrowDirection,
                showName = overrides.showName ?: baseEl.showName,
            )
        }
        is SchemaElement.AssociativeEntity -> {
            val outerName = when (val o = overrides.name) {
                null -> baseEl.name
                else -> {
                    val t = o.trim()
                    if (t.isEmpty()) {
                        return ConceptualProceduralToolPlacementResult.Err("name_blank")
                    }
                    t
                }
            }
            if (entityLikeDisplayNameTaken(schemaPlaced, outerName, id)) {
                return ConceptualProceduralToolPlacementResult.Err("name_conflict")
            }
            val innerName = when (val o = overrides.relationshipName) {
                null -> baseEl.relationshipName
                else -> {
                    val t = o.trim()
                    if (t.isEmpty()) {
                        return ConceptualProceduralToolPlacementResult.Err("relationship_name_blank")
                    }
                    t
                }
            }
            if (relationshipStyleNameTaken(schemaPlaced, innerName, id)) {
                return ConceptualProceduralToolPlacementResult.Err("relationship_name_conflict")
            }
            baseEl.copy(
                name = outerName,
                observations = overrides.observations ?: baseEl.observations,
                dictionary = overrides.dictionary ?: baseEl.dictionary,
                labelStyle = mergeLabelStyle(baseEl.labelStyle, overrides),
                relationshipName = innerName,
                relationshipObservations = overrides.relationshipObservations ?: baseEl.relationshipObservations,
                relationshipDictionary = overrides.relationshipDictionary ?: baseEl.relationshipDictionary,
                arrowDirection = arrow ?: baseEl.arrowDirection,
            )
        }
        else -> return ConceptualProceduralToolPlacementResult.Err("unexpected_element_kind")
    }

    val outSchema = schemaPlaced.withElement(updated)
    return ConceptualProceduralToolPlacementResult.Ok(outSchema, updated)
}
