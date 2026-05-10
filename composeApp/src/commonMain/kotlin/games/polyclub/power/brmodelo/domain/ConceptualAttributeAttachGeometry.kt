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

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Which side of an owner box an attribute is attached to (Pascal `MePonto` / `OrganizeAtributos` cases).
 * Codes match `TLigacao` ponto indices: 1=left, 2=top, 3=right, 4=bottom.
 */
enum class ConceptualAttributeAttachPonto(val pascalCode: Int) {
    LEFT(1),
    TOP(2),
    RIGHT(3),
    BOTTOM(4),
    ;

    companion object {
        fun fromPascalCode(code: Int): ConceptualAttributeAttachPonto? =
            entries.firstOrNull { it.pascalCode == code }
    }
}

/**
 * Determines which edge of [elemPos] connects to an attribute at [attrPos], using the same
 * non-overlap rules as [games.polyclub.power.brmodelo.ui.canvas.SchemaRenderer] (formerly `attrPontoByPosition`).
 */
internal fun conceptualAttributeAttachPonto(elemPos: ElementPosition, attrPos: ElementPosition): Int {
    val eLeft = elemPos.x.toFloat()
    val eRight = (elemPos.x + elemPos.width).toFloat()
    val eTop = elemPos.y.toFloat()
    val eBottom = (elemPos.y + elemPos.height).toFloat()
    val aLeft = attrPos.x.toFloat()
    val aRight = (attrPos.x + attrPos.width).toFloat()
    val aTop = attrPos.y.toFloat()
    val aBottom = (attrPos.y + attrPos.height).toFloat()
    return when {
        aRight <= eLeft -> 1
        aLeft >= eRight -> 3
        aBottom <= eTop -> 2
        aTop >= eBottom -> 4
        else -> conceptualAttributeAngleFallbackPonto(elemPos, attrPos)
    }
}

/**
 * Angle-based quadrant fallback when attribute bbox overlaps the owner (unusual).
 */
private fun conceptualAttributeAngleFallbackPonto(pos: ElementPosition, attrPos: ElementPosition): Int {
    val dx = (attrPos.x + attrPos.width / 2.0) - (pos.x + pos.width / 2.0)
    val dy = (attrPos.y + attrPos.height / 2.0) - (pos.y + pos.height / 2.0)
    val angle = atan2(dy, dx) * (180.0 / PI)
    return when {
        angle >= -135.0 && angle < -45.0 -> 2
        angle >= -45.0 && angle < 45.0 -> 3
        angle >= 45.0 && angle < 135.0 -> 4
        else -> 1
    }
}

private fun distancePointToSegment(
    px: Float,
    py: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    val lenSq = dx * dx + dy * dy
    if (lenSq < 1e-6f) return hypot(px - x1, py - y1)
    var t = ((px - x1) * dx + (py - y1) * dy) / lenSq
    t = t.coerceIn(0f, 1f)
    val qx = x1 + t * dx
    val qy = y1 + t * dy
    return hypot(px - qx, py - qy)
}

/**
 * Picks the owner edge closest to [click] (schema coordinates). Tie-break: LEFT, TOP, RIGHT, BOTTOM.
 */
internal fun closestConceptualAttributeAttachPonto(owner: ElementPosition, click: Offset): ConceptualAttributeAttachPonto {
    val px = click.x
    val py = click.y
    val left = owner.x.toFloat()
    val top = owner.y.toFloat()
    val right = left + owner.width
    val bottom = top + owner.height

    data class Cand(val side: ConceptualAttributeAttachPonto, val d: Float)
    val cands = listOf(
        Cand(ConceptualAttributeAttachPonto.LEFT, distancePointToSegment(px, py, left, top, left, bottom)),
        Cand(ConceptualAttributeAttachPonto.TOP, distancePointToSegment(px, py, left, top, right, top)),
        Cand(ConceptualAttributeAttachPonto.RIGHT, distancePointToSegment(px, py, right, top, right, bottom)),
        Cand(ConceptualAttributeAttachPonto.BOTTOM, distancePointToSegment(px, py, left, bottom, right, bottom)),
    )
    return cands.minWith(
        compareBy<Cand> { it.d }
            .thenBy { it.side.pascalCode },
    ).side
}
