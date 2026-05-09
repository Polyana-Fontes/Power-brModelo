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

import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.SchemaHistory

/**
 * One editor tab: undo stack, live schema, inspector baseline, disk baseline, and canvas selection.
 */
internal data class EditorTabSession(
    val id: Long,
    val history: SchemaHistory,
    val schema: ConceptualSchema,
    val inspectorCommittedSchema: ConceptualSchema?,
    val savedDiskBaseline: ConceptualSchema?,
    val selection: CanvasSelection,
) {
    fun hasUnsavedChanges(): Boolean =
        savedDiskBaseline == null || schema != savedDiskBaseline

    /** True when closing should prompt (dirty state, excluding an untouched starter blank tab). */
    fun needsCloseConfirmation(): Boolean =
        hasUnsavedChanges() && !isReplaceableBlankStarter()

    fun displayTitle(): String = schema.name.trim().ifBlank { "Sem título" }

    /**
     * Initial blank canvas tab that has never been saved or edited on the undo stack.
     * Used to replace that tab when opening the first real file instead of keeping an empty tab forever.
     */
    fun isReplaceableBlankStarter(): Boolean {
        if (savedDiskBaseline != null) return false
        val s = schema
        if (s.filePath.isNotBlank() || s.openedFromBrm) return false
        if (s.elements.isNotEmpty() || s.connections.isNotEmpty()) return false
        return !history.canUndo && !history.canRedo
    }

    companion object {
        fun blank(id: Long): EditorTabSession {
            val empty = ConceptualSchema()
            val history = SchemaHistory(empty)
            return EditorTabSession(
                id = id,
                history = history,
                schema = empty,
                inspectorCommittedSchema = empty,
                savedDiskBaseline = null,
                selection = CanvasSelection.None,
            )
        }

        fun fromLoadedModel(id: Long, schema: ConceptualSchema): EditorTabSession {
            val history = SchemaHistory(schema)
            return EditorTabSession(
                id = id,
                history = history,
                schema = schema,
                inspectorCommittedSchema = schema,
                savedDiskBaseline = schema,
                selection = CanvasSelection.None,
            )
        }
    }
}
