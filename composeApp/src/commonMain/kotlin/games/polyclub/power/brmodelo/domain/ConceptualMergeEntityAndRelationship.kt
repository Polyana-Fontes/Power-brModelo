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
 * When [selection] picks exactly one [SchemaElement.Entity] and one [SchemaElement.Relationship],
 * returns `(entityId, relationshipId)`; otherwise `null`. Cardinality-only picks are ignored for the
 * count but cause failure (menu stays disabled).
 */
fun entityAndRelationshipIdsForMerge(schema: ConceptualSchema, selection: CanvasSelection): Pair<Int, Int>? {
    val (elementIds, cardinalityIds) = selection.toMultiPickSets()
    if (cardinalityIds.isNotEmpty()) return null
    if (elementIds.size != 2) return null
    val a = schema.elements[elementIds.first()] ?: return null
    val b = schema.elements[elementIds.last()] ?: return null
    return when {
        a is SchemaElement.Entity && b is SchemaElement.Relationship -> a.id to b.id
        b is SchemaElement.Entity && a is SchemaElement.Relationship -> b.id to a.id
        else -> null
    }
}

/** True when **Operações → Unir em Entidade Associativa** applies. */
fun canMergeEntityAndRelationshipToAssociativeMenu(schema: ConceptualSchema, selection: CanvasSelection): Boolean =
    entityAndRelationshipIdsForMerge(schema, selection) != null

/**
 * Merges the chosen [SchemaElement.Entity] and [SchemaElement.Relationship] into one
 * [SchemaElement.AssociativeEntity] using the **entity's id**, **position**, and **outer** metadata.
 * The relationship element is removed; its dictionary, observations, name, and arrow become the
 * inner associative fields. Connections and attribute owners that referenced the relationship id
 * are rewired to the entity id. Self-loops created by the merge are dropped; duplicate directed
 * edges are coalesced.
 *
 * Ribbon: **Operações → Unir em Entidade Associativa**.
 */
fun applyMergeEntityAndRelationshipToAssociative(
    schema: ConceptualSchema,
    selection: CanvasSelection,
): ConceptualSchema? {
    val (entityId, relId) = entityAndRelationshipIdsForMerge(schema, selection) ?: return null
    val ent = schema.elements[entityId] as? SchemaElement.Entity ?: return null
    val rel = schema.elements[relId] as? SchemaElement.Relationship ?: return null

    var work = schema.withAttributeOwnersRemapped(fromOwnerId = relId, toOwnerId = entityId)

    val rewritten =
        work.connections.mapNotNull { conn ->
            val c =
                if (conn.elementIdA == relId || conn.elementIdB == relId) {
                    conn.copy(
                        elementIdA = if (conn.elementIdA == relId) entityId else conn.elementIdA,
                        elementIdB = if (conn.elementIdB == relId) entityId else conn.elementIdB,
                        useAssociativeOuterForEndA =
                            if (conn.elementIdA == relId) false else conn.useAssociativeOuterForEndA,
                        useAssociativeOuterForEndB =
                            if (conn.elementIdB == relId) false else conn.useAssociativeOuterForEndB,
                    )
                } else {
                    conn
                }
            val cleared =
                c.copy(
                    useAssociativeOuterForEndA = if (c.elementIdA == entityId) false else c.useAssociativeOuterForEndA,
                    useAssociativeOuterForEndB = if (c.elementIdB == entityId) false else c.useAssociativeOuterForEndB,
                )
            if (cleared.elementIdA == cleared.elementIdB) null else cleared
        }.dedupeDirectedConnectionsPreservingFirst()

    work = work.copy(connections = rewritten).withoutElement(relId)

    val assoc =
        SchemaElement.AssociativeEntity(
            id = entityId,
            name = ent.name,
            position = ent.position,
            observations = ent.observations,
            dictionary = ent.dictionary,
            labelStyle = ent.labelStyle,
            hiddenAttributes = ent.hiddenAttributes + rel.hiddenAttributes,
            relationshipName = rel.name,
            relationshipDictionary = rel.dictionary,
            relationshipObservations = rel.observations,
            arrowDirection = rel.arrowDirection,
        )
    return work.withElement(assoc)
}

private fun ConceptualSchema.withAttributeOwnersRemapped(fromOwnerId: Int, toOwnerId: Int): ConceptualSchema {
    var w = this
    for (attr in attributes.filter { it.ownerId == fromOwnerId }) {
        w = w.withElement(attr.copy(ownerId = toOwnerId))
    }
    return w
}

private fun List<Connection>.dedupeDirectedConnectionsPreservingFirst(): List<Connection> {
    val seen = mutableSetOf<Pair<Int, Int>>()
    val out = ArrayList<Connection>(size)
    for (c in this) {
        val key = c.elementIdA to c.elementIdB
        if (!seen.add(key)) continue
        out.add(c)
    }
    return out
}
