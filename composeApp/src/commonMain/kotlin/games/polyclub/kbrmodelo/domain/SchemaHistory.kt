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
 * Undo/redo history for a [ConceptualSchema].
 *
 * Stores a linear sequence of immutable schema snapshots using two [ArrayDeque] stacks.
 * Every call to [push] creates a new undo checkpoint; every [undo]/[redo] restores
 * a previous snapshot.
 *
 * Usage pattern in the UI:
 * - **Live preview** (e.g. dragging an element): update the displayed schema without
 *   calling [push] so the intermediate frames are not undoable.
 * - **Commit** (e.g. pointer up after drag, text field blur): call [push] to record
 *   the final state as an undoable checkpoint.
 *
 * This class is NOT thread-safe and must be accessed from the UI thread only.
 *
 * @param initial The schema state to start from (typically null for a fresh app launch).
 */
class SchemaHistory(initial: ConceptualSchema?) {

    private val undoStack = ArrayDeque<ConceptualSchema?>()
    private val redoStack = ArrayDeque<ConceptualSchema?>()

    /** The currently committed schema state. */
    var current: ConceptualSchema? = initial
        private set

    /** True when there is at least one state to undo. */
    val canUndo: Boolean get() = undoStack.isNotEmpty()

    /** True when there is at least one state to redo. */
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /**
     * Records [newSchema] as the new committed state.
     *
     * The previous [current] is pushed onto the undo stack and the redo stack
     * is cleared (a new action always invalidates the redo branch).
     */
    fun push(newSchema: ConceptualSchema?) {
        undoStack.addLast(current)
        redoStack.clear()
        current = newSchema
    }

    /**
     * Replaces [current] without pushing an undo checkpoint (e.g. updating [ConceptualSchema.filePath]
     * after a successful Save).
     */
    fun syncCurrent(newSchema: ConceptualSchema?) {
        current = newSchema
    }

    /**
     * Reverts to the previous committed state.
     *
     * If there is nothing to undo, returns [current] unchanged.
     *
     * @return The schema state after undoing.
     */
    fun undo(): ConceptualSchema? {
        if (!canUndo) return current
        redoStack.addLast(current)
        current = undoStack.removeLast()
        return current
    }

    /**
     * Re-applies the most recently undone state.
     *
     * If there is nothing to redo, returns [current] unchanged.
     *
     * @return The schema state after redoing.
     */
    fun redo(): ConceptualSchema? {
        if (!canRedo) return current
        undoStack.addLast(current)
        current = redoStack.removeLast()
        return current
    }
}
