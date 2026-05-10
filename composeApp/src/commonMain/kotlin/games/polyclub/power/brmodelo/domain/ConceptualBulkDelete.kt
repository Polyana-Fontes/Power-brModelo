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

import kotlin.math.max
import kotlin.math.min

/**
 * Axis-aligned band in schema (model) coordinates for bulk-delete selection.
 */
data class ConceptualBulkDeleteBand(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(left <= right && top <= bottom) { "Band must be normalized (left≤right, top≤bottom)." }
    }

    companion object {
        fun fromCorners(ax: Float, ay: Float, bx: Float, by: Float): ConceptualBulkDeleteBand {
            val l = min(ax, bx)
            val r = max(ax, bx)
            val t = min(ay, by)
            val b = max(ay, by)
            return ConceptualBulkDeleteBand(l, t, r, b)
        }
    }
}

private fun SchemaElement.boundsOverlapBand(band: ConceptualBulkDeleteBand): Boolean {
    val p = position
    val l = p.x.toFloat()
    val t = p.y.toFloat()
    val r = (p.x + p.width).toFloat()
    val b = (p.y + p.height).toFloat()
    return l < band.right && r > band.left && t < band.bottom && b > band.top
}

/**
 * Elements whose bounding box intersects [band] (inside or partially inside).
 */
fun elementsIntersectingBulkDeleteBand(schema: ConceptualSchema, band: ConceptualBulkDeleteBand): Set<Int> =
    schema.elements.mapNotNull { (id, el) ->
        if (el.boundsOverlapBand(band)) id else null
    }.toSet()

/**
 * Expands [seedIds] with every element that must be removed together for a consistent model:
 * owned attributes, composite children, and specializations whose base entity is removed.
 *
 * [SchemaElement.SelfRelationship] is **not** pulled in when its owner entity is removed: the entity
 * is deleted and incident connections are dropped, while the self-relationship element may remain
 * (see [ConceptualSchema.withoutElements]).
 */
fun expandBulkDeleteClosure(schema: ConceptualSchema, seedIds: Set<Int>): Set<Int> {
    if (seedIds.isEmpty()) return emptySet()
    val result = seedIds.toMutableSet()
    var changed = true
    while (changed) {
        changed = false
        for ((id, el) in schema.elements) {
            if (id in result) continue
            when (el) {
                is SchemaElement.Attribute -> {
                    if (el.ownerId in result) {
                        result.add(id)
                        changed = true
                    }
                }
                is SchemaElement.Specialization -> {
                    if (el.baseEntityId in result) {
                        result.add(id)
                        changed = true
                    }
                }
                else -> Unit
            }
        }
        for ((id, el) in schema.elements) {
            if (el is SchemaElement.Attribute && el.id in result) {
                for (cid in el.childAttributeIds) {
                    if (cid !in result) {
                        result.add(cid)
                        changed = true
                    }
                }
            }
        }
    }
    return result
}

/**
 * Counts shown in the inspector during bulk delete preview.
 *
 * [relationships] includes [SchemaElement.Relationship] and [SchemaElement.SelfRelationship].
 * [cardinalityLabels] counts selected cardinality labels ([Connection] ids), used for multi-select UI only.
 * [hiddenAttributesLeaves] sums [HiddenAttribute.physicalFieldLeafCount] for every removed element.
 * [total] is the sum of all categories (one undoable operation is still a single [SchemaHistory.push]).
 */
data class BulkDeleteCategoryCounts(
    val entities: Int = 0,
    val relationships: Int = 0,
    val associativeEntities: Int = 0,
    val specializations: Int = 0,
    val attributes: Int = 0,
    val hiddenAttributesLeaves: Int = 0,
    val observations: Int = 0,
    val cardinalityLabels: Int = 0,
) {
    val total: Int =
        entities + relationships + associativeEntities + specializations +
            attributes + hiddenAttributesLeaves + observations + cardinalityLabels
}

fun bulkDeleteCategoryCounts(
    schema: ConceptualSchema,
    elementIds: Set<Int>,
    cardinalityLabelConnectionIds: Set<Int> = emptySet(),
): BulkDeleteCategoryCounts {
    if (elementIds.isEmpty() && cardinalityLabelConnectionIds.isEmpty()) {
        return BulkDeleteCategoryCounts()
    }
    var entities = 0
    var relationships = 0
    var associativeEntities = 0
    var specializations = 0
    var attributes = 0
    var observations = 0
    var hiddenLeaves = 0
    for (id in elementIds) {
        val el = schema.elements[id] ?: continue
        hiddenLeaves += el.hiddenAttributes.sumOf { it.physicalFieldLeafCount() }
        when (el) {
            is SchemaElement.Entity -> entities++
            is SchemaElement.Relationship -> relationships++
            is SchemaElement.SelfRelationship -> relationships++
            is SchemaElement.AssociativeEntity -> associativeEntities++
            is SchemaElement.Specialization -> specializations++
            is SchemaElement.Attribute -> attributes++
            is SchemaElement.Annotation -> observations++
        }
    }
    val cardinalityLabels = cardinalityLabelConnectionIds.count { cid ->
        schema.connections.any { it.id == cid }
    }
    return BulkDeleteCategoryCounts(
        entities = entities,
        relationships = relationships,
        associativeEntities = associativeEntities,
        specializations = specializations,
        attributes = attributes,
        hiddenAttributesLeaves = hiddenLeaves,
        observations = observations,
        cardinalityLabels = cardinalityLabels,
    )
}

/** Category counts for whatever is currently selected on the canvas (inspector summary). */
fun bulkDeleteCategoryCountsForCanvasSelection(
    schema: ConceptualSchema,
    selection: CanvasSelection,
): BulkDeleteCategoryCounts =
    when (selection) {
        CanvasSelection.None -> BulkDeleteCategoryCounts()
        is CanvasSelection.Element -> bulkDeleteCategoryCounts(schema, setOf(selection.id), emptySet())
        is CanvasSelection.Cardinality ->
            bulkDeleteCategoryCounts(schema, emptySet(), setOf(selection.connectionId))
        is CanvasSelection.Multiple ->
            bulkDeleteCategoryCounts(schema, selection.elementIds, selection.cardinalityConnectionIds)
    }

fun bulkDeleteResolvedIds(schema: ConceptualSchema, band: ConceptualBulkDeleteBand): Set<Int> {
    val seed = elementsIntersectingBulkDeleteBand(schema, band)
    return expandBulkDeleteClosure(schema, seed)
}

/**
 * Same closure as a rubber-band delete, but starting from a single selected element id.
 */
fun singleElementDeletionClosure(schema: ConceptualSchema, elementId: Int): Set<Int> =
    expandBulkDeleteClosure(schema, setOf(elementId))

/**
 * Deletes the current canvas selection in one step (same rules as the Delete / Backspace handler).
 * Returns null when nothing would change.
 */
fun deleteCanvasSelection(schema: ConceptualSchema, selection: CanvasSelection): ConceptualSchema? =
    when (selection) {
        CanvasSelection.None -> null
        is CanvasSelection.Cardinality -> {
            val stripped = schema.withoutConnection(selection.connectionId)
            if (stripped == schema) null
            else stripped.withNormalizedAttributeMultiValuedCounts()
        }
        is CanvasSelection.Element -> {
            val ids = singleElementDeletionClosure(schema, selection.id)
            if (ids.isEmpty()) null
            else schema.withoutElements(ids).withNormalizedAttributeMultiValuedCounts()
        }
        is CanvasSelection.Multiple -> {
            var next = schema
            if (selection.elementIds.isNotEmpty()) {
                val ids = expandBulkDeleteClosure(schema, selection.elementIds)
                if (ids.isNotEmpty()) {
                    next = next.withoutElements(ids)
                }
            }
            for (cid in selection.cardinalityConnectionIds) {
                val n2 = next.withoutConnection(cid)
                if (n2 != next) next = n2
            }
            if (next != schema) next.withNormalizedAttributeMultiValuedCounts() else null
        }
    }
