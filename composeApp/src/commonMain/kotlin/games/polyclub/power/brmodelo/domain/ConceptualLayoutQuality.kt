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

import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Heuristic: gaps smaller than this (edge-to-edge, pixels) are reported as uncomfortably tight. */
const val CONCEPTUAL_LAYOUT_TIGHT_CLEARANCE_MAX_PX: Int = 12

/**
 * Element ids taken from a [CanvasSelection] for layout diagnostics scope.
 * Returns null when the selection does not narrow to concrete element picks (full-tab scan is appropriate).
 */
fun canvasElementIdsForLayoutScope(selection: CanvasSelection): Set<Int>? = when (selection) {
    CanvasSelection.None -> null
    is CanvasSelection.Element -> setOf(selection.id)
    is CanvasSelection.Multiple -> selection.elementIds.takeIf { it.isNotEmpty() }
    is CanvasSelection.Cardinality -> null
}

private data class IntBox(val id: Int, val left: Int, val top: Int, val right: Int, val bottom: Int)

private fun SchemaElement.toBox(): IntBox {
    val p = position
    return IntBox(id, p.x, p.y, p.x + p.width, p.y + p.height)
}

/** Positive area overlap between two axis-aligned integer boxes. */
private fun boxesOverlapPositiveArea(a: IntBox, b: IntBox): Boolean {
    val xOverlap = min(a.right, b.right) - max(a.left, b.left)
    val yOverlap = min(a.bottom, b.bottom) - max(a.top, b.top)
    return xOverlap > 0 && yOverlap > 0
}

/**
 * Minimum pixel distance between box edges (0 when [boxesOverlapPositiveArea], 0 when touching at a point/edge).
 */
private fun boxEdgeDistancePx(a: IntBox, b: IntBox): Int {
    if (boxesOverlapPositiveArea(a, b)) return 0
    val xOverlap = min(a.right, b.right) - max(a.left, b.left)
    val yOverlap = min(a.bottom, b.bottom) - max(a.top, b.top)
    val dx = if (xOverlap > 0) 0 else max(0, max(a.left, b.left) - min(a.right, b.right))
    val dy = if (yOverlap > 0) 0 else max(0, max(a.top, b.top) - min(a.bottom, b.bottom))
    return ceil(hypot(dx.toDouble(), dy.toDouble())).toInt()
}

private fun orient(ax: Int, ay: Int, bx: Int, by: Int, cx: Int, cy: Int): Long =
    (bx.toLong() - ax) * (cy.toLong() - ay) - (by.toLong() - ay) * (cx.toLong() - ax)

private fun onSeg(ax: Int, ay: Int, bx: Int, by: Int, cx: Int, cy: Int): Boolean =
    cx <= max(ax, bx) && cx >= min(ax, bx) && cy <= max(ay, by) && cy >= min(ay, by)

/** Proper segment intersection (endpoints meeting alone does not count). */
internal fun segmentsIntersectProper(
    ax: Int,
    ay: Int,
    bx: Int,
    by: Int,
    cx: Int,
    cy: Int,
    dx: Int,
    dy: Int,
): Boolean {
    val o1 = orient(ax, ay, bx, by, cx, cy)
    val o2 = orient(ax, ay, bx, by, dx, dy)
    val o3 = orient(cx, cy, dx, dy, ax, ay)
    val o4 = orient(cx, cy, dx, dy, bx, by)
    if (o1 != 0L && o2 != 0L && o3 != 0L && o4 != 0L) {
        return (o1 > 0) != (o2 > 0) && (o3 > 0) != (o4 > 0)
    }
    if (o1 == 0L && onSeg(ax, ay, bx, by, cx, cy)) return true
    if (o2 == 0L && onSeg(ax, ay, bx, by, dx, dy)) return true
    if (o3 == 0L && onSeg(cx, cy, dx, dy, ax, ay)) return true
    if (o4 == 0L && onSeg(cx, cy, dx, dy, bx, by)) return true
    return false
}

private fun elementCenterInt(el: SchemaElement): Pair<Int, Int> {
    val p = el.position
    return (p.x + p.width / 2) to (p.y + p.height / 2)
}

data class ConceptualLayoutOverlapPair(
    val elementIdA: Int,
    val elementIdB: Int,
)

data class ConceptualLayoutTightClearancePair(
    val elementIdA: Int,
    val elementIdB: Int,
    val gapPx: Int,
)

data class ConceptualLayoutLineCrossing(
    val connectionIdA: Int,
    val connectionIdB: Int,
)

/**
 * Lightweight geometric diagnostics for MCP agents (approximate connection routing: center-to-center segments).
 */
data class ConceptualLayoutQualityReport(
    val overlaps: List<ConceptualLayoutOverlapPair>,
    val tightClearances: List<ConceptualLayoutTightClearancePair>,
    val lineCrossings: List<ConceptualLayoutLineCrossing>,
) {
    val hasAnyIssue: Boolean =
        overlaps.isNotEmpty() || tightClearances.isNotEmpty() || lineCrossings.isNotEmpty()
}

/**
 * Compact signals for MCP agents, derived from a [ConceptualLayoutQualityReport].
 *
 * [affectedElementIds] lists canvas element ids implicated in overlaps, tight clearances, and (when [schema]
 * is provided) endpoints of crossing connection pairs.
 */
data class ConceptualLayoutAgentSignals(
    val hasBlockingOverlap: Boolean,
    val affectedElementIds: List<Int>,
    val agentHint: String?,
)

fun conceptualLayoutAgentSignals(
    report: ConceptualLayoutQualityReport,
    schema: ConceptualSchema?,
): ConceptualLayoutAgentSignals {
    val affected = LinkedHashSet<Int>()
    for (p in report.overlaps) {
        affected.add(p.elementIdA)
        affected.add(p.elementIdB)
    }
    for (p in report.tightClearances) {
        affected.add(p.elementIdA)
        affected.add(p.elementIdB)
    }
    if (schema != null) {
        for (xc in report.lineCrossings) {
            val c1 = schema.connections.find { it.id == xc.connectionIdA }
            val c2 = schema.connections.find { it.id == xc.connectionIdB }
            c1?.let {
                affected.add(it.elementIdA)
                affected.add(it.elementIdB)
            }
            c2?.let {
                affected.add(it.elementIdA)
                affected.add(it.elementIdB)
            }
        }
    }
    val hasBlockingOverlap = report.overlaps.isNotEmpty()
    val agentHint = when {
        report.overlaps.isNotEmpty() || report.tightClearances.isNotEmpty() -> "spacing"
        report.lineCrossings.isNotEmpty() -> "routing"
        else -> null
    }
    return ConceptualLayoutAgentSignals(
        hasBlockingOverlap = hasBlockingOverlap,
        affectedElementIds = affected.sorted(),
        agentHint = agentHint,
    )
}

private fun pairTouchesScope(a: Int, b: Int, scope: Set<Int>?): Boolean =
    scope == null || a in scope || b in scope

private fun connectionTouchesScope(c: Connection, scope: Set<Int>?): Boolean =
    scope == null || c.elementIdA in scope || c.elementIdB in scope

/**
 * Scans [schema] for overlapping element boxes, uncomfortably small clearances, and approximate link crossings.
 *
 * When [elementIdsScope] is null, the whole diagram is scanned. Otherwise only issues touching at least one id
 * in the set are included.
 */
fun analyzeConceptualLayoutQuality(
    schema: ConceptualSchema,
    elementIdsScope: Set<Int>?,
): ConceptualLayoutQualityReport {
    val boxes = schema.elements.values.map { it.toBox() }.sortedBy { it.id }
    val overlaps = ArrayList<ConceptualLayoutOverlapPair>()
    val tight = ArrayList<ConceptualLayoutTightClearancePair>()
    for (i in boxes.indices) {
        for (j in i + 1 until boxes.size) {
            val a = boxes[i]
            val b = boxes[j]
            if (!pairTouchesScope(a.id, b.id, elementIdsScope)) continue
            if (boxesOverlapPositiveArea(a, b)) {
                overlaps.add(ConceptualLayoutOverlapPair(min(a.id, b.id), max(a.id, b.id)))
                continue
            }
            val gap = boxEdgeDistancePx(a, b)
            if (gap in 1 until CONCEPTUAL_LAYOUT_TIGHT_CLEARANCE_MAX_PX) {
                tight.add(ConceptualLayoutTightClearancePair(min(a.id, b.id), max(a.id, b.id), gap))
            }
        }
    }

    val crossings = ArrayList<ConceptualLayoutLineCrossing>()
    val conns = schema.connections.sortedBy { it.id }
    for (i in conns.indices) {
        for (j in i + 1 until conns.size) {
            val c1 = conns[i]
            val c2 = conns[j]
            if (!connectionTouchesScope(c1, elementIdsScope) && !connectionTouchesScope(c2, elementIdsScope)) {
                continue
            }
            val e1a = schema.elements[c1.elementIdA] ?: continue
            val e1b = schema.elements[c1.elementIdB] ?: continue
            val e2a = schema.elements[c2.elementIdA] ?: continue
            val e2b = schema.elements[c2.elementIdB] ?: continue
            val p1a = elementCenterInt(e1a)
            val p1b = elementCenterInt(e1b)
            val p2a = elementCenterInt(e2a)
            val p2b = elementCenterInt(e2b)
            val shared = setOf(c1.elementIdA, c1.elementIdB).intersect(setOf(c2.elementIdA, c2.elementIdB))
            if (shared.isNotEmpty()) continue
            if (segmentsIntersectProper(p1a.first, p1a.second, p1b.first, p1b.second, p2a.first, p2a.second, p2b.first, p2b.second)) {
                crossings.add(ConceptualLayoutLineCrossing(min(c1.id, c2.id), max(c1.id, c2.id)))
            }
        }
    }
    return ConceptualLayoutQualityReport(overlaps, tight, crossings)
}
