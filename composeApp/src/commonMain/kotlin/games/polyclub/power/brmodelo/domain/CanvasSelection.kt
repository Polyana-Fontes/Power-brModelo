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
 * Represents the current selection state on the conceptual schema canvas.
 *
 * Mirrors [TModelo.FSelecionado] from the original Pascal source: typically one selected object,
 * or [Multiple] after a rubber-band / Shift multi-select.
 *
 * Unlike the Pascal implementation — where [TCardinalidade] was a full [TBase]
 * component — cardinality labels are encoded as virtual selections that reference
 * the owning [games.polyclub.power.brmodelo.domain.Connection] by ID.
 */
sealed class CanvasSelection {

    /** Nothing is selected (corresponds to [TModelo.FSelecionado] == nil). */
    data object None : CanvasSelection()

    /**
     * A [games.polyclub.power.brmodelo.domain.SchemaElement] with the given [id] is selected.
     *
     * Covers entities, relationships, associative entities, attributes,
     * specializations, self-relationships, and annotations.
     */
    data class Element(val id: Int) : CanvasSelection()

    /**
     * Several picks are selected (rectangle tool, Shift+click, or both).
     *
     * [elementIds] lists [SchemaElement] ids; [cardinalityConnectionIds] lists [Connection] ids whose
     * cardinality label is part of the selection (labels are not schema elements).
     */
    data class Multiple(
        val elementIds: Set<Int> = emptySet(),
        val cardinalityConnectionIds: Set<Int> = emptySet(),
    ) : CanvasSelection()

    /**
     * The cardinality label of the [games.polyclub.power.brmodelo.domain.Connection] with the given [connectionId] is selected.
     *
     * Corresponds to [TCardinalidade] being the [TModelo.FSelecionado] in the original Pascal.
     */
    data class Cardinality(val connectionId: Int) : CanvasSelection()
}

/** Number of distinct picks in a multi-selection (elements + cardinality labels). */
fun CanvasSelection.Multiple.totalPickCount(): Int =
    elementIds.size + cardinalityConnectionIds.size

/** Count of selected canvas picks (0, 1, or multi total). Used by the inspector summary. */
fun CanvasSelection.selectedPickCount(): Int = when (this) {
    CanvasSelection.None -> 0
    is CanvasSelection.Element,
    is CanvasSelection.Cardinality,
    -> 1
    is CanvasSelection.Multiple -> totalPickCount()
}

/**
 * Builds the selection after a geometric rectangle pick on the schema.
 *
 * When [additive] is true, union [bandElementIds] / [bandCardinalityIds] with the picks already in
 * [selectionAtStart]; when false, the band replaces the selection (unless both band sets are empty,
 * in which case the result is [CanvasSelection.None]).
 */
fun mergeCanvasBandPick(
    additive: Boolean,
    selectionAtStart: CanvasSelection,
    bandElementIds: Set<Int>,
    bandCardinalityIds: Set<Int>,
): CanvasSelection {
    val (e0, c0) = selectionAtStart.toMultiPickSets()
    val e = if (additive) e0 + bandElementIds else bandElementIds
    val c = if (additive) c0 + bandCardinalityIds else bandCardinalityIds
    return canvasSelectionFromPickSets(e, c)
}

/** Canonical [CanvasSelection] from merged pick sets (empty → [CanvasSelection.None]). */
fun canvasSelectionFromPickSets(
    elementIds: Set<Int>,
    cardinalityConnectionIds: Set<Int>,
): CanvasSelection {
    if (elementIds.isEmpty() && cardinalityConnectionIds.isEmpty()) return CanvasSelection.None
    return CanvasSelection.Multiple(elementIds = elementIds, cardinalityConnectionIds = cardinalityConnectionIds)
}

/**
 * Selects every [ConceptualSchema.elements] id in [schema]. Cardinality-only picks are omitted.
 * Empty model → [CanvasSelection.None].
 */
fun canvasSelectionSelectAllElements(schema: ConceptualSchema): CanvasSelection =
    canvasSelectionFromPickSets(schema.elements.keys.toSet(), emptySet())

/**
 * Splits the current selection into sets used for Shift additive multi-select.
 * [Element] and [Cardinality] each contribute one pick; [Multiple] is merged as-is.
 */
fun CanvasSelection.toMultiPickSets(): Pair<Set<Int>, Set<Int>> = when (this) {
    is CanvasSelection.Element -> setOf(id) to emptySet()
    is CanvasSelection.Cardinality -> emptySet<Int>() to setOf(connectionId)
    is CanvasSelection.Multiple -> elementIds to cardinalityConnectionIds
    CanvasSelection.None -> emptySet<Int>() to emptySet()
}

/** Shift+ additive click: add [elementId], or remove it if already selected. */
fun toggleElementInMultiSelection(current: CanvasSelection, elementId: Int): CanvasSelection {
    val (e, c) = current.toMultiPickSets()
    val nextE = if (elementId in e) e - elementId else e + elementId
    return canvasSelectionFromPickSets(nextE, c)
}

/** Shift+ additive click: add cardinality label [connectionId], or remove if already selected. */
fun toggleCardinalityInMultiSelection(current: CanvasSelection, connectionId: Int): CanvasSelection {
    val (e, c) = current.toMultiPickSets()
    val nextC = if (connectionId in c) c - connectionId else c + connectionId
    return canvasSelectionFromPickSets(e, nextC)
}
