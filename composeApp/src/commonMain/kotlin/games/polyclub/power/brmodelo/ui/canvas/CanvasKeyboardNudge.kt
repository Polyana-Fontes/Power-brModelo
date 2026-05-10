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

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.TextMeasurer
import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.SchemaElement

private enum class ArrowAxisDir {
    UP,
    DOWN,
    LEFT,
    RIGHT,
}

private fun Key.toArrowAxisDir(): ArrowAxisDir? = when (this) {
    Key.DirectionUp -> ArrowAxisDir.UP
    Key.DirectionDown -> ArrowAxisDir.DOWN
    Key.DirectionLeft -> ArrowAxisDir.LEFT
    Key.DirectionRight -> ArrowAxisDir.RIGHT
    else -> null
}

private fun CanvasSelection.selectedElementIds(): Set<Int> = when (this) {
    CanvasSelection.None -> emptySet()
    is CanvasSelection.Element -> setOf(id)
    is CanvasSelection.Multiple -> elementIds
    is CanvasSelection.Cardinality -> emptySet()
}

private fun CanvasSelection.selectedCardinalityConnectionIds(): Set<Int> = when (this) {
    CanvasSelection.None -> emptySet()
    is CanvasSelection.Element -> emptySet()
    is CanvasSelection.Multiple -> cardinalityConnectionIds
    is CanvasSelection.Cardinality -> setOf(connectionId)
}

private fun SchemaElement.allowsManualResize(): Boolean = when (this) {
    is SchemaElement.Attribute -> !autoSize
    is SchemaElement.Annotation -> !autoSize
    else -> true
}

private fun resizePositionFromKeyboard(pos: ElementPosition, dir: ArrowAxisDir, step: Int): ElementPosition {
    val minW = ElementPosition.MIN_WIDTH_PX
    val minH = ElementPosition.MIN_HEIGHT_PX
    return when (dir) {
        ArrowAxisDir.UP ->
            pos.copy(y = pos.y - step, height = pos.height + step)
        ArrowAxisDir.DOWN -> {
            val newH = (pos.height - step).coerceAtLeast(minH)
            val dh = pos.height - newH
            pos.copy(y = pos.y + dh, height = newH)
        }
        ArrowAxisDir.LEFT ->
            pos.copy(x = pos.x - step, width = pos.width + step)
        ArrowAxisDir.RIGHT -> {
            val newW = (pos.width - step).coerceAtLeast(minW)
            val dw = pos.width - newW
            pos.copy(x = pos.x + dw, width = newW)
        }
    }.coercedToMinimumDimensions()
}

/**
 * Arrow keys: move selection by 1 px (10 px with Shift). Ctrl+arrows resize (height up/down, width left/right);
 * Ctrl+Shift resizes by 10 px. Shrinking down/right keeps the bottom/right edge fixed (top/left moves with the resize).
 * Respects manual resize locks ([SchemaElement.Attribute.autoSize], [SchemaElement.Annotation.autoSize],
 * [games.polyclub.power.brmodelo.domain.Connection.cardinalityAutoSize]).
 */
fun ConceptualSchema.applyCanvasKeyboardArrow(
    selection: CanvasSelection,
    key: Key,
    isCtrlPressed: Boolean,
    isShiftPressed: Boolean,
    textMeasurer: TextMeasurer,
): ConceptualSchema? {
    val dir = key.toArrowAxisDir() ?: return null
    val step = if (isShiftPressed) 10 else 1
    return if (isCtrlPressed) {
        applyKeyboardResize(selection, dir, step, textMeasurer)
    } else {
        applyKeyboardMove(selection, dir, step, textMeasurer)
    }
}

private fun ConceptualSchema.applyKeyboardMove(
    selection: CanvasSelection,
    dir: ArrowAxisDir,
    step: Int,
    textMeasurer: TextMeasurer,
): ConceptualSchema? {
    val elemIds = selection.selectedElementIds()
    val cardIds = selection.selectedCardinalityConnectionIds()
    val dx = when (dir) {
        ArrowAxisDir.LEFT -> -step
        ArrowAxisDir.RIGHT -> step
        else -> 0
    }
    val dy = when (dir) {
        ArrowAxisDir.UP -> -step
        ArrowAxisDir.DOWN -> step
        else -> 0
    }
    if (dx == 0 && dy == 0) return null
    if (elemIds.isEmpty() && cardIds.isEmpty()) return null

    var s = this
    for (id in elemIds) {
        val el = s.elements[id] ?: continue
        val p = el.position
        s = s.withElement(el.withPosition(p.copy(x = p.x + dx, y = p.y + dy)))
    }
    return when {
        elemIds.isNotEmpty() ->
            s.withCardinalityPositionsAfterElementsMovedByDelta(
                movedElementIds = elemIds,
                dx = dx,
                dy = dy,
                selectedCardinalityConnectionIds = cardIds,
                textMeasurer = textMeasurer,
            )
        cardIds.isNotEmpty() ->
            s.withCardinalityPositionsAfterElementsMovedByDelta(
                movedElementIds = emptySet(),
                dx = dx,
                dy = dy,
                selectedCardinalityConnectionIds = cardIds,
                textMeasurer = textMeasurer,
            )
        else -> null
    }
}

private fun ConceptualSchema.applyKeyboardResize(
    selection: CanvasSelection,
    dir: ArrowAxisDir,
    step: Int,
    textMeasurer: TextMeasurer,
): ConceptualSchema? {
    val elemIds = selection.selectedElementIds()
    val cardIds = selection.selectedCardinalityConnectionIds()
    if (elemIds.isEmpty() && cardIds.isEmpty()) return null

    var s = this
    var changed = false
    val resizedElementIds = mutableSetOf<Int>()

    for (id in elemIds) {
        val el = s.elements[id] ?: continue
        if (!el.allowsManualResize()) continue
        val newPos = resizePositionFromKeyboard(el.position, dir, step)
        if (newPos != el.position) {
            s = s.withElement(el.withPosition(newPos))
            resizedElementIds.add(id)
            changed = true
        }
    }

    for (cid in cardIds) {
        val conn = s.connections.firstOrNull { it.id == cid } ?: continue
        if (conn.cardinalityAutoSize) continue
        val base = conn.cardinalityPosition
            ?: materializeCardinalityPositionForFixed(s, conn, textMeasurer)
            ?: continue
        val newPos = resizePositionFromKeyboard(base, dir, step)
        if (newPos != base) {
            s = s.copy(
                connections = s.connections.map { c ->
                    if (c.id == cid) c.copy(cardinalityPosition = newPos) else c
                },
            )
            changed = true
        }
    }

    if (!changed) return null

    if (resizedElementIds.isNotEmpty()) {
        s = s.withRecalculatedFloatingCardinalityPositions(
            onlyIncidentToElementId = resizedElementIds.singleOrNull(),
            textMeasurer = textMeasurer,
        )
    }

    return s
}
