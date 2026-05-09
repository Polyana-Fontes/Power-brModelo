/*
 * KbrModelo - Kotlin port of brModelo 3.0 originally written in Pascal
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

package games.polyclub.kbrmodelo.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import games.polyclub.kbrmodelo.domain.CanvasSelection
import games.polyclub.kbrmodelo.domain.ConceptualSchema
import games.polyclub.kbrmodelo.domain.ElementPosition
import games.polyclub.kbrmodelo.domain.SchemaElement
import kotlin.math.abs

// ── Resize handle constants ───────────────────────────────────────────────────

/** Side length (in pixels) of each square resize handle drawn at element corners. */
const val HANDLE_SIZE_PX = 8f

/** Enumeration of the four corner resize handles. */
enum class ResizeHandle { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

// ── Main hit-test entry point ─────────────────────────────────────────────────

/**
 * Performs a hit-test at [point] (in schema/canvas coordinates — not screen coordinates;
 * the caller must subtract the pan offset before invoking this function).
 *
 * Priority order — highest to lowest, matching the Pascal z-order:
 * 1. Cardinality labels (topmost layer).
 * 2. Elements in reverse insertion order (last inserted = visually on top).
 *
 * Returns [CanvasSelection.None] when nothing is hit.
 *
 * Mirrors [TBase.MouseDown] routing in the original `mer.pas`.
 */
fun hitTest(schema: ConceptualSchema, point: Offset): CanvasSelection {
    // 1. Cardinality labels are drawn on top of everything
    val cardHit = hitTestCardinality(schema, point)
    if (cardHit != CanvasSelection.None) return cardHit

    // 2. Elements in reverse insertion order (last element on top)
    return hitTestElement(schema, point)
}

/**
 * Tests whether [point] falls inside any cardinality label in [schema].
 *
 * Only tests connections that have a stored [Connection.cardinalityPosition];
 * connections whose label position is computed dynamically at render time are
 * skipped here (the user must move the label once to lock it in place, after
 * which it becomes hit-testable).
 */
fun hitTestCardinality(schema: ConceptualSchema, point: Offset): CanvasSelection {
    for (conn in schema.connections.asReversed()) {
        if (!conn.showCardinality || conn.cardinality == null) continue
        val lp = conn.cardinalityPosition ?: continue

        val labelText = buildCardinalityText(conn)
        val estimatedWidth = estimateTextWidth(labelText)
        // Small X correction mirrors the render-time xAdjustment = cardOnlyWidth / 4
        val xAdjust = estimateTextWidth(conn.cardinality.label) / 4f
        val rect = Rect(
            left   = lp.x + xAdjust,
            top    = lp.y.toFloat(),
            right  = lp.x + xAdjust + estimatedWidth,
            bottom = lp.y + CARDINALITY_LABEL_HEIGHT,
        )
        if (rect.contains(point)) return CanvasSelection.Cardinality(conn.id)
    }
    return CanvasSelection.None
}

/**
 * Tests whether [point] falls inside any [SchemaElement] in [schema].
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

// ── Resize handle hit-test ────────────────────────────────────────────────────

/**
 * Returns which resize handle [point] falls within for [position], or null if none.
 *
 * Each handle is a [HANDLE_SIZE_PX]×[HANDLE_SIZE_PX] square at one of the four
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
 * Returns a copy of this [SchemaElement] with [position] replaced.
 *
 * Convenience extension because [SchemaElement] is a sealed class and `.copy()`
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

private const val CARDINALITY_LABEL_HEIGHT = 20f

/** Approximate text width in pixels at 11sp (7 px/char is a conservative estimate). */
private fun estimateTextWidth(text: String): Float = (text.length * 7f + 10f).coerceAtLeast(30f)

private fun buildCardinalityText(conn: games.polyclub.kbrmodelo.domain.Connection): String {
    val base = conn.cardinality?.label ?: return ""
    return if (conn.cardinalityRole.isNotEmpty()) "${conn.cardinalityRole} $base" else base
}

private fun ElementPosition.toRect(): Rect =
    Rect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat())
