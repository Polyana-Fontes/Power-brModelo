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

import androidx.compose.ui.geometry.Offset
import games.polyclub.power.brmodelo.domain.ConceptualProceduralToolKind
import games.polyclub.power.brmodelo.domain.ConceptualProceduralToolOverrides
import games.polyclub.power.brmodelo.domain.ConceptualLinkConnectionOverridePatch
import games.polyclub.power.brmodelo.domain.ConceptualLinkPick
import games.polyclub.power.brmodelo.domain.ConceptualSpecializationToolVariant
import games.polyclub.power.brmodelo.domain.HiddenAttribute
import games.polyclub.power.brmodelo.domain.ConceptualAttributeAttachPonto
import games.polyclub.power.brmodelo.domain.ConceptualAttributeToolVariant
import games.polyclub.power.brmodelo.domain.ConceptualCompositeLeafSpec
import games.polyclub.power.brmodelo.domain.ConceptualSimpleAttributePlacementOverrides
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.CanvasSelectionRectangleMergeMode
import games.polyclub.power.brmodelo.domain.ConceptualSearchHit
import games.polyclub.power.brmodelo.domain.ConceptualSearchOutcome
import games.polyclub.power.brmodelo.domain.ConceptualSearchTextScope
import games.polyclub.power.brmodelo.domain.ConceptualSearchTypeFilters
import games.polyclub.power.brmodelo.ui.ConceptualSubsetRasterEncodeResult
import games.polyclub.power.brmodelo.ui.ConceptualSubsetRasterFormat
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
    /** Returns the new tab's stable [games.polyclub.power.brmodelo.ui.EditorTabSession.id] (XML/PNG/JPEG URIs use this id). */
    val onAddBlankTab: () -> Long,
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
    val onApplySimpleConceptualAttributeAtTab: (
        tabIndex: Int,
        targetElementId: Int,
        variant: ConceptualAttributeToolVariant,
        attachSide: ConceptualAttributeAttachPonto?,
        overrides: ConceptualSimpleAttributePlacementOverrides?,
    ) -> McpProceduralToolApplyOutcome,
    val onApplyCompositeConceptualAttributeAtTab: (
        tabIndex: Int,
        targetElementId: Int,
        attachSide: ConceptualAttributeAttachPonto?,
        leafSpecs: List<ConceptualCompositeLeafSpec>,
        nestedHiddenAttributes: List<HiddenAttribute>,
    ) -> McpProceduralToolApplyOutcome,
    val onApplyHiddenAttributeForestAtTab: (
        tabIndex: Int,
        holderElementId: Int,
        newRoots: List<HiddenAttribute>,
    ) -> McpProceduralToolApplyOutcome,
    /**
     * Same conceptual rules as the editor **Ligar Objetos** tool (two picks in one call). Must run on the UI thread.
     * When [endA] and [endB] denote the same entity auto-relationship, [autoSelfRelationshipClickSchema] is optional
     * schema-space coordinates (same as canvas); `null` uses the legacy right-side diamond placement.
     * When [dryRun] is true, validate and return preview JSON (including merged projected `layoutQuality`) without
     * committing the tab (no undo entry).
     */
    val onLinkConceptualObjectsAtTab: (
        tabIndex: Int,
        endA: ConceptualLinkPick,
        endB: ConceptualLinkPick,
        relationshipOverrides: ConceptualProceduralToolOverrides?,
        connectionPatches: List<ConceptualLinkConnectionOverridePatch>?,
        autoSelfRelationshipClickSchema: Offset?,
        dryRun: Boolean,
    ) -> McpProceduralToolApplyOutcome,
    /** Inspector-aligned model metadata edits (one undo step). */
    val onApplyEditConceptualModelAtTab: (tabIndex: Int, patch: Map<String, Any?>) -> McpProceduralToolApplyOutcome,
    /** Inspector-aligned canvas element property edits (one undo step). */
    val onApplyEditCanvasElementAtTab: (tabIndex: Int, elementId: Int, patch: Map<String, Any?>) -> McpProceduralToolApplyOutcome,
    /** Inspector-aligned connection / cardinality edits (one undo step). */
    val onApplyEditConnectionAtTab: (tabIndex: Int, connectionId: Int, patch: Map<String, Any?>) -> McpProceduralToolApplyOutcome,
    /** Inspector-aligned hidden-attribute node edits by tree path (one undo step). */
    val onApplyEditHiddenAttributeAtTab: (tabIndex: Int, holderElementId: Int, path: List<Int>, patch: Map<String, Any?>) -> McpProceduralToolApplyOutcome,
    /**
     * **Operações → Organizar Atributos** on [tabIndex] (one undo step). When [attributeSides] is null or empty,
     * all attach sides are reorganized like the menu; otherwise only those sides (left / top / right / bottom).
     */
    val onApplyOrganizeAttributesMenuAtTab: (
        tabIndex: Int,
        attributeSides: Set<ConceptualAttributeAttachPonto>?,
    ) -> McpProceduralToolApplyOutcome,
    /**
     * Translates canvas elements by delta in schema coordinates; expands owned on-canvas attributes when requested.
     * One undo step; cardinality updates match canvas drag (fixed vs floating).
     */
    val onApplyMoveCanvasElementsAtTab: (
        tabIndex: Int,
        seedElementIds: List<Int>,
        deltaX: Int,
        deltaY: Int,
        moveOwnedCanvasAttributes: Boolean,
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
    /**
     * Encodes the tab's conceptual schema as PNG bytes (same as **Exportar em PNG**). Invoked on the UI thread.
     */
    val onEncodeTabConceptualMenuExportPng: (tabIndex: Int) -> ByteArray?,
    /**
     * Encodes the tab's conceptual schema as JPEG bytes (same as **Exportar em JPEG**). Invoked on the UI thread.
     */
    val onEncodeTabConceptualMenuExportJpeg: (tabIndex: Int) -> ByteArray?,
    /**
     * Encodes a subset of canvas elements as PNG or JPEG (same subgraph + crop as Ctrl+C clipboard preview;
     * JPEG uses opaque canvas-gray background like **Exportar em JPEG**). Invoked on the UI thread.
     */
    val onEncodeConceptualElementSubsetRaster: (
        tabIndex: Int,
        seedElementIds: List<Int>,
        format: ConceptualSubsetRasterFormat,
    ) -> ConceptualSubsetRasterEncodeResult?,
    /**
     * Marquee-style rectangle selection in schema coordinates (same geometric pick as the canvas tool).
     * [dryRun] returns hits and projected selection without mutating the UI (window focus is ignored).
     * Invoked on the UI thread.
     */
    val onApplyCanvasSelectionRectangleAtTab: (
        tabIndex: Int,
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int,
        mergeMode: CanvasSelectionRectangleMergeMode,
        dryRun: Boolean,
        requestWindowFocus: Boolean,
    ) -> McpProceduralToolApplyOutcome,
    /**
     * Replaces the canvas selection on [tabIndex] (no schema mutation; not an undo step).
     */
    val onSetCanvasSelectionAtTab: (tabIndex: Int, selection: CanvasSelection) -> Unit,
    /** Desktop: raises the main editor window; no-op on WASM. */
    val onRequestAppWindowFocus: () -> Unit,
    /** Shows a short snackbar so the user knows MCP changed focus, selection, or tab. */
    val onShowMcpAgentUserNotice: (McpAgentUserNotice) -> Unit,
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
