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

import androidx.compose.ui.text.TextMeasurer
import games.polyclub.power.brmodelo.domain.Connection
import games.polyclub.power.brmodelo.domain.ConceptualSchema

/**
 * Applies the same cardinality label side-effects as [games.polyclub.power.brmodelo.ui.InspectorPanel] when
 * toggling **Fixar posição** or **Tamanho aut.** (materializes a stored box when needed).
 */
fun ConceptualSchema.withConnectionCardinalityInspectorParity(
    previous: Connection,
    updated: Connection,
    textMeasurer: TextMeasurer,
): ConceptualSchema {
    if (updated.id != previous.id) return this
    var conn = updated
    when {
        !previous.cardinalityFixed && conn.cardinalityFixed -> {
            val p = conn.cardinalityPosition
            if (p == null || p.width <= 0 || p.height <= 0) {
                materializeCardinalityPositionForFixed(this, conn, textMeasurer)?.let { pos ->
                    conn = conn.copy(cardinalityPosition = pos)
                }
            }
        }
        previous.cardinalityFixed && !conn.cardinalityFixed -> {
            val unfixed = conn.copy(cardinalityFixed = false)
            val pos = materializeCardinalityPositionForFixed(this, unfixed, textMeasurer)
            conn = unfixed.copy(cardinalityPosition = pos ?: unfixed.cardinalityPosition)
        }
    }
    if (!conn.cardinalityAutoSize && previous.cardinalityAutoSize) {
        val p = conn.cardinalityPosition
        if (p == null || p.width <= 0 || p.height <= 0) {
            materializeCardinalityPositionForFixed(this, conn, textMeasurer)?.let { pos ->
                conn = conn.copy(cardinalityPosition = pos)
            }
        }
    }
    if (!previous.showCardinality && conn.showCardinality && conn.cardinality != null) {
        val p = conn.cardinalityPosition
        if (p == null || p.width <= 0 || p.height <= 0) {
            conn = enrichConnectionWithInitialCardinalityPosition(this, conn, textMeasurer)
        }
    }
    return copy(connections = connections.map { if (it.id == conn.id) conn else it })
}
