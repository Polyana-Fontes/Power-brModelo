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

package games.polyclub.power.brmodelo.mcp

import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.CanvasSelectionRectangleMergeMode
import games.polyclub.power.brmodelo.domain.toMultiPickSets

internal object McpSelectionRectangleResponseJson {

    private fun jsonIntArray(sorted: List<Int>): String =
        sorted.joinToString(separator = ",", prefix = "[", postfix = "]")

    private fun jsonPickObject(elementIds: List<Int>, cardinalityIds: List<Int>): String =
        """{"elementIds":${jsonIntArray(elementIds)},"cardinalityConnectionIds":${jsonIntArray(cardinalityIds)}}"""

    private fun sortedPicks(sel: CanvasSelection): Pair<List<Int>, List<Int>> {
        val (e, c) = sel.toMultiPickSets()
        return e.sorted() to c.sorted()
    }

    fun canvasSelectionRectangleSuccess(
        resourceUri: String,
        dryRun: Boolean,
        mergeMode: CanvasSelectionRectangleMergeMode,
        requestWindowFocusRequested: Boolean,
        requestWindowFocusApplied: Boolean,
        selectionCommittedToUi: Boolean,
        bandElementIds: Set<Int>,
        bandCardinalityIds: Set<Int>,
        selectionBefore: CanvasSelection,
        selectionAfterProjection: CanvasSelection,
        selectionSymmetricDeltaElements: List<Int>,
        selectionSymmetricDeltaCardinality: List<Int>,
    ): String {
        val mm = mergeMode.name.lowercase()
        val (be, bc) = bandElementIds.sorted() to bandCardinalityIds.sorted()
        val (sbE, sbC) = sortedPicks(selectionBefore)
        val (saE, saC) = sortedPicks(selectionAfterProjection)
        val uriJson = mcpJsonStringLiteral(resourceUri)
        return buildString {
            append("""{"ok":true,"resourceUri":$uriJson,"dryRun":$dryRun,"mergeMode":"$mm",""")
            append("""requestWindowFocusRequested":$requestWindowFocusRequested,"requestWindowFocusApplied":$requestWindowFocusApplied,"selectionCommittedToUi":$selectionCommittedToUi,""")
            append("""objectsInBand":${jsonPickObject(be, bc)},""")
            append("""selectionUiBefore":${jsonPickObject(sbE, sbC)},""")
            append("""selectionUiAfter":${jsonPickObject(saE, saC)},""")
            append("""selectionSymmetricDelta":${jsonPickObject(selectionSymmetricDeltaElements, selectionSymmetricDeltaCardinality)}""")
            append('}')
        }
    }
}
