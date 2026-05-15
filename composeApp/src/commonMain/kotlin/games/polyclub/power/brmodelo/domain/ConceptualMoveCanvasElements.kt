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
 * Result of translating canvas elements in model space (MCP / batch moves).
 * [movedElementIds] is the full set that was translated (including expanded owned attributes when requested).
 */
internal sealed interface ConceptualMoveCanvasElementsApplyResult {
    data class Ok(
        val schema: ConceptualSchema,
        val movedElementIds: Set<Int>,
    ) : ConceptualMoveCanvasElementsApplyResult

    data class Err(val code: String) : ConceptualMoveCanvasElementsApplyResult
}

/**
 * Expands [seedIds] with every on-canvas [SchemaElement.Attribute] whose [SchemaElement.Attribute.ownerId]
 * is already in the set (closure), mirroring how owned attributes follow their owner when dragged on the canvas.
 */
internal fun expandCanvasElementMoveSet(
    schema: ConceptualSchema,
    seedIds: Set<Int>,
    moveOwnedCanvasAttributes: Boolean,
): Set<Int> {
    val validSeeds = seedIds.filter { it in schema.elements }.toSet()
    if (!moveOwnedCanvasAttributes) return validSeeds
    val acc = validSeeds.toMutableSet()
    var growing = true
    while (growing) {
        growing = false
        for ((id, el) in schema.elements) {
            if (id in acc) continue
            if (el is SchemaElement.Attribute && el.ownerId in acc) {
                acc.add(id)
                growing = true
            }
        }
    }
    return acc
}

/**
 * Translates every element in the expanded move set by ([deltaX], [deltaY]) in schema pixels.
 * Caller must then run `withCardinalityPositionsAfterElementsMovedByDelta` on the UI thread with the same
 * deltas and [movedElementIds] (see canvas drag commit path).
 */
internal fun applyMoveCanvasElementsByTranslation(
    schema: ConceptualSchema,
    seedElementIds: List<Int>,
    deltaX: Int,
    deltaY: Int,
    moveOwnedCanvasAttributes: Boolean,
): ConceptualMoveCanvasElementsApplyResult {
    if (deltaX == 0 && deltaY == 0) {
        return ConceptualMoveCanvasElementsApplyResult.Err("delta_zero")
    }
    val seeds = seedElementIds.toSet()
    if (seeds.isEmpty()) {
        return ConceptualMoveCanvasElementsApplyResult.Err("elementIds_empty")
    }
    if (seeds.any { it !in schema.elements }) {
        return ConceptualMoveCanvasElementsApplyResult.Err("element_not_found")
    }
    val toMove = expandCanvasElementMoveSet(schema, seeds, moveOwnedCanvasAttributes)
    var s = schema
    for (id in toMove) {
        val el = s.elements[id] ?: return ConceptualMoveCanvasElementsApplyResult.Err("element_not_found")
        val p = el.position
        val nextPos = p.copy(x = p.x + deltaX, y = p.y + deltaY).coercedToMinimumDimensions()
        s = s.withElement(schemaElementAtPosition(el, nextPos).withCoercedMinimumDimensions())
    }
    return ConceptualMoveCanvasElementsApplyResult.Ok(
        s.withNormalizedAttributeMultiValuedCounts(),
        toMove,
    )
}

private fun schemaElementAtPosition(el: SchemaElement, newPosition: ElementPosition): SchemaElement =
    when (el) {
        is SchemaElement.Entity -> el.copy(position = newPosition)
        is SchemaElement.Relationship -> el.copy(position = newPosition)
        is SchemaElement.AssociativeEntity -> el.copy(position = newPosition)
        is SchemaElement.Attribute -> el.copy(position = newPosition)
        is SchemaElement.Specialization -> el.copy(position = newPosition)
        is SchemaElement.SelfRelationship -> el.copy(position = newPosition)
        is SchemaElement.Annotation -> el.copy(position = newPosition)
    }
