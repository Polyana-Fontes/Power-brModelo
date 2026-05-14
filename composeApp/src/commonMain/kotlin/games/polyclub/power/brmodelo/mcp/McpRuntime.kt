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

import games.polyclub.power.brmodelo.domain.ConceptualProceduralToolKind
import games.polyclub.power.brmodelo.domain.ConceptualProceduralToolOverrides
import games.polyclub.power.brmodelo.domain.ConceptualSpecializationToolVariant
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ConceptualSearchHit
import games.polyclub.power.brmodelo.domain.ConceptualSearchOutcome
import games.polyclub.power.brmodelo.domain.ConceptualSearchTextScope
import games.polyclub.power.brmodelo.domain.ConceptualSearchTypeFilters
import games.polyclub.power.brmodelo.ui.EditorTabSession

/**
 * Snapshot of open editor tabs exposed to the MCP server (read on the UI thread).
 */
internal data class McpTabSnapshot(
    val sessions: List<EditorTabSession>,
    val selectedIndex: Int,
)

/**
 * UI-thread actions invoked from MCP tool handlers (desktop only).
 */
internal class McpUiBindings(
    val current: () -> McpTabSnapshot,
    val onSelectTab: (Int) -> Unit,
    val onAddBlankTab: () -> Unit,
    val onForceCloseTab: (Int) -> Unit,
    val onRequestCloseTab: (Int) -> Unit,
    val onSaveTab: (Int, Boolean) -> Boolean,
    /** Loads a model from an absolute file path; returns null on success or a short error code/message. */
    val onOpenModelFileAtPath: (String) -> String?,
    /**
     * Opens conceptual XML from UTF-8 text. [fileName] is basename only (no path); used for the tab title.
     * Returns null on success or a short error code/message.
     */
    val onOpenXmlAsUnsavedTab: (fileName: String, xmlUtf8: String) -> String?,
    /**
     * Parses [xmlUtf8] as conceptual MER XML and replaces the tab schema in one undoable step.
     * Preserves the tab's disk path and opened-from-brm flags on the merged schema.
     */
    val onReplaceModelXmlAtTab: (tabIndex: Int, xmlUtf8: String) -> String?,
    /**
     * Runs a search/replace on the tab's serialized XML, re-parses, and commits in one undoable step.
     * When [replaceAll] is false, [oldString] must appear exactly once in the serialized XML.
     */
    val onPatchModelXmlAtTab: (tabIndex: Int, oldString: String, newString: String, replaceAll: Boolean) -> String?,
    /**
     * Inserts an entity, relationship, or associative entity using the same domain placement rules as the
     * canvas tools, without changing the user's active ribbon/canvas tool selection.
     */
    val onPlaceProceduralConceptualToolAtTab: (
        tabIndex: Int,
        kind: ConceptualProceduralToolKind,
        topLeftX: Int,
        topLeftY: Int,
        overrides: ConceptualProceduralToolOverrides,
    ) -> McpProceduralToolApplyOutcome,
    /**
     * Applies one conceptual specialization ribbon variant on [baseEntityId] (must be invoked on the UI thread).
     * Same domain rules as clicking the specialization tool on that entity; does not change the active canvas tool.
     */
    val onApplyConceptualSpecializationAtTab: (
        tabIndex: Int,
        baseEntityId: Int,
        variant: ConceptualSpecializationToolVariant,
    ) -> McpProceduralToolApplyOutcome,
    /**
     * Runs the same conceptual search as the **Localizar** dialog on [tabIndex] (must be invoked on the UI thread).
     */
    val onConceptualSearchFind: (
        tabIndex: Int,
        query: String,
        typeFilters: ConceptualSearchTypeFilters,
        textScope: ConceptualSearchTextScope,
    ) -> ConceptualSearchOutcome,
    /**
     * Selects the canvas target, optional hidden-attribute path, inspector tab, and canvas pan focus for one hit.
     * Returns null on success or a short error code.
     */
    val onConceptualSearchApplyHit: (tabIndex: Int, hit: ConceptualSearchHit) -> String?,
    val onNotifyUser: (String) -> Unit,
    val onServerRunningChanged: (Boolean) -> Unit,
)

/**
 * Desktop MCP server + settings; WASM [actual] is a no-op stub.
 */
internal expect class McpRuntime() {
    fun setSettingsDialogOpener(opener: () -> Unit)

    fun updateBindings(bindings: McpUiBindings?)

    fun openSettingsDialog()

    fun startServer(): Boolean

    fun stopServer()

    fun isServerRunning(): Boolean

    fun onTabsChanged()

    fun shutdown()
}

internal fun McpTabSnapshot.schemaForTab(index: Int): ConceptualSchema? =
    sessions.getOrNull(index)?.schema
