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

package games.polyclub.kbrmodelo.domain

/**
 * Represents the current selection state on the conceptual schema canvas.
 *
 * Mirrors [TModelo.FSelecionado] from the original Pascal source: a model can
 * have exactly one selected object at a time (or nothing selected).
 *
 * Unlike the Pascal implementation — where [TCardinalidade] was a full [TBase]
 * component — cardinality labels are encoded as virtual selections that reference
 * the owning [Connection] by ID.
 */
sealed class CanvasSelection {

    /** Nothing is selected (corresponds to [TModelo.FSelecionado] == nil). */
    data object None : CanvasSelection()

    /**
     * A [SchemaElement] with the given [id] is selected.
     *
     * Covers entities, relationships, associative entities, attributes,
     * specializations, self-relationships, and annotations.
     */
    data class Element(val id: Int) : CanvasSelection()

    /**
     * The cardinality label of the [Connection] with the given [connectionId] is selected.
     *
     * Corresponds to [TCardinalidade] being the [TModelo.FSelecionado] in the original Pascal.
     */
    data class Cardinality(val connectionId: Int) : CanvasSelection()
}
