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
 * IDs of [SchemaElement.Relationship] picks in [selection] (ignores cardinality-only picks).
 */
fun relationshipIdsSelectedForPromote(schema: ConceptualSchema, selection: CanvasSelection): Set<Int> {
    val (e, _) = selection.toMultiPickSets()
    return e.mapNotNull { id -> if (schema.elements[id] is SchemaElement.Relationship) id else null }.toSet()
}

/** True when **Operações → Promover à Entidade Associativa** applies. */
fun canPromoteToAssociativeEntityMenu(schema: ConceptualSchema, selection: CanvasSelection): Boolean =
    relationshipIdsSelectedForPromote(schema, selection).isNotEmpty()

/**
 * Replaces each selected [SchemaElement.Relationship] with an [SchemaElement.AssociativeEntity] **keeping the same id**,
 * so all [ConceptualSchema.connections] and attribute owner links stay valid. The inner relationship keeps the old name,
 * dictionary, observations, and arrow; the outer entity name is a fresh `EntAssocN` from [nextUnusedAssociativeEntityOuterName].
 */
fun applyPromoteRelationshipsToAssociativeEntities(
    schema: ConceptualSchema,
    selection: CanvasSelection,
): ConceptualSchema? {
    val relIds = relationshipIdsSelectedForPromote(schema, selection).toList().sorted()
    if (relIds.isEmpty()) return null
    var work = schema
    for (relId in relIds) {
        val rel = work.elements[relId] as? SchemaElement.Relationship ?: continue
        val outerName = work.nextUnusedAssociativeEntityOuterName()
        val p = rel.position
        val assoc = SchemaElement.AssociativeEntity(
            id = relId,
            name = outerName,
            position = ElementPosition(
                x = p.x,
                y = p.y,
                width = ConceptualPlacementDefaults.associativeOuterWidth,
                height = ConceptualPlacementDefaults.associativeOuterHeight,
            ),
            observations = "",
            dictionary = "",
            labelStyle = rel.labelStyle,
            hiddenAttributes = rel.hiddenAttributes,
            relationshipName = rel.name,
            relationshipDictionary = rel.dictionary,
            relationshipObservations = rel.observations,
            arrowDirection = rel.arrowDirection,
        )
        work = work.withElement(assoc)
    }
    return work
}
