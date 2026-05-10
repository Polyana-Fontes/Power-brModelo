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
 * Inner relationship diamond bounds for an [SchemaElement.AssociativeEntity] (15 px inset on each side),
 * matching the canvas renderer and Pascal `TEntidadeAssoss` / `InflateRect -15`.
 */
internal fun associativeInnerDiamondPosition(outer: ElementPosition): ElementPosition =
    ElementPosition(
        x = outer.x + 15,
        y = outer.y + 15,
        width = (outer.width - 30).coerceAtLeast(10),
        height = (outer.height - 30).coerceAtLeast(10),
    )

/** IDs of [SchemaElement.AssociativeEntity] in [selection] (ignores cardinality-only picks). */
fun associativeEntityIdsSelectedForDemote(schema: ConceptualSchema, selection: CanvasSelection): Set<Int> {
    val (e, _) = selection.toMultiPickSets()
    return e.mapNotNull { id -> if (schema.elements[id] is SchemaElement.AssociativeEntity) id else null }.toSet()
}

fun canDemoteAssociativeToRelationshipMenu(schema: ConceptualSchema, selection: CanvasSelection): Boolean =
    associativeEntityIdsSelectedForDemote(schema, selection).isNotEmpty()

fun canDemoteAssociativeToEntityMenu(schema: ConceptualSchema, selection: CanvasSelection): Boolean =
    associativeEntityIdsSelectedForDemote(schema, selection).isNotEmpty()

/**
 * Replaces each selected [SchemaElement.AssociativeEntity] with a [SchemaElement.Relationship] **same id**,
 * using inner-diamond geometry and inner relationship metadata. Outer entity caption and text are dropped.
 * Connection [Connection.useAssociativeOuterForEndA]/[Connection.useAssociativeOuterForEndB] flags are cleared.
 *
 * Ribbon: **Operações → Rebaixar à Relação**.
 */
fun applyDemoteAssociativeToRelationship(
    schema: ConceptualSchema,
    selection: CanvasSelection,
): ConceptualSchema? {
    val ids = associativeEntityIdsSelectedForDemote(schema, selection).toList().sorted()
    if (ids.isEmpty()) return null
    var work = schema
    for (aid in ids) {
        val assoc = work.elements[aid] as? SchemaElement.AssociativeEntity ?: continue
        val inner = associativeInnerDiamondPosition(assoc.position)
        val rel =
            SchemaElement.Relationship(
                id = aid,
                name = assoc.relationshipName,
                position = inner,
                observations = assoc.relationshipObservations,
                dictionary = assoc.relationshipDictionary,
                labelStyle = assoc.labelStyle,
                hiddenAttributes = assoc.hiddenAttributes,
                arrowDirection = assoc.arrowDirection,
                showName = true,
            )
        work = work.withElement(rel)
        work = work.withAssociativeFlagsStrippedForElement(aid)
    }
    return work
}

/**
 * Replaces each selected [SchemaElement.AssociativeEntity] with a [SchemaElement.Entity] **same id**,
 * using outer rectangle geometry and outer metadata. Inner relationship fields are dropped.
 *
 * Non-attribute connections that were incident on the associative (inner or outer legs to other
 * participants) are **removed** so the model never keeps plain **entity–entity** edges (invalid in MER).
 * Legs that used the **inner diamond** ([Connection.useAssociativeOuterForEndA]/B false on the
 * associative end) and link to at least **two** distinct conceptual participants
 * ([SchemaElement.Entity], [SchemaElement.AssociativeEntity], [SchemaElement.SelfRelationship])
 * are **replaced** by a new [SchemaElement.Relationship] at the old inner-diamond position, with
 * one connection per participant (cardinality copied when the old edge was `(associative → participant)`).
 * With fewer than two such participants, inner legs are only dropped.
 * Attribute–owner links to the associative are preserved.
 *
 * Ribbon: **Operações → Separar Entidade da Relação**.
 */
fun applyDemoteAssociativeToEntity(
    schema: ConceptualSchema,
    selection: CanvasSelection,
): ConceptualSchema? {
    val ids = associativeEntityIdsSelectedForDemote(schema, selection).toList().sorted()
    if (ids.isEmpty()) return null
    var work = schema
    for (aid in ids) {
        val assoc = work.elements[aid] as? SchemaElement.AssociativeEntity ?: continue
        val innerPos = associativeInnerDiamondPosition(assoc.position)

        val innerParticipantTemplates = LinkedHashMap<Int, Connection>()
        val connectionIdsToRemove = mutableSetOf<Int>()

        for (c in work.connections) {
            if (c.elementIdA != aid && c.elementIdB != aid) continue
            if (isAttributeOwnerLinkToAssociative(work, c, aid)) continue

            connectionIdsToRemove.add(c.id)

            if (!associativeEndUsesInnerDiamond(c, aid)) continue
            val other = otherEndId(c, aid)
            val el = work.elements[other] ?: continue
            if (!isConceptualRelationshipParticipant(el)) continue
            innerParticipantTemplates.putIfAbsent(other, c)
        }

        work = work.copy(connections = work.connections.filter { it.id !in connectionIdsToRemove })

        work =
            work.withElement(
                SchemaElement.Entity(
                    id = aid,
                    name = assoc.name,
                    position = assoc.position,
                    observations = assoc.observations,
                    dictionary = assoc.dictionary,
                    labelStyle = assoc.labelStyle,
                    hiddenAttributes = assoc.hiddenAttributes,
                ),
            )

        if (innerParticipantTemplates.size >= 2) {
            val (wRel, relId) = work.allocateId()
            work = wRel
            val rel =
                SchemaElement.Relationship(
                    id = relId,
                    name = work.nextUnusedRelationshipName(),
                    position = innerPos,
                    observations = "",
                    dictionary = "",
                    labelStyle = ConceptualPlacementDefaults.labelStyle,
                    hiddenAttributes = emptyList(),
                    arrowDirection = ArrowDirection.NONE,
                    showName = true,
                )
            work = work.withElement(rel)

            for ((participantId, oldConn) in innerParticipantTemplates) {
                if (isDuplicateConceptualRelEntityConnection(work, relId, participantId)) continue
                val (wC, connId) = work.allocateId()
                work = wC
                work = work.withConnection(
                    newRelationshipToParticipantConnection(
                        id = connId,
                        relId = relId,
                        participantId = participantId,
                        template = oldConn,
                        associativeId = aid,
                    ),
                )
            }
        }
    }
    return work
}

private fun isAttributeOwnerLinkToAssociative(schema: ConceptualSchema, conn: Connection, assocId: Int): Boolean {
    val a = schema.elements[conn.elementIdA]
    val b = schema.elements[conn.elementIdB]
    if (a is SchemaElement.Attribute && conn.elementIdB == assocId) return true
    if (b is SchemaElement.Attribute && conn.elementIdA == assocId) return true
    return false
}

private fun otherEndId(conn: Connection, assocId: Int): Int =
    if (conn.elementIdA == assocId) conn.elementIdB else conn.elementIdA

private fun associativeEndUsesInnerDiamond(conn: Connection, assocId: Int): Boolean =
    when {
        conn.elementIdA == assocId -> !conn.useAssociativeOuterForEndA
        conn.elementIdB == assocId -> !conn.useAssociativeOuterForEndB
        else -> false
    }

private fun isConceptualRelationshipParticipant(el: SchemaElement): Boolean =
    el is SchemaElement.Entity ||
        el is SchemaElement.AssociativeEntity ||
        el is SchemaElement.SelfRelationship

/**
 * Builds a rel→participant [Connection], reusing visual/cardinality fields from [template] when it
 * was stored as `(associativeId → participantId)`.
 */
private fun newRelationshipToParticipantConnection(
    id: Int,
    relId: Int,
    participantId: Int,
    template: Connection,
    associativeId: Int,
): Connection {
    val base =
        if (template.elementIdA == associativeId && template.elementIdB == participantId) {
            template.copy(
                id = id,
                elementIdA = relId,
                elementIdB = participantId,
                useAssociativeOuterForEndA = false,
                useAssociativeOuterForEndB = false,
            )
        } else {
            Connection(
                id = id,
                elementIdA = relId,
                elementIdB = participantId,
                cardinality = Cardinality.ZERO_TO_MANY,
                showCardinality = template.showCardinality,
                cardinalityFixed = template.cardinalityFixed,
                isWeak = template.isWeak,
                orientation = template.orientation,
                cardinalityRole = template.cardinalityRole,
                cardinalityObservations = template.cardinalityObservations,
                cardinalityPosition = template.cardinalityPosition,
                cardinalityAutoSize = template.cardinalityAutoSize,
                useAssociativeOuterForEndA = false,
                useAssociativeOuterForEndB = false,
            )
        }
    return base
}

private fun ConceptualSchema.withAssociativeFlagsStrippedForElement(elementId: Int): ConceptualSchema =
    copy(
        connections = connections.map { c ->
            var out = c
            if (c.elementIdA == elementId) out = out.copy(useAssociativeOuterForEndA = false)
            if (c.elementIdB == elementId) out = out.copy(useAssociativeOuterForEndB = false)
            out
        },
    )
