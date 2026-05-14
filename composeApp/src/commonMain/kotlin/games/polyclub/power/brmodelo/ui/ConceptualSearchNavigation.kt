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

package games.polyclub.power.brmodelo.ui

import androidx.compose.ui.text.TextMeasurer
import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ConceptualSearchHit
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.ui.canvas.connectionCardinalityBoxForModel

/**
 * Maps a conceptual search hit to selection, inspector tab, hidden-attribute reveal path,
 * and optional canvas bounds to centre the viewport on.
 */
internal data class ConceptualSearchNavigateAction(
    val selection: CanvasSelection,
    val hiddenAttributeRevealPath: List<Int>?,
    val inspectorTab: InspectorTab,
    val centerOnBounds: ElementPosition?,
)

internal fun conceptualSearchNavigateAction(
    schema: ConceptualSchema,
    hit: ConceptualSearchHit,
    textMeasurer: TextMeasurer,
): ConceptualSearchNavigateAction? {
    return when (hit) {
        is ConceptualSearchHit.ElementHit -> {
            if (schema.elements[hit.elementId] == null) null
            else ConceptualSearchNavigateAction(
                selection = CanvasSelection.Element(hit.elementId),
                hiddenAttributeRevealPath = null,
                inspectorTab = InspectorTab.Selecao,
                centerOnBounds = hit.position,
            )
        }
        is ConceptualSearchHit.CardinalityHit -> {
            val conn = schema.connections.firstOrNull { it.id == hit.connectionId } ?: return null
            val box = hit.position
                ?: connectionCardinalityBoxForModel(schema, conn, textMeasurer)
            ConceptualSearchNavigateAction(
                selection = CanvasSelection.Cardinality(hit.connectionId),
                hiddenAttributeRevealPath = null,
                inspectorTab = InspectorTab.Selecao,
                centerOnBounds = box,
            )
        }
        is ConceptualSearchHit.HiddenHit -> {
            val owner = schema.elements[hit.ownerElementId] ?: return null
            ConceptualSearchNavigateAction(
                selection = CanvasSelection.Element(hit.ownerElementId),
                hiddenAttributeRevealPath = hit.path,
                inspectorTab = InspectorTab.AtrOcultos,
                centerOnBounds = owner.position,
            )
        }
    }
}
