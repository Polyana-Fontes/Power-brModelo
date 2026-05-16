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

package games.polyclub.power.brmodelo.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextMeasurer
import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.ConceptualBulkDeleteBand
import games.polyclub.power.brmodelo.domain.ConceptualLinkPick
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.elementsIntersectingBulkDeleteBand
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.SchemaElement
import games.polyclub.power.brmodelo.ui.canvas.toRect
import kotlin.math.abs

// ── Resize handle constants ───────────────────────────────────────────────────

/** Side length (in pixels) of each square resize handle drawn at element corners. */
const val HANDLE_SIZE_PX = 8f

/** Enumeration of the four corner resize handles. */
enum class ResizeHandle { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

// ── Main hit-test entry point ─────────────────────────────────────────────────

/**
 * Performs a hit-test at [point] in **model** (schema) coordinates — the caller must map from view
 * pixels with [games.polyclub.power.brmodelo.ui.canvas.viewOffsetToModel] (subtract pan, divide by zoom).
 *
 * Priority order — highest to lowest, matching the Pascal z-order:
 * 1. Cardinality labels (topmost layer).
 * 2. Elements in reverse insertion order (last inserted = visually on top).
 *
 * Returns [games.polyclub.power.brmodelo.domain.CanvasSelection.None] when nothing is hit.
 *
 * Mirrors [TBase.MouseDown] routing in the original `mer.pas`.
 */
fun hitTest(schema: ConceptualSchema, point: Offset, textMeasurer: TextMeasurer): CanvasSelection {
    // 1. Cardinality labels are drawn on top of everything
    val cardHit = hitTestCardinality(schema, point, textMeasurer)
    if (cardHit != CanvasSelection.None) return cardHit

    // 2. Elements in reverse insertion order (last element on top)
    return hitTestElement(schema, point)
}

/**
 * Tests whether [point] falls inside any cardinality label in [schema].
 *
 * Uses [cardinalityLabelInteractionRect], which matches both stored positions and the
 * same fallback layout as [drawCardinalityLabel] when [Connection.cardinalityPosition] is null.
 */
fun hitTestCardinality(schema: ConceptualSchema, point: Offset, textMeasurer: TextMeasurer): CanvasSelection {
    for (conn in schema.connections.asReversed()) {
        val rect = cardinalityLabelInteractionRect(schema, conn, textMeasurer) ?: continue
        if (rect.contains(point)) return CanvasSelection.Cardinality(conn.id)
    }
    return CanvasSelection.None
}

/**
 * Tests whether [point] falls inside any [games.polyclub.power.brmodelo.domain.SchemaElement] in [schema].
 *
 * Uses bounding-box (axis-aligned rectangle) hit-testing for all element shapes,
 * matching the Pascal approach which uses [TBase.BoundsRect] for mouse interaction.
 */
fun hitTestElement(schema: ConceptualSchema, point: Offset): CanvasSelection {
    // Iterate in reverse so last-drawn (visually on top) elements are checked first.
    for (element in schema.elements.values.toList().asReversed()) {
        if (element.position.toRect().contains(point)) {
            return CanvasSelection.Element(element.id)
        }
    }
    return CanvasSelection.None
}

private fun Rect.overlapsBand(band: ConceptualBulkDeleteBand): Boolean =
    left < band.right && right > band.left && top < band.bottom && bottom > band.top

/**
 * Connection ids whose cardinality label interaction rect intersects [band] (rectangle multi-select).
 */
fun cardinalityLabelsIntersectingSelectionBand(
    schema: ConceptualSchema,
    band: ConceptualBulkDeleteBand,
    textMeasurer: TextMeasurer,
): Set<Int> {
    val out = mutableSetOf<Int>()
    for (conn in schema.connections) {
        val rect = cardinalityLabelInteractionRect(schema, conn, textMeasurer) ?: continue
        if (rect.overlapsBand(band)) out.add(conn.id)
    }
    return out
}

/** Geometric picks inside the selection rectangle: no attribute/specialization closure. */
data class SelectionBandGeometricPick(
    val elementIds: Set<Int>,
    val cardinalityConnectionIds: Set<Int>,
)

fun selectionBandGeometricPick(
    schema: ConceptualSchema,
    band: ConceptualBulkDeleteBand,
    textMeasurer: TextMeasurer,
): SelectionBandGeometricPick {
    val elements = elementsIntersectingBulkDeleteBand(schema, band)
    val cards = cardinalityLabelsIntersectingSelectionBand(schema, band, textMeasurer)
    return SelectionBandGeometricPick(elements, cards)
}

// ── Resize handle hit-test ────────────────────────────────────────────────────

/**
 * Returns which resize handle [point] falls within for [position], or null if none.
 *
 * Each handle is a [games.polyclub.power.brmodelo.ui.canvas.HANDLE_SIZE_PX]×[games.polyclub.power.brmodelo.ui.canvas.HANDLE_SIZE_PX] square at one of the four
 * corners of the element's bounding box.
 */
fun getResizeHandleAt(position: ElementPosition, point: Offset): ResizeHandle? {
    val corners = resizeHandleRects(position)
    for ((handle, rect) in corners) {
        if (rect.contains(point)) return handle
    }
    return null
}

/**
 * Returns the screen [Rect] for each of the four resize handles of [position].
 */
fun resizeHandleRects(position: ElementPosition): Map<ResizeHandle, Rect> {
    val half = HANDLE_SIZE_PX / 2f
    val left   = position.x.toFloat()
    val top    = position.y.toFloat()
    val right  = left + position.width
    val bottom = top  + position.height

    fun handleRect(cx: Float, cy: Float) = Rect(cx - half, cy - half, cx + half, cy + half)

    return mapOf(
        ResizeHandle.TOP_LEFT     to handleRect(left,  top),
        ResizeHandle.TOP_RIGHT    to handleRect(right, top),
        ResizeHandle.BOTTOM_LEFT  to handleRect(left,  bottom),
        ResizeHandle.BOTTOM_RIGHT to handleRect(right, bottom),
    )
}

// ── Schema element position mutation helper ───────────────────────────────────

/**
 * Returns a copy of this [games.polyclub.power.brmodelo.domain.SchemaElement] with [position] replaced.
 *
 * Convenience extension because [games.polyclub.power.brmodelo.domain.SchemaElement] is a sealed class and `.copy()`
 * is only available on the concrete subtypes.
 */
fun SchemaElement.withPosition(position: ElementPosition): SchemaElement = when (this) {
    is SchemaElement.Entity          -> copy(position = position)
    is SchemaElement.Relationship    -> copy(position = position)
    is SchemaElement.AssociativeEntity -> copy(position = position)
    is SchemaElement.Attribute       -> copy(position = position)
    is SchemaElement.Specialization  -> copy(position = position)
    is SchemaElement.SelfRelationship -> copy(position = position)
    is SchemaElement.Annotation      -> copy(position = position)
}

// ── Private helpers ───────────────────────────────────────────────────────────

private fun ElementPosition.toRect(): Rect =
    Rect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat())

// ── Conceptual link tool hit-test ─────────────────────────────────────────────

/** Inset of the inner relationship diamond inside an associative entity (matches [SchemaRenderer]). */
private const val ASSOCIATIVE_INNER_INSET_PX = 15

/**
 * True if [point] lies inside the axis-aligned rhombus inscribed in [innerBounds]
 * (relationship diamond geometry).
 */
fun relationshipDiamondContains(innerBounds: ElementPosition, point: Offset): Boolean {
    val cx = innerBounds.x + innerBounds.width / 2f
    val cy = innerBounds.y + innerBounds.height / 2f
    val dx = kotlin.math.abs(point.x - cx)
    val dy = kotlin.math.abs(point.y - cy)
    val hx = innerBounds.width / 2f
    val hy = innerBounds.height / 2f
    if (hx <= 0f || hy <= 0f) return false
    return dx / hx + dy / hy <= 1f
}

/**
 * Hit-test for the "Ligar objetos" tool: returns a [ConceptualLinkPick] for entity / relationship /
 * self-relationship / specialization / associative (inner vs outer), or `null` if nothing linkable was hit.
 *
 * Uses the same z-order as [hitTestElement] (last-drawn element wins).
 */
fun hitTestConceptualLinkPick(schema: ConceptualSchema, point: Offset): ConceptualLinkPick? {
    for (element in schema.elements.values.toList().asReversed()) {
        if (!element.position.toRect().contains(point)) continue
        return when (element) {
            is SchemaElement.AssociativeEntity -> {
                val outer = element.position
                val inner = ElementPosition(
                    x = outer.x + ASSOCIATIVE_INNER_INSET_PX,
                    y = outer.y + ASSOCIATIVE_INNER_INSET_PX,
                    width = (outer.width - 2 * ASSOCIATIVE_INNER_INSET_PX).coerceAtLeast(10),
                    height = (outer.height - 2 * ASSOCIATIVE_INNER_INSET_PX).coerceAtLeast(10),
                )
                if (relationshipDiamondContains(inner, point)) {
                    ConceptualLinkPick(element.id, isAssociativeOuterEntitySide = false)
                } else {
                    ConceptualLinkPick(element.id, isAssociativeOuterEntitySide = true)
                }
            }
            is SchemaElement.Entity,
            is SchemaElement.Relationship,
            is SchemaElement.SelfRelationship,
            is SchemaElement.Specialization,
            -> ConceptualLinkPick(element.id, isAssociativeOuterEntitySide = false)
            else -> null
        } ?: continue
    }
    return null
}

/**
 * Hit-test for specialization tools: topmost [SchemaElement.Entity] under [point], or `null`.
 * Associative entities are excluded (Pascal `UsrSelA is TEntidade` only).
 */
fun hitTestPlainEntityId(schema: ConceptualSchema, point: Offset): Int? {
    for (element in schema.elements.values.toList().asReversed()) {
        if (element !is SchemaElement.Entity) continue
        if (element.position.toRect().contains(point)) return element.id
    }
    return null
}
