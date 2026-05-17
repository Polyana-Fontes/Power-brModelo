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

package games.polyclub.power.brmodelo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.rememberTextMeasurer
import games.polyclub.power.brmodelo.domain.applyAppendHiddenAttributeForest
import games.polyclub.power.brmodelo.domain.applyConceptualCompositeAttributeWithLeafChildren
import games.polyclub.power.brmodelo.domain.applyConceptualSimpleAttributeTool
import games.polyclub.power.brmodelo.domain.ConceptualAppendHiddenAttributesResult
import games.polyclub.power.brmodelo.domain.ConceptualAttributeToolResult
import games.polyclub.power.brmodelo.domain.applyDictionarySlots
import games.polyclub.power.brmodelo.domain.buildConceptualClipboardPayload
import games.polyclub.power.brmodelo.domain.canOpenBulkDataDictionaryForSelection
import games.polyclub.power.brmodelo.domain.collectDictionarySlotsForSelection
import games.polyclub.power.brmodelo.domain.ConceptualDictionarySlotRow
import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.CanvasSelectionRectangleMergeMode
import games.polyclub.power.brmodelo.domain.analyzeConceptualLayoutQuality
import games.polyclub.power.brmodelo.domain.canvasElementIdsForLayoutScope
import games.polyclub.power.brmodelo.domain.classifyMcpLinkObjectsPattern
import games.polyclub.power.brmodelo.domain.ConceptualBulkDeleteBand
import games.polyclub.power.brmodelo.domain.canvasSelectionSymmetricPickDelta
import games.polyclub.power.brmodelo.domain.mergeCanvasRectangleSelection
import games.polyclub.power.brmodelo.domain.toMultiPickSets
import games.polyclub.power.brmodelo.domain.deleteCanvasSelection
import games.polyclub.power.brmodelo.domain.elementIdsForClipboard
import games.polyclub.power.brmodelo.domain.extractClipboardFragment
import games.polyclub.power.brmodelo.domain.ConceptualPasteContext
import games.polyclub.power.brmodelo.domain.ConceptualPlacementDefaults
import games.polyclub.power.brmodelo.domain.ConceptualProceduralToolPlacementResult
import games.polyclub.power.brmodelo.domain.pasteConceptualClipboard
import games.polyclub.power.brmodelo.domain.placeProceduralConceptualTool
import games.polyclub.power.brmodelo.mcp.McpAgentUserNotice
import games.polyclub.power.brmodelo.mcp.McpConnectionToolResponseJson
import games.polyclub.power.brmodelo.mcp.McpConceptualToolElementResponseJson
import games.polyclub.power.brmodelo.mcp.McpDesktopSync
import games.polyclub.power.brmodelo.mcp.McpProceduralToolApplyOutcome
import games.polyclub.power.brmodelo.mcp.McpProceduralToolLayoutQualityScan
import games.polyclub.power.brmodelo.mcp.McpModelXmlPatch
import games.polyclub.power.brmodelo.mcp.McpRuntime
import games.polyclub.power.brmodelo.mcp.McpSelectionRectangleResponseJson
import games.polyclub.power.brmodelo.mcp.McpSettingsDialog
import games.polyclub.power.brmodelo.mcp.McpSettingsStore
import games.polyclub.power.brmodelo.mcp.formatMcpAgentUserNoticePtBr
import games.polyclub.power.brmodelo.mcp.mcpJsonStringLiteral
import games.polyclub.power.brmodelo.mcp.mcpServerUrlFromStoredSettings
import games.polyclub.power.brmodelo.mcp.modelResourceUriForSession
import games.polyclub.power.brmodelo.mcp.tryLoadPickedFileFromAbsolutePath
import games.polyclub.power.brmodelo.domain.ConceptualAttributeToolVariant
import games.polyclub.power.brmodelo.domain.applyHideCanvasAttribute
import games.polyclub.power.brmodelo.domain.applyRevealHiddenAttribute
import games.polyclub.power.brmodelo.domain.canHideCanvasAttributeMenu
import games.polyclub.power.brmodelo.domain.canRevealHiddenAttributeMenu
import games.polyclub.power.brmodelo.domain.hiddenAttributeStorageOwnerId
import games.polyclub.power.brmodelo.domain.ultimateNonAttributeOwner
import games.polyclub.power.brmodelo.domain.applyConvertOptionalSpecializationsToRestricted
import games.polyclub.power.brmodelo.domain.applyConvertRestrictedSpecializationToOptionals
import games.polyclub.power.brmodelo.domain.applyMergeEntityAndRelationshipToAssociative
import games.polyclub.power.brmodelo.domain.canConvertOptionalSpecializationsToRestrictedMenu
import games.polyclub.power.brmodelo.domain.canConvertRestrictedSpecializationToOptionalsMenu
import games.polyclub.power.brmodelo.domain.entityAndRelationshipIdsForMerge
import games.polyclub.power.brmodelo.domain.applyDemoteAssociativeToEntity
import games.polyclub.power.brmodelo.domain.applyDemoteAssociativeToRelationship
import games.polyclub.power.brmodelo.domain.applyOrganizeAttributesMenuAction
import games.polyclub.power.brmodelo.domain.applyPromoteAttributeToEntity
import games.polyclub.power.brmodelo.domain.applyPromoteRelationshipsToAssociativeEntities
import games.polyclub.power.brmodelo.domain.canMergeEntityAndRelationshipToAssociativeMenu
import games.polyclub.power.brmodelo.domain.canDemoteAssociativeToEntityMenu
import games.polyclub.power.brmodelo.domain.canDemoteAssociativeToRelationshipMenu
import games.polyclub.power.brmodelo.domain.canOrganizeAttributesMenuSelection
import games.polyclub.power.brmodelo.domain.canPromoteAttributeToEntityMenu
import games.polyclub.power.brmodelo.domain.canPromoteToAssociativeEntityMenu
import games.polyclub.power.brmodelo.domain.canSelectAttributeTreeMenu
import games.polyclub.power.brmodelo.domain.expandCanvasSelectionWithAttributeTrees
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ConceptualSearchOutcome
import games.polyclub.power.brmodelo.domain.searchConceptualModel
import games.polyclub.power.brmodelo.domain.singleSelectedElementId
import games.polyclub.power.brmodelo.domain.organizeAttributesOnOwnerSide
import games.polyclub.power.brmodelo.domain.relayoutCompositeSubtree
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.LabelStyle
import games.polyclub.power.brmodelo.domain.SchemaElement
import games.polyclub.power.brmodelo.domain.ConceptualSpecializationToolResult
import games.polyclub.power.brmodelo.domain.ConceptualSpecializationToolVariant
import games.polyclub.power.brmodelo.domain.applyConceptualLinkObjectsMcpPatches
import games.polyclub.power.brmodelo.domain.applyConceptualSpecializationTool
import games.polyclub.power.brmodelo.domain.ConceptualLinkObjectsMcpApplyResult
import games.polyclub.power.brmodelo.domain.ConceptualLinkValidationResult
import games.polyclub.power.brmodelo.domain.NormalizeLinkPicksForMcpLinkExistingEndpointsOnlyResult
import games.polyclub.power.brmodelo.domain.normalizeLinkPicksForMcpLinkExistingEndpointsOnly
import games.polyclub.power.brmodelo.domain.validateAndBuildConceptualLink
import games.polyclub.power.brmodelo.domain.applyEditCanvasElement
import games.polyclub.power.brmodelo.domain.applyEditConceptualModel
import games.polyclub.power.brmodelo.domain.applyEditConnection
import games.polyclub.power.brmodelo.domain.applyEditHiddenAttributeAtPath
import games.polyclub.power.brmodelo.domain.applyMoveCanvasElementsByTranslation
import games.polyclub.power.brmodelo.domain.ConceptualMoveCanvasElementsApplyResult
import games.polyclub.power.brmodelo.domain.ConceptualPropertyEditResult
import games.polyclub.power.brmodelo.domain.serialization.ConceptualSchemaBrmParser
import games.polyclub.power.brmodelo.domain.serialization.ConceptualSchemaXmlParser
import games.polyclub.power.brmodelo.domain.serialization.ConceptualSchemaXmlSerializer
import games.polyclub.power.brmodelo.ui.BulkDataDictionaryDialog
import games.polyclub.power.brmodelo.ui.canvas.withAutoSizedAttributeSubtree
import games.polyclub.power.brmodelo.ui.BrModeloScreen
import games.polyclub.power.brmodelo.ui.AttributeToolRibbonBinding
import games.polyclub.power.brmodelo.ui.AutoSelfRelationshipToolRibbonBinding
import games.polyclub.power.brmodelo.ui.ClipboardRibbonBinding
import games.polyclub.power.brmodelo.ui.clipboard.BrModeloConceptualClipboardStore
import games.polyclub.power.brmodelo.ui.ConceptualSearchDialog
import games.polyclub.power.brmodelo.ui.ConceptualSchemaDictionaryDialog
import games.polyclub.power.brmodelo.ui.ConceptualSearchNavigateAction
import games.polyclub.power.brmodelo.ui.conceptualSearchNavigateAction
import games.polyclub.power.brmodelo.ui.encodeConceptualElementSubsetRasterBlocking
import games.polyclub.power.brmodelo.ui.encodeConceptualSchemaAsMenuExportJpegBytesBlocking
import games.polyclub.power.brmodelo.ui.encodeConceptualSchemaAsMenuExportPngBytesBlocking
import games.polyclub.power.brmodelo.ui.ConceptualSubsetRasterFormat
import games.polyclub.power.brmodelo.ui.clipboard.brModeloClipboardSetPlainText
import games.polyclub.power.brmodelo.ui.clipboard.encodeImageBitmapToPngBytes
import games.polyclub.power.brmodelo.ui.canvas.SchemaCanvasViewState
import games.polyclub.power.brmodelo.ui.canvas.afterCardinalitySyncForElementBoundsChange
import games.polyclub.power.brmodelo.ui.canvas.enrichConnectionWithInitialCardinalityPosition
import games.polyclub.power.brmodelo.ui.canvas.withFloatingCardinalityLayoutForgotten
import games.polyclub.power.brmodelo.ui.canvas.withRecalculatedFloatingCardinalityPositions
import games.polyclub.power.brmodelo.ui.canvas.withConnectionCardinalityInspectorParity
import games.polyclub.power.brmodelo.ui.canvas.renderSchemaToImageBitmap
import games.polyclub.power.brmodelo.ui.canvas.selectionBandGeometricPick
import games.polyclub.power.brmodelo.ui.canvas.syncFloatingCardinalityLayoutAfterMutationFromBaseline
import games.polyclub.power.brmodelo.ui.canvas.withCardinalityPositionsAfterElementsMovedByDelta
import games.polyclub.power.brmodelo.ui.BulkDeleteObjectsToolRibbonBinding
import games.polyclub.power.brmodelo.ui.RectangleSelectionToolRibbonBinding
import games.polyclub.power.brmodelo.ui.BulkDeleteUiState
import games.polyclub.power.brmodelo.ui.SelectionBandUiState
import games.polyclub.power.brmodelo.ui.CloseTabUnsavedDialog
import games.polyclub.power.brmodelo.ui.ConceptualCanvasTool
import games.polyclub.power.brmodelo.ui.EditorTabSession
import games.polyclub.power.brmodelo.ui.EntityToolRibbonBinding
import games.polyclub.power.brmodelo.ui.InspectorTab
import games.polyclub.power.brmodelo.ui.InspectorSelectionFieldFocusRequest
import games.polyclub.power.brmodelo.ui.LinkObjectsToolRibbonBinding
import games.polyclub.power.brmodelo.ui.ObservationToolRibbonBinding
import games.polyclub.power.brmodelo.ui.OperationsMenuRibbonBinding
import games.polyclub.power.brmodelo.ui.SpecializationToolRibbonBinding
import games.polyclub.power.brmodelo.ui.EntityToolVariant
import games.polyclub.power.brmodelo.ui.MainMenuType
import games.polyclub.power.brmodelo.ui.PickedFile
import games.polyclub.power.brmodelo.ui.QuitApplicationUnsavedDialog
import games.polyclub.power.brmodelo.ui.RibbonMcpUi
import games.polyclub.power.brmodelo.ui.ConceptualLabelFontChooserDialog
import games.polyclub.power.brmodelo.ui.ConceptualLabelFontChooserRequest
import games.polyclub.power.brmodelo.ui.SelectFontRibbonBinding
import games.polyclub.power.brmodelo.ui.RibbonTab
import games.polyclub.power.brmodelo.ui.consumeWindowDropFile
import games.polyclub.power.brmodelo.ui.isDesktopTarget
import games.polyclub.power.brmodelo.ui.isWindowDragActive
import games.polyclub.power.brmodelo.ui.setupWindowDragDrop
import games.polyclub.power.brmodelo.ui.showNativeFilePicker
import games.polyclub.power.brmodelo.ui.components.ribbon.attributeVariantRibbonPresentation
import games.polyclub.power.brmodelo.ui.components.ribbon.entityVariantRibbonPresentation
import games.polyclub.power.brmodelo.ui.components.ribbon.specializationVariantRibbonPresentation
import games.polyclub.power.brmodelo.ui.matchesAttributeVariant
import games.polyclub.power.brmodelo.ui.matchesEntityVariant
import games.polyclub.power.brmodelo.ui.matchesSpecializationVariant
import games.polyclub.power.brmodelo.ui.toConceptualTool
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun App(onApplicationTitleChange: (String) -> Unit = {}) {
    var isMainMenuOpen by remember { mutableStateOf(false) }
    var activeMenu by remember { mutableStateOf<MainMenuType?>(null) }
    var selectedRibbonTab by remember { mutableStateOf(RibbonTab.EsquemaConceitual) }
    var clipboardUiTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedRibbonTab) {
        if (selectedRibbonTab == RibbonTab.Opcoes) {
            clipboardUiTick++
        }
    }

    var conceptualCanvasTool by remember { mutableStateOf<ConceptualCanvasTool>(ConceptualCanvasTool.None) }
    var bulkDeleteUi by remember { mutableStateOf<BulkDeleteUiState?>(null) }
    var selectionBandUi by remember { mutableStateOf<SelectionBandUiState?>(null) }

    var canvasCenterOnBoundsRequest by remember { mutableStateOf<ElementPosition?>(null) }
    var inspectorTabRequest by remember { mutableStateOf<InspectorTab?>(null) }
    var inspectorSelectionFieldFocusRequest by remember { mutableStateOf<InspectorSelectionFieldFocusRequest?>(null) }
    var inspectorFieldFocusRevision by remember { mutableLongStateOf(0L) }
    var conceptualSearchDialogOpen by remember { mutableStateOf(false) }
    var schemaDataDictionarySchema by remember { mutableStateOf<ConceptualSchema?>(null) }
    var conceptualLabelFontChooserNonce by remember { mutableLongStateOf(0L) }
    var conceptualLabelFontRequest by remember { mutableStateOf<ConceptualLabelFontChooserRequest?>(null) }

    LaunchedEffect(conceptualCanvasTool) {
        if (conceptualCanvasTool !is ConceptualCanvasTool.BulkDeleteObjects) {
            bulkDeleteUi = null
        }
        if (conceptualCanvasTool != ConceptualCanvasTool.None &&
            conceptualCanvasTool != ConceptualCanvasTool.RectangleSelection
        ) {
            selectionBandUi = null
        }
    }
    var entityToolVariant by remember { mutableStateOf(EntityToolVariant.Plain) }
    var specializationToolVariant by remember { mutableStateOf(ConceptualSpecializationToolVariant.Basic) }
    var attributeToolVariant by remember { mutableStateOf(ConceptualAttributeToolVariant.Basic) }

    val initialTabId = 1L
    var nextTabId by remember { mutableLongStateOf(initialTabId + 1) }
    var tabSessions by remember {
        mutableStateOf(listOf(EditorTabSession.blank(initialTabId)))
    }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTabIndex) {
        val sessionId = tabSessions.getOrNull(selectedTabIndex)?.id ?: return@LaunchedEffect
        val awaiting = conceptualCanvasTool as? ConceptualCanvasTool.LinkObjects.AwaitingSecond ?: return@LaunchedEffect
        if (awaiting.startedOnEditorTabId != -1L && awaiting.startedOnEditorTabId != sessionId) {
            conceptualCanvasTool = ConceptualCanvasTool.LinkObjects.AwaitingFirst
        }
    }

    var pendingCloseTabIndex by remember { mutableStateOf<Int?>(null) }
    var pendingApplicationQuit by remember { mutableStateOf(false) }
    var bulkDataDictionaryRows by remember { mutableStateOf<List<ConceptualDictionarySlotRow>?>(null) }

    fun selectedSession(): EditorTabSession = tabSessions[selectedTabIndex]

    fun replaceTabAt(index: Int, session: EditorTabSession) {
        tabSessions = tabSessions.toMutableList().also { it[index] = session }
    }

    fun mutateSelectedTab(mutator: (EditorTabSession) -> EditorTabSession) {
        replaceTabAt(selectedTabIndex, mutator(selectedSession()))
    }

    /** Each tab owns its [EditorTabSession.history]; mutations always target the selected tab. */
    fun pushCommitOnSelected(normalized: ConceptualSchema) {
        val idx = selectedTabIndex
        val tab = tabSessions[idx]
        tab.history.push(normalized)
        replaceTabAt(
            idx,
            tab.copy(
                schema = normalized,
                inspectorCommittedSchema = tab.history.current,
            ),
        )
    }

    /** One undo checkpoint for an arbitrary open tab (e.g. MCP XML replace/patch). */
    fun pushCommitOnTabAt(index: Int, normalized: ConceptualSchema) {
        if (index !in tabSessions.indices) return
        val tab = tabSessions[index]
        val n = normalized.withNormalizedAttributeMultiValuedCounts()
        tab.history.push(n)
        val committed = tab.history.current ?: return
        replaceTabAt(
            index,
            tab.copy(
                schema = committed,
                inspectorCommittedSchema = committed,
                selection = CanvasSelection.None,
                hiddenAttributeRevealPath = null,
            ),
        )
    }

    fun performRemoveTab(index: Int) {
        if (tabSessions.size == 1) {
            tabSessions = listOf(EditorTabSession.blank(nextTabId++))
            selectedTabIndex = 0
            return
        }
        val oldSel = selectedTabIndex
        val newList = tabSessions.filterIndexed { i, _ -> i != index }
        tabSessions = newList
        selectedTabIndex = when {
            index < oldSel -> oldSel - 1
            index == oldSel -> minOf(oldSel, newList.lastIndex)
            else -> oldSel
        }
    }

    fun requestCloseTab(index: Int) {
        if (index !in tabSessions.indices) return
        if (tabSessions[index].needsCloseConfirmation()) {
            pendingCloseTabIndex = index
        } else {
            performRemoveTab(index)
        }
    }

    fun forceCloseTab(index: Int) {
        if (index !in tabSessions.indices) return
        performRemoveTab(index)
    }

    fun openLoadedModel(model: ConceptualSchema) {
        val incomingPath = model.filePath.trim()
        if (incomingPath.isNotEmpty()) {
            val dupIdx = tabSessions.indexOfFirst { tab ->
                val existing = tab.schema.filePath.trim()
                existing.isNotEmpty() && diskPathsReferToSameFile(existing, incomingPath)
            }
            if (dupIdx >= 0) {
                selectedTabIndex = dupIdx
                return
            }
        }

        if (tabSessions.size == 1 && tabSessions[0].isReplaceableBlankStarter()) {
            val soleId = tabSessions[0].id
            tabSessions = listOf(EditorTabSession.fromLoadedModel(soleId, model))
            selectedTabIndex = 0
            return
        }

        val session = EditorTabSession.fromLoadedModel(nextTabId++, model)
        tabSessions = tabSessions + session
        selectedTabIndex = tabSessions.lastIndex
    }

    fun openNewUnsavedTab(model: ConceptualSchema) {
        if (tabSessions.size == 1 && tabSessions[0].isReplaceableBlankStarter()) {
            val soleId = tabSessions[0].id
            tabSessions = listOf(EditorTabSession.fromUnsavedModel(soleId, model))
            selectedTabIndex = 0
            return
        }

        val session = EditorTabSession.fromUnsavedModel(nextTabId++, model)
        tabSessions = tabSessions + session
        selectedTabIndex = tabSessions.lastIndex
    }

    fun addBlankTab() {
        val session = EditorTabSession.blank(nextTabId++)
        tabSessions = tabSessions + session
        selectedTabIndex = tabSessions.lastIndex
    }

    /**
     * Appends an empty conceptual tab without changing [selectedTabIndex] (MCP automation so the user's
     * focused diagram stays visible while agents work on the new tab).
     */
    fun addBlankTabWithoutSelecting(): Long {
        val session = EditorTabSession.blank(nextTabId++)
        tabSessions = tabSessions + session
        return session.id
    }

    var isDragOverFromPolling by remember { mutableStateOf(false) }
    var isDragOverFromCallback by remember { mutableStateOf(false) }
    val isDragOver = isDragOverFromPolling || isDragOverFromCallback
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val mcpRuntime = remember { McpRuntime() }
    var showMcpSettings by remember { mutableStateOf(false) }
    var mcpServerRunning by remember { mutableStateOf(false) }

    DisposableEffect(mcpRuntime) {
        mcpRuntime.setSettingsDialogOpener { showMcpSettings = true }
        onDispose {
            mcpRuntime.setSettingsDialogOpener { }
            mcpRuntime.shutdown()
        }
    }

    val clipboardPreviewTextMeasurer = rememberTextMeasurer()
    val clipboardPreviewDensity = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    suspend fun saveTabAt(index: Int, saveAs: Boolean): Boolean {
        val tab = tabSessions.getOrNull(index) ?: return true
        val s = tab.schema
        val pickLocation = saveAs || s.filePath.isBlank() || s.openedFromBrm
        val updated = saveConceptualSchemaXml(
            schema = s,
            suggestedBaseName = s.name.ifBlank { "modelo" },
            pickLocation = pickLocation,
        ) ?: return false
        tab.history.syncCurrent(updated)
        replaceTabAt(
            index,
            tab.copy(
                schema = updated,
                inspectorCommittedSchema = updated,
                savedDiskBaseline = updated,
            ),
        )
        return true
    }

    LaunchedEffect(Unit) {
        setupWindowDragDrop()
    }

    LaunchedEffect(tabSessions) {
        val warn = tabSessions.any { it.needsCloseConfirmation() }
        setBrowserUnloadWarningEnabled(warn)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            isDragOverFromPolling = isWindowDragActive()
            val dropped = consumeWindowDropFile()
            if (dropped != null) {
                runCatching { parseModelBytesWithSource(dropped.bytes) }
                    .onSuccess { (parsed, fromBrm) ->
                        openLoadedModel(mergeLoadedModel(parsed, fromBrm, dropped))
                    }
            }
        }
    }

    val openFile: () -> Unit = {
        scope.launch {
            val picked = showNativeFilePicker() ?: return@launch
            runCatching { parseModelBytesWithSource(picked.bytes) }
                .onSuccess { (parsed, fromBrm) ->
                    openLoadedModel(mergeLoadedModel(parsed, fromBrm, picked))
                }
        }
    }

    val loadPickedFile: (PickedFile) -> Unit = { picked ->
        runCatching { parseModelBytesWithSource(picked.bytes) }
            .onSuccess { (parsed, fromBrm) ->
                openLoadedModel(mergeLoadedModel(parsed, fromBrm, picked))
            }
    }

    val onSchemaPreview: (ConceptualSchema) -> Unit = { preview ->
        mutateSelectedTab { it.copy(schema = preview) }
    }

    val onSchemaCommit: (ConceptualSchema) -> Unit = {
        pushCommitOnSelected(it.withNormalizedAttributeMultiValuedCounts())
    }

    val onRevertSchemaPreview: () -> Unit = {
        mutateSelectedTab { tab ->
            val committed = tab.history.current ?: tab.schema
            tab.copy(schema = committed)
        }
    }

    fun enqueueSave(saveAs: Boolean) {
        scope.launch { saveTabAt(selectedTabIndex, saveAs) }
    }

    DisposableEffect(selectedTabIndex, tabSessions) {
        bindDesktopSaveShortcut { enqueueSave(saveAs = false) }
        onDispose { bindDesktopSaveShortcut(null) }
    }

    val tabsState = rememberUpdatedState(tabSessions)
    val requestApplicationQuit: () -> Unit = {
        scope.launch {
            if (tabsState.value.any { it.needsCloseConfirmation() }) {
                pendingApplicationQuit = true
            } else {
                quitApplicationCompletely()
            }
        }
    }
    DisposableEffect(scope) {
        registerDesktopMainWindowCloseHandler { requestApplicationQuit() }
        onDispose { registerDesktopMainWindowCloseHandler(null) }
    }

    val currentName = selectedSession().schema.name
    LaunchedEffect(currentName, selectedTabIndex) {
        onApplicationTitleChange(formatApplicationWindowTitle(currentName))
    }

    val sel = selectedSession()

    var schemaCanvasViewState by remember { mutableStateOf(SchemaCanvasViewState()) }
    val selectedTabIdxState = rememberUpdatedState(selectedTabIndex)
    val schemaCanvasViewStateRef = rememberUpdatedState(schemaCanvasViewState)

    val onCopyConceptualClipboard: () -> Unit = copy@{
        val tab = tabSessions.getOrNull(selectedTabIndex) ?: return@copy
        val payload = buildConceptualClipboardPayload(tab.schema, tab.selection, tab.id) ?: return@copy
        BrModeloConceptualClipboardStore.mirrorLocalClipboardText(payload)
        clipboardUiTick++
        scope.launch {
            val previewSchema = extractClipboardFragment(
                tab.schema,
                elementIdsForClipboard(tab.schema, tab.selection),
            ) ?: tab.schema
            val preview = renderSchemaToImageBitmap(
                schema = previewSchema,
                textMeasurer = clipboardPreviewTextMeasurer,
                density = clipboardPreviewDensity,
                withBackground = false,
            )
            val pngBytes = encodeImageBitmapToPngBytes(preview)
            BrModeloConceptualClipboardStore.writePreferred(payload, pngBytes)
        }
    }

    val onCutConceptualClipboard: () -> Unit = cut@{
        val idx = selectedTabIndex
        val tab = tabSessions.getOrNull(idx) ?: return@cut
        val payload = buildConceptualClipboardPayload(tab.schema, tab.selection, tab.id) ?: return@cut
        BrModeloConceptualClipboardStore.mirrorLocalClipboardText(payload)
        clipboardUiTick++
        scope.launch {
            val previewSchema = extractClipboardFragment(
                tab.schema,
                elementIdsForClipboard(tab.schema, tab.selection),
            ) ?: tab.schema
            val preview = renderSchemaToImageBitmap(
                schema = previewSchema,
                textMeasurer = clipboardPreviewTextMeasurer,
                density = clipboardPreviewDensity,
                withBackground = false,
            )
            val pngBytes = encodeImageBitmapToPngBytes(preview)
            BrModeloConceptualClipboardStore.writePreferred(payload, pngBytes)
            val current = tabsState.value.getOrNull(idx) ?: return@launch
            if (current.id != tab.id) return@launch
            val next = deleteCanvasSelection(current.schema, current.selection) ?: return@launch
            current.history.push(next)
            replaceTabAt(
                idx,
                current.copy(
                    schema = next,
                    inspectorCommittedSchema = current.history.current,
                    selection = CanvasSelection.None,
                    hiddenAttributeRevealPath = null,
                ),
            )
        }
    }

    val onPasteConceptualClipboard: () -> Unit = paste@{
        if (isDesktopTarget && !BrModeloConceptualClipboardStore.hasPasteableConceptualPayloadForRibbonUi()) {
            return@paste
        }
        // Snapshot anchor **before** suspending on clipboard I/O (async read would advance frames and stale pointers).
        val viewSnapshot = schemaCanvasViewStateRef.value
        val idx = selectedTabIdxState.value
        val tabSnapshot = tabsState.value.getOrNull(idx) ?: return@paste
        scope.launch {
            val text = BrModeloConceptualClipboardStore.readPreferred() ?: return@launch
            val t = tabsState.value.getOrNull(idx) ?: return@launch
            if (t.id != tabSnapshot.id) return@launch
            val ctx = ConceptualPasteContext(
                targetSchema = t.schema,
                targetEditorTabId = t.id,
                layoutWidthPx = viewSnapshot.layoutWidthPx,
                layoutHeightPx = viewSnapshot.layoutHeightPx,
                panX = viewSnapshot.panX,
                panY = viewSnapshot.panY,
                zoom = viewSnapshot.zoom,
                pointerModelX = viewSnapshot.pointerModelX(),
                pointerModelY = viewSnapshot.pointerModelY(),
                isPointerOverCanvas = viewSnapshot.isPointerOverCanvas,
            )
            val beforePaste = t.schema
            val result = pasteConceptualClipboard(ctx, text) ?: return@launch
            val pasted = result.schema.syncFloatingCardinalityLayoutAfterMutationFromBaseline(
                baseline = beforePaste,
                textMeasurer = clipboardPreviewTextMeasurer,
                rehomeConnectionsAbsentInBaseline = true,
            )
            t.history.push(pasted)
            replaceTabAt(
                idx,
                t.copy(
                    schema = pasted,
                    inspectorCommittedSchema = t.history.current,
                    selection = result.selection,
                    hiddenAttributeRevealPath = null,
                ),
            )
        }
    }

    val conceptualCopyCutEnabled = buildConceptualClipboardPayload(sel.schema, sel.selection, sel.id) != null
    val conceptualPasteEnabled = run {
        clipboardUiTick
        if (!isDesktopTarget) {
            true
        } else {
            BrModeloConceptualClipboardStore.hasPasteableConceptualPayloadForRibbonUi()
        }
    }

    val clipboardRibbonBinding = ClipboardRibbonBinding(
        copyEnabled = conceptualCopyCutEnabled,
        cutEnabled = conceptualCopyCutEnabled,
        pasteEnabled = conceptualPasteEnabled,
        onCopy = onCopyConceptualClipboard,
        onCut = onCutConceptualClipboard,
        onPaste = onPasteConceptualClipboard,
    )

    val selectFontRibbonBinding = SelectFontRibbonBinding(
        enabled = sel.selection.singleSelectedElementId() != null,
        onSelectFont = {
            val idx = selectedTabIdxState.value
            val tab = tabsState.value.getOrNull(idx) ?: return@SelectFontRibbonBinding
            val eid = tab.selection.singleSelectedElementId() ?: run {
                scope.launch { snackbarHostState.showSnackbar("Selecione um objeto no diagrama.") }
                return@SelectFontRibbonBinding
            }
            val el = tab.schema.elements[eid] ?: return@SelectFontRibbonBinding
            conceptualCanvasTool = ConceptualCanvasTool.None
            conceptualLabelFontChooserNonce += 1L
            conceptualLabelFontRequest = ConceptualLabelFontChooserRequest(
                editorTabId = tab.id,
                elementId = eid,
                initial = el.labelStyle,
                openNonce = conceptualLabelFontChooserNonce,
            )
        },
    )

    val (entityTitle, entityIcon) = entityVariantRibbonPresentation(entityToolVariant)
    val entityToolBinding = EntityToolRibbonBinding(
        variant = entityToolVariant,
        isArmed = conceptualCanvasTool.matchesEntityVariant(entityToolVariant),
        displayTitle = entityTitle,
        displayIcon = entityIcon,
        onMainClick = {
            if (conceptualCanvasTool.matchesEntityVariant(entityToolVariant)) {
                conceptualCanvasTool = ConceptualCanvasTool.None
            } else {
                conceptualCanvasTool = entityToolVariant.toConceptualTool()
            }
        },
        onDropdownVariant = { v ->
            entityToolVariant = v
            conceptualCanvasTool = v.toConceptualTool()
        },
    )

    val observationToolBinding = ObservationToolRibbonBinding(
        isArmed = conceptualCanvasTool is ConceptualCanvasTool.Observation,
        onClick = {
            conceptualCanvasTool =
                if (conceptualCanvasTool is ConceptualCanvasTool.Observation) {
                    ConceptualCanvasTool.None
                } else {
                    ConceptualCanvasTool.Observation
                }
        },
    )

    val linkObjectsToolBinding = LinkObjectsToolRibbonBinding(
        isArmed = conceptualCanvasTool is ConceptualCanvasTool.LinkObjects,
        onClick = {
            conceptualCanvasTool =
                if (conceptualCanvasTool is ConceptualCanvasTool.LinkObjects) {
                    ConceptualCanvasTool.None
                } else {
                    ConceptualCanvasTool.LinkObjects.AwaitingFirst
                }
        },
    )

    val autoSelfRelationshipToolBinding = AutoSelfRelationshipToolRibbonBinding(
        isArmed = conceptualCanvasTool is ConceptualCanvasTool.AutoSelfRelationship,
        onClick = {
            conceptualCanvasTool =
                if (conceptualCanvasTool is ConceptualCanvasTool.AutoSelfRelationship) {
                    ConceptualCanvasTool.None
                } else {
                    ConceptualCanvasTool.AutoSelfRelationship
                }
        },
    )

    val (specializationTitle, specializationIcon) = specializationVariantRibbonPresentation(specializationToolVariant)
    val specializationToolBinding = SpecializationToolRibbonBinding(
        variant = specializationToolVariant,
        isArmed = conceptualCanvasTool.matchesSpecializationVariant(specializationToolVariant),
        displayTitle = specializationTitle,
        displayIcon = specializationIcon,
        onMainClick = {
            if (conceptualCanvasTool.matchesSpecializationVariant(specializationToolVariant)) {
                conceptualCanvasTool = ConceptualCanvasTool.None
            } else {
                conceptualCanvasTool = specializationToolVariant.toConceptualTool()
            }
        },
        onDropdownVariant = { v ->
            specializationToolVariant = v
            conceptualCanvasTool = v.toConceptualTool()
        },
    )

    val (attributeTitle, attributeIcon) = attributeVariantRibbonPresentation(attributeToolVariant)
    val attributeToolBinding = AttributeToolRibbonBinding(
        variant = attributeToolVariant,
        isArmed = conceptualCanvasTool.matchesAttributeVariant(attributeToolVariant),
        displayTitle = attributeTitle,
        displayIcon = attributeIcon,
        onMainClick = {
            if (conceptualCanvasTool.matchesAttributeVariant(attributeToolVariant)) {
                conceptualCanvasTool = ConceptualCanvasTool.None
            } else {
                conceptualCanvasTool = attributeToolVariant.toConceptualTool()
            }
        },
        onDropdownVariant = { v ->
            attributeToolVariant = v
            conceptualCanvasTool = v.toConceptualTool()
        },
    )

    val bulkDeleteObjectsToolBinding = BulkDeleteObjectsToolRibbonBinding(
        isArmed = conceptualCanvasTool is ConceptualCanvasTool.BulkDeleteObjects,
        onClick = {
            conceptualCanvasTool =
                if (conceptualCanvasTool is ConceptualCanvasTool.BulkDeleteObjects) {
                    ConceptualCanvasTool.None
                } else {
                    ConceptualCanvasTool.BulkDeleteObjects
                }
        },
    )

    val rectangleSelectionToolBinding = RectangleSelectionToolRibbonBinding(
        isArmed = conceptualCanvasTool is ConceptualCanvasTool.RectangleSelection,
        onClick = {
            conceptualCanvasTool =
                if (conceptualCanvasTool is ConceptualCanvasTool.RectangleSelection) {
                    ConceptualCanvasTool.None
                } else {
                    ConceptualCanvasTool.RectangleSelection
                }
        },
    )

    fun applyConceptualSearchNavigateOnTab(tabIndex: Int, action: ConceptualSearchNavigateAction) {
        val tab = tabSessions.getOrNull(tabIndex) ?: return
        replaceTabAt(
            tabIndex,
            tab.copy(
                selection = action.selection,
                hiddenAttributeRevealPath = action.hiddenAttributeRevealPath,
            ),
        )
        canvasCenterOnBoundsRequest = action.centerOnBounds
        inspectorTabRequest = action.inspectorTab
    }

    fun applyConceptualSearchNavigateAction(action: ConceptualSearchNavigateAction) {
        applyConceptualSearchNavigateOnTab(selectedTabIndex, action)
    }

    val organizeAttrsEnabled = canOrganizeAttributesMenuSelection(sel.schema, sel.selection)
    val onOrganizeAttributes: () -> Unit = organize@{
        val tab = tabSessions.getOrNull(selectedTabIndex) ?: return@organize
        val before = tab.schema
        val updated = applyOrganizeAttributesMenuAction(before, tab.selection) ?: return@organize
        val synced = updated.syncFloatingCardinalityLayoutAfterMutationFromBaseline(
            baseline = before,
            textMeasurer = clipboardPreviewTextMeasurer,
            rehomeConnectionsAbsentInBaseline = false,
        )
        pushCommitOnSelected(synced.withNormalizedAttributeMultiValuedCounts())
    }
    val selectAttrsEnabled = canSelectAttributeTreeMenu(sel.schema, sel.selection)
    val onSelectAttributes: () -> Unit = {
        mutateSelectedTab { tab ->
            tab.copy(selection = expandCanvasSelectionWithAttributeTrees(tab.schema, tab.selection))
        }
    }
    val promoteAssociativeEnabled = canPromoteToAssociativeEntityMenu(sel.schema, sel.selection)
    val onPromoteToAssociative: () -> Unit = promote@{
        val tab = tabSessions.getOrNull(selectedTabIndex) ?: return@promote
        val updated = applyPromoteRelationshipsToAssociativeEntities(tab.schema, tab.selection) ?: return@promote
        pushCommitOnSelected(updated.withNormalizedAttributeMultiValuedCounts())
    }
    val promoteAttributeToEntityEnabled = canPromoteAttributeToEntityMenu(sel.schema, sel.selection)
    val onPromoteAttributeToEntity: () -> Unit = promoteEnt@{
        val tab = tabSessions.getOrNull(selectedTabIndex) ?: return@promoteEnt
        val updated = applyPromoteAttributeToEntity(tab.schema, tab.selection) ?: return@promoteEnt
        pushCommitOnSelected(updated.withNormalizedAttributeMultiValuedCounts())
    }
    val demoteAssocRelEnabled = canDemoteAssociativeToRelationshipMenu(sel.schema, sel.selection)
    val onDemoteAssociativeToRelationship: () -> Unit = demRel@{
        val tab = tabSessions.getOrNull(selectedTabIndex) ?: return@demRel
        val updated = applyDemoteAssociativeToRelationship(tab.schema, tab.selection) ?: return@demRel
        pushCommitOnSelected(updated.withNormalizedAttributeMultiValuedCounts())
    }
    val demoteAssocEntEnabled = canDemoteAssociativeToEntityMenu(sel.schema, sel.selection)
    val onDemoteAssociativeToEntity: () -> Unit = demEnt@{
        val tab = tabSessions.getOrNull(selectedTabIndex) ?: return@demEnt
        val updated = applyDemoteAssociativeToEntity(tab.schema, tab.selection) ?: return@demEnt
        pushCommitOnSelected(updated.withNormalizedAttributeMultiValuedCounts())
    }
    val mergeToAssocEnabled = canMergeEntityAndRelationshipToAssociativeMenu(sel.schema, sel.selection)
    val onMergeEntityAndRelationshipToAssociative: () -> Unit = merge@{
        val tab = tabSessions.getOrNull(selectedTabIndex) ?: return@merge
        val entityId = entityAndRelationshipIdsForMerge(tab.schema, tab.selection)?.first ?: return@merge
        val updated = applyMergeEntityAndRelationshipToAssociative(tab.schema, tab.selection) ?: return@merge
        pushCommitOnSelected(updated.withNormalizedAttributeMultiValuedCounts())
        mutateSelectedTab { it.copy(selection = CanvasSelection.Element(entityId)) }
    }
    val convertToRestrictedEnabled = canConvertOptionalSpecializationsToRestrictedMenu(sel.schema, sel.selection)
    val onConvertOptionalSpecializationsToRestricted: () -> Unit = convR@{
        val tab = tabSessions.getOrNull(selectedTabIndex) ?: return@convR
        val updated = applyConvertOptionalSpecializationsToRestricted(tab.schema, tab.selection) ?: return@convR
        pushCommitOnSelected(updated.withNormalizedAttributeMultiValuedCounts())
    }
    val convertToOptionalsEnabled = canConvertRestrictedSpecializationToOptionalsMenu(sel.schema, sel.selection)
    val onConvertRestrictedSpecializationToOptionals: () -> Unit = convO@{
        val tab = tabSessions.getOrNull(selectedTabIndex) ?: return@convO
        val updated = applyConvertRestrictedSpecializationToOptionals(tab.schema, tab.selection) ?: return@convO
        pushCommitOnSelected(updated.withNormalizedAttributeMultiValuedCounts())
    }
    val hideAttrEnabled = canHideCanvasAttributeMenu(sel.schema, sel.selection)
    val onHideCanvasAttribute: () -> Unit = hide@{
        val tab = tabSessions.getOrNull(selectedTabIndex) ?: return@hide
        val attrId = (tab.selection as? CanvasSelection.Element)?.id ?: return@hide
        val attr = tab.schema.elements[attrId] as? SchemaElement.Attribute ?: return@hide
        val storageId = hiddenAttributeStorageOwnerId(tab.schema, attr)
        val updated = applyHideCanvasAttribute(tab.schema, tab.selection) ?: return@hide
        val selectOwnerId = ultimateNonAttributeOwner(updated, storageId)
        pushCommitOnSelected(updated.withNormalizedAttributeMultiValuedCounts())
        mutateSelectedTab {
            it.copy(
                selection = CanvasSelection.Element(selectOwnerId),
                hiddenAttributeRevealPath = null,
            )
        }
    }
    val revealHiddenEnabled =
        canRevealHiddenAttributeMenu(sel.schema, sel.selection, sel.hiddenAttributeRevealPath)
    val onRevealHiddenAttribute: () -> Unit = rev@{
        val tab = tabSessions.getOrNull(selectedTabIndex) ?: return@rev
        val path = tab.hiddenAttributeRevealPath ?: return@rev
        val ownerId = (tab.selection as? CanvasSelection.Element)?.id ?: return@rev
        val (updated, newAttrId) = applyRevealHiddenAttribute(tab.schema, ownerId, path) ?: return@rev
        pushCommitOnSelected(updated.withNormalizedAttributeMultiValuedCounts())
        mutateSelectedTab {
            it.copy(
                selection = CanvasSelection.Element(newAttrId),
                hiddenAttributeRevealPath = null,
            )
        }
    }
    val dataDictionaryBulkEnabled = canOpenBulkDataDictionaryForSelection(sel.selection)
    val onOpenDataDictionaryBulk: () -> Unit = dict@{
        val tab = tabSessions.getOrNull(selectedTabIndex) ?: return@dict
        bulkDataDictionaryRows = collectDictionarySlotsForSelection(tab.schema, tab.selection)
    }
    val operationsMenuBinding = OperationsMenuRibbonBinding(
        organizeAttributesEnabled = organizeAttrsEnabled,
        onOrganizeAttributes = onOrganizeAttributes,
        selectAttributesEnabled = selectAttrsEnabled,
        onSelectAttributes = onSelectAttributes,
        promoteToAssociativeEnabled = promoteAssociativeEnabled,
        onPromoteToAssociative = onPromoteToAssociative,
        promoteAttributeToEntityEnabled = promoteAttributeToEntityEnabled,
        onPromoteAttributeToEntity = onPromoteAttributeToEntity,
        demoteAssociativeToRelationshipEnabled = demoteAssocRelEnabled,
        onDemoteAssociativeToRelationship = onDemoteAssociativeToRelationship,
        demoteAssociativeToEntityEnabled = demoteAssocEntEnabled,
        onDemoteAssociativeToEntity = onDemoteAssociativeToEntity,
        mergeEntityAndRelationshipToAssociativeEnabled = mergeToAssocEnabled,
        onMergeEntityAndRelationshipToAssociative = onMergeEntityAndRelationshipToAssociative,
        convertOptionalSpecializationsToRestrictedEnabled = convertToRestrictedEnabled,
        onConvertOptionalSpecializationsToRestricted = onConvertOptionalSpecializationsToRestricted,
        convertRestrictedSpecializationToOptionalsEnabled = convertToOptionalsEnabled,
        onConvertRestrictedSpecializationToOptionals = onConvertRestrictedSpecializationToOptionals,
        hideCanvasAttributeEnabled = hideAttrEnabled,
        onHideCanvasAttribute = onHideCanvasAttribute,
        revealHiddenAttributeEnabled = revealHiddenEnabled,
        onRevealHiddenAttribute = onRevealHiddenAttribute,
        dataDictionaryBulkEnabled = dataDictionaryBulkEnabled,
        onOpenDataDictionaryBulk = onOpenDataDictionaryBulk,
        undoEnabled = sel.history.canUndo,
        onUndo = {
            mutateSelectedTab { tab ->
                if (!tab.history.canUndo) return@mutateSelectedTab tab
                tab.history.undo()
                val cur = tab.history.current ?: tab.schema
                tab.copy(
                    schema = cur,
                    inspectorCommittedSchema = cur,
                    selection = CanvasSelection.None,
                    hiddenAttributeRevealPath = null,
                )
            }
        },
        redoEnabled = sel.history.canRedo,
        onRedo = {
            mutateSelectedTab { tab ->
                if (!tab.history.canRedo) return@mutateSelectedTab tab
                tab.history.redo()
                val cur = tab.history.current ?: tab.schema
                tab.copy(
                    schema = cur,
                    inspectorCommittedSchema = cur,
                    selection = CanvasSelection.None,
                    hiddenAttributeRevealPath = null,
                )
            }
        },
        conceptualFindEnabled = true,
        onOpenConceptualFind = { conceptualSearchDialogOpen = true },
    )

    SideEffect {
        if (isDesktopTarget) {
            val launchMcpUserNotice: (McpAgentUserNotice) -> Unit = { notice ->
                scope.launch {
                    val msg = formatMcpAgentUserNoticePtBr(notice)
                    if (msg.isNotEmpty()) {
                        snackbarHostState.showSnackbar(
                            message = msg,
                            duration = SnackbarDuration.Long,
                        )
                    }
                }
            }
            McpDesktopSync.syncBindingsFromApp(
                runtime = mcpRuntime,
                snackbarHostState = snackbarHostState,
                scope = scope,
                tabSessions = tabSessions,
                selectedTabIndex = selectedTabIndex,
                onSelectTab = { selectedTabIndex = it },
                onAddBlankTab = { addBlankTabWithoutSelecting() },
                onForceCloseTab = { forceCloseTab(it) },
                onRequestCloseTab = { requestCloseTab(it) },
                saveTabAt = ::saveTabAt,
                onOpenModelFileAtPath = mcpOpenPath@{ path ->
                    val picked = tryLoadPickedFileFromAbsolutePath(path)
                        ?: return@mcpOpenPath "unreadable_or_missing_file"
                    val (parsed, fromBrm) = try {
                        parseModelBytesWithSource(picked.bytes)
                    } catch (e: Throwable) {
                        return@mcpOpenPath "parse_failed:${e.message ?: (e::class.simpleName ?: "Error")}"
                    }
                    openLoadedModel(mergeLoadedModel(parsed, fromBrm, picked))
                    null
                },
                onOpenXmlAsUnsavedTab = mcpXml@{ fileName, xml ->
                    validateMcpOpenXmlBasename(fileName)?.let { return@mcpXml it }
                    val bytes = xml.encodeToByteArray()
                    val (parsed, fromBrm) = try {
                        parseModelBytesWithSource(bytes)
                    } catch (e: Throwable) {
                        return@mcpXml "parse_failed:${e.message ?: (e::class.simpleName ?: "Error")}"
                    }
                    if (fromBrm) {
                        return@mcpXml "only_conceptual_xml_supported"
                    }
                    val title = schemaNameFromMcpXmlBasename(fileName)
                    val model = parsed.copy(name = title, filePath = "", openedFromBrm = false)
                    openNewUnsavedTab(model)
                    null
                },
                onReplaceModelXmlAtTab = mcpReplace@{ tabIdx, xml ->
                    if (tabIdx !in tabSessions.indices) {
                        return@mcpReplace "invalid_tab_index"
                    }
                    val bytes = xml.encodeToByteArray()
                    val (parsed, fromBrm) = try {
                        parseModelBytesWithSource(bytes)
                    } catch (e: Throwable) {
                        return@mcpReplace "parse_failed:${e.message ?: (e::class.simpleName ?: "Error")}"
                    }
                    if (fromBrm) {
                        return@mcpReplace "only_conceptual_xml_supported"
                    }
                    val tab = tabSessions[tabIdx]
                    val merged = parsed.copy(
                        filePath = tab.schema.filePath,
                        openedFromBrm = tab.schema.openedFromBrm,
                    )
                    pushCommitOnTabAt(tabIdx, merged)
                    null
                },
                onPatchModelXmlAtTab = mcpPatch@{ tabIdx, oldStr, newStr, replaceAll ->
                    if (tabIdx !in tabSessions.indices) {
                        return@mcpPatch "invalid_tab_index"
                    }
                    val currentXml = try {
                        ConceptualSchemaXmlSerializer.serialize(tabSessions[tabIdx].schema)
                    } catch (e: Throwable) {
                        return@mcpPatch "serialize_failed:${e.message ?: (e::class.simpleName ?: "Error")}"
                    }
                    val (patchedXml, patchErr) = McpModelXmlPatch.applyXmlStringPatch(
                        currentXml,
                        oldStr,
                        newStr,
                        replaceAll,
                    )
                    if (patchErr != null) {
                        return@mcpPatch patchErr
                    }
                    val out = patchedXml ?: return@mcpPatch "patch_failed"
                    val bytes = out.encodeToByteArray()
                    val (parsed, fromBrm) = try {
                        parseModelBytesWithSource(bytes)
                    } catch (e: Throwable) {
                        return@mcpPatch "parse_failed_after_patch:${e.message ?: (e::class.simpleName ?: "Error")}"
                    }
                    if (fromBrm) {
                        return@mcpPatch "only_conceptual_xml_supported"
                    }
                    val tab = tabSessions[tabIdx]
                    val merged = parsed.copy(
                        filePath = tab.schema.filePath,
                        openedFromBrm = tab.schema.openedFromBrm,
                    )
                    pushCommitOnTabAt(tabIdx, merged)
                    null
                },
                onPlaceProceduralConceptualToolAtTab = mcpPlaceTool@{ tabIdx, kind, x, y, overrides ->
                    if (tabIdx !in tabSessions.indices) {
                        return@mcpPlaceTool McpProceduralToolApplyOutcome.err("invalid_tab_index")
                    }
                    val tab = tabSessions[tabIdx]
                    when (val r = tab.schema.placeProceduralConceptualTool(kind, x, y, overrides)) {
                        is ConceptualProceduralToolPlacementResult.Err ->
                            McpProceduralToolApplyOutcome.err(r.code)
                        is ConceptualProceduralToolPlacementResult.Ok -> {
                            val normalized = r.schema.withNormalizedAttributeMultiValuedCounts()
                            val placed = normalized.elements[r.element.id]
                                ?: return@mcpPlaceTool McpProceduralToolApplyOutcome.err("internal_missing_placed_element")
                            pushCommitOnTabAt(tabIdx, normalized)
                            McpProceduralToolApplyOutcome.ok(
                                tabIdx,
                                McpConceptualToolElementResponseJson.elementSummary(placed),
                                McpProceduralToolLayoutQualityScan(tabIdx, setOf(placed.id)),
                            )
                        }
                    }
                },
                onApplyConceptualSpecializationAtTab = mcpSpec@{ tabIdx, baseEntityId, variant ->
                    if (tabIdx !in tabSessions.indices) {
                        return@mcpSpec McpProceduralToolApplyOutcome.err("invalid_tab_index")
                    }
                    val tab = tabSessions[tabIdx]
                    when (val r = applyConceptualSpecializationTool(tab.schema, baseEntityId, variant)) {
                        is ConceptualSpecializationToolResult.Error ->
                            McpProceduralToolApplyOutcome.err(r.message)
                        is ConceptualSpecializationToolResult.Ok -> {
                            val normalized = r.schema.withNormalizedAttributeMultiValuedCounts()
                            val placed = normalized.elements[r.newSpecializationId]
                                ?: return@mcpSpec McpProceduralToolApplyOutcome.err("internal_missing_specialization")
                            pushCommitOnTabAt(tabIdx, normalized)
                            McpProceduralToolApplyOutcome.ok(
                                tabIdx,
                                McpConceptualToolElementResponseJson.elementSummary(placed),
                                McpProceduralToolLayoutQualityScan(tabIdx, setOf(r.newSpecializationId)),
                            )
                        }
                    }
                },
                onApplySimpleConceptualAttributeAtTab = mcpSimpleAttr@{ tabIdx, targetId, variant, attachSide, overrides ->
                    if (tabIdx !in tabSessions.indices) {
                        return@mcpSimpleAttr McpProceduralToolApplyOutcome.err("invalid_tab_index")
                    }
                    val tab = tabSessions[tabIdx]
                    val beforeAttrTool = tab.schema
                    when (val r = applyConceptualSimpleAttributeTool(beforeAttrTool, targetId, variant, attachSide, overrides)) {
                        is ConceptualAttributeToolResult.Error ->
                            McpProceduralToolApplyOutcome.err(r.message)
                        is ConceptualAttributeToolResult.Ok -> {
                            var committed = r.schema.withAutoSizedAttributeSubtree(
                                r.newPrimaryAttributeId,
                                clipboardPreviewTextMeasurer,
                                layoutDirection,
                            )
                            val placed = committed.elements[r.newPrimaryAttributeId] as? SchemaElement.Attribute
                            if (placed?.isComposite == true) {
                                committed = relayoutCompositeSubtree(committed, placed.id)
                            }
                            committed = organizeAttributesOnOwnerSide(
                                committed,
                                r.ownerElementId,
                                r.attachSide,
                            )
                            committed = committed.syncFloatingCardinalityLayoutAfterMutationFromBaseline(
                                baseline = beforeAttrTool,
                                textMeasurer = clipboardPreviewTextMeasurer,
                                rehomeConnectionsAbsentInBaseline = true,
                            )
                            val normalized = committed.withNormalizedAttributeMultiValuedCounts()
                            val out = normalized.elements[r.newPrimaryAttributeId]
                                ?: return@mcpSimpleAttr McpProceduralToolApplyOutcome.err("internal_missing_attribute")
                            pushCommitOnTabAt(tabIdx, normalized)
                            McpProceduralToolApplyOutcome.ok(
                                tabIdx,
                                McpConceptualToolElementResponseJson.elementSummary(out),
                                McpProceduralToolLayoutQualityScan(
                                    tabIdx,
                                    setOf(r.newPrimaryAttributeId, r.ownerElementId),
                                ),
                            )
                        }
                    }
                },
                onApplyCompositeConceptualAttributeAtTab = mcpCompAttr@{ tabIdx, targetId, attachSide, leafSpecs, nestedHidden ->
                    if (tabIdx !in tabSessions.indices) {
                        return@mcpCompAttr McpProceduralToolApplyOutcome.err("invalid_tab_index")
                    }
                    val tab = tabSessions[tabIdx]
                    val beforeCompAttr = tab.schema
                    when (
                        val r = applyConceptualCompositeAttributeWithLeafChildren(
                            beforeCompAttr,
                            targetId,
                            attachSide,
                            leafSpecs,
                            nestedHidden,
                        )
                    ) {
                        is ConceptualAttributeToolResult.Error ->
                            McpProceduralToolApplyOutcome.err(r.message)
                        is ConceptualAttributeToolResult.Ok -> {
                            var committed = r.schema.withAutoSizedAttributeSubtree(
                                r.newPrimaryAttributeId,
                                clipboardPreviewTextMeasurer,
                                layoutDirection,
                            )
                            val placed = committed.elements[r.newPrimaryAttributeId] as? SchemaElement.Attribute
                            if (placed?.isComposite == true) {
                                committed = relayoutCompositeSubtree(committed, placed.id)
                            }
                            committed = organizeAttributesOnOwnerSide(
                                committed,
                                r.ownerElementId,
                                r.attachSide,
                            )
                            committed = committed.syncFloatingCardinalityLayoutAfterMutationFromBaseline(
                                baseline = beforeCompAttr,
                                textMeasurer = clipboardPreviewTextMeasurer,
                                rehomeConnectionsAbsentInBaseline = true,
                            )
                            val normalized = committed.withNormalizedAttributeMultiValuedCounts()
                            val out = normalized.elements[r.newPrimaryAttributeId]
                                ?: return@mcpCompAttr McpProceduralToolApplyOutcome.err("internal_missing_attribute")
                            pushCommitOnTabAt(tabIdx, normalized)
                            McpProceduralToolApplyOutcome.ok(
                                tabIdx,
                                McpConceptualToolElementResponseJson.elementSummary(out),
                                McpProceduralToolLayoutQualityScan(
                                    tabIdx,
                                    setOf(r.newPrimaryAttributeId, r.ownerElementId),
                                ),
                            )
                        }
                    }
                },
                onApplyHiddenAttributeForestAtTab = mcpHidden@{ tabIdx, holderId, roots ->
                    if (tabIdx !in tabSessions.indices) {
                        return@mcpHidden McpProceduralToolApplyOutcome.err("invalid_tab_index")
                    }
                    val tab = tabSessions[tabIdx]
                    when (val r = applyAppendHiddenAttributeForest(tab.schema, holderId, roots)) {
                        is ConceptualAppendHiddenAttributesResult.Error ->
                            McpProceduralToolApplyOutcome.err(r.message)
                        is ConceptualAppendHiddenAttributesResult.Ok -> {
                            pushCommitOnTabAt(tabIdx, r.schema)
                            McpProceduralToolApplyOutcome.okFullJson(
                                """{"ok":true,"resourceUri":${mcpJsonStringLiteral(modelResourceUriForSession(tab.id))},"holderElementId":$holderId,"appendedRootCount":${roots.size}}""",
                                McpProceduralToolLayoutQualityScan(tabIdx, setOf(holderId)),
                            )
                        }
                    }
                },
                onLinkConceptualObjectsAtTab = mcpLink@{ tabIdx, endA, endB, relOverrides, connPatches, autoSelfClick, dryRun, linkExistingEndpointsOnly ->
                    if (tabIdx !in tabSessions.indices) {
                        return@mcpLink McpProceduralToolApplyOutcome.err("invalid_tab_index")
                    }
                    val tab = tabSessions[tabIdx]
                    val before = tab.schema
                    val (resolvedEndA, resolvedEndB) =
                        if (linkExistingEndpointsOnly) {
                            when (
                                val normalized =
                                    normalizeLinkPicksForMcpLinkExistingEndpointsOnly(before, endA, endB)
                            ) {
                                is NormalizeLinkPicksForMcpLinkExistingEndpointsOnlyResult.Err ->
                                    return@mcpLink McpProceduralToolApplyOutcome.err(normalized.code)
                                is NormalizeLinkPicksForMcpLinkExistingEndpointsOnlyResult.Ok ->
                                    normalized.endA to normalized.endB
                            }
                        } else {
                            endA to endB
                        }
                    when (val link = validateAndBuildConceptualLink(before, resolvedEndA, resolvedEndB, autoSelfClick)) {
                        is ConceptualLinkValidationResult.Error ->
                            McpProceduralToolApplyOutcome.err(link.message)
                        is ConceptualLinkValidationResult.Ok -> {
                            when (
                                val patched =
                                    applyConceptualLinkObjectsMcpPatches(
                                        before,
                                        link.schema,
                                        relOverrides,
                                        connPatches,
                                    )
                            ) {
                                is ConceptualLinkObjectsMcpApplyResult.Err ->
                                    McpProceduralToolApplyOutcome.err(patched.code)
                                is ConceptualLinkObjectsMcpApplyResult.Ok -> {
                                    val oldConnIds = before.connections.map { it.id }.toSet()
                                    var committed = patched.schema.withFloatingCardinalityLayoutForgotten()
                                    for (conn in committed.connections.filter { it.id !in oldConnIds }) {
                                        val enriched =
                                            enrichConnectionWithInitialCardinalityPosition(
                                                committed,
                                                conn,
                                                clipboardPreviewTextMeasurer,
                                            )
                                        committed = committed.copy(
                                            connections = committed.connections.map {
                                                if (it.id == conn.id) enriched else it
                                            },
                                        )
                                    }
                                    committed = committed.withRecalculatedFloatingCardinalityPositions(
                                        textMeasurer = clipboardPreviewTextMeasurer,
                                    )
                                    val normalized = committed.withNormalizedAttributeMultiValuedCounts()
                                    val newConns =
                                        normalized.connections.filter { it.id !in oldConnIds }.sortedBy { it.id }
                                    val newElemIds =
                                        normalized.elements.keys.filter { it !in before.elements.keys }
                                    val newRel = newElemIds.asSequence()
                                        .mapNotNull { id -> normalized.elements[id] as? SchemaElement.Relationship }
                                        .firstOrNull()
                                    val newSelf = newElemIds.asSequence()
                                        .mapNotNull { id -> normalized.elements[id] as? SchemaElement.SelfRelationship }
                                        .firstOrNull()
                                    val linkPattern = classifyMcpLinkObjectsPattern(before, normalized)
                                    val linkScope = buildSet {
                                        addAll(newElemIds)
                                        newConns.forEach {
                                            add(it.elementIdA)
                                            add(it.elementIdB)
                                        }
                                    }
                                    val body = McpConnectionToolResponseJson.linkObjectsToolSuccessJson(
                                        modelResourceUriForSession(tab.id),
                                        newConns,
                                        newRel,
                                        newSelf,
                                        linkPattern = linkPattern,
                                        dryRun = dryRun,
                                    )
                                    if (dryRun) {
                                        val previewReport = analyzeConceptualLayoutQuality(normalized, linkScope)
                                        return@mcpLink McpProceduralToolApplyOutcome.okFullJson(
                                            body,
                                            McpProceduralToolLayoutQualityScan(
                                                tabIdx,
                                                linkScope,
                                                reportOverride = previewReport,
                                                schemaForLayoutQualityJson = normalized,
                                            ),
                                        )
                                    }
                                    pushCommitOnTabAt(tabIdx, normalized)
                                    McpProceduralToolApplyOutcome.okFullJson(
                                        body,
                                        McpProceduralToolLayoutQualityScan(tabIdx, linkScope),
                                    )
                                }
                            }
                        }
                    }
                },
                onApplyEditConceptualModelAtTab = mcpEditModel@{ tabIdx, patch ->
                    if (tabIdx !in tabSessions.indices) {
                        return@mcpEditModel McpProceduralToolApplyOutcome.err("invalid_tab_index")
                    }
                    when (val r = applyEditConceptualModel(tabSessions[tabIdx].schema, patch)) {
                        is ConceptualPropertyEditResult.Err ->
                            McpProceduralToolApplyOutcome.err(r.code)
                        is ConceptualPropertyEditResult.Ok -> {
                            pushCommitOnTabAt(tabIdx, r.schema.withNormalizedAttributeMultiValuedCounts())
                            McpProceduralToolApplyOutcome.okFullJson(
                                """{"ok":true,"resourceUri":${mcpJsonStringLiteral(modelResourceUriForSession(tabSessions[tabIdx].id))}}""",
                                McpProceduralToolLayoutQualityScan(tabIdx, null),
                            )
                        }
                    }
                },
                onApplyEditCanvasElementAtTab = mcpEditEl@{ tabIdx, elementId, patch ->
                    if (tabIdx !in tabSessions.indices) {
                        return@mcpEditEl McpProceduralToolApplyOutcome.err("invalid_tab_index")
                    }
                    val tab = tabSessions[tabIdx]
                    when (val r = applyEditCanvasElement(tab.schema, elementId, patch)) {
                        is ConceptualPropertyEditResult.Err ->
                            McpProceduralToolApplyOutcome.err(r.code)
                        is ConceptualPropertyEditResult.Ok -> {
                            var s = r.schema
                            val beforeEdit = tab.schema
                            val touched = s.elements[elementId] as? SchemaElement.Attribute
                            if (touched != null) {
                                s = s.withAutoSizedAttributeSubtree(
                                    touched.id,
                                    clipboardPreviewTextMeasurer,
                                    layoutDirection,
                                )
                                val fresh = s.elements[elementId] as? SchemaElement.Attribute
                                if (fresh?.isComposite == true) {
                                    s = relayoutCompositeSubtree(s, fresh.id)
                                }
                            }
                            s = s.syncFloatingCardinalityLayoutAfterMutationFromBaseline(
                                baseline = beforeEdit,
                                textMeasurer = clipboardPreviewTextMeasurer,
                                rehomeConnectionsAbsentInBaseline = false,
                            )
                            pushCommitOnTabAt(tabIdx, s.withNormalizedAttributeMultiValuedCounts())
                            McpProceduralToolApplyOutcome.okFullJson(
                                """{"ok":true,"resourceUri":${mcpJsonStringLiteral(modelResourceUriForSession(tabSessions[tabIdx].id))}}""",
                                McpProceduralToolLayoutQualityScan(tabIdx, setOf(elementId)),
                            )
                        }
                    }
                },
                onApplyEditConnectionAtTab = mcpEditConn@{ tabIdx, connectionId, patch ->
                    if (tabIdx !in tabSessions.indices) {
                        return@mcpEditConn McpProceduralToolApplyOutcome.err("invalid_tab_index")
                    }
                    val tab = tabSessions[tabIdx]
                    val prev = tab.schema.connections.firstOrNull { it.id == connectionId }
                        ?: return@mcpEditConn McpProceduralToolApplyOutcome.err("connection_not_found")
                    when (val r = applyEditConnection(tab.schema, connectionId, patch)) {
                        is ConceptualPropertyEditResult.Err ->
                            McpProceduralToolApplyOutcome.err(r.code)
                        is ConceptualPropertyEditResult.Ok -> {
                            var s = r.schema
                            val newConn = s.connections.firstOrNull { it.id == connectionId }
                                ?: return@mcpEditConn McpProceduralToolApplyOutcome.err("connection_not_found")
                            if (
                                patch.keys.any {
                                    it == "cardinalityFixed" || it == "cardinalityAutoSize" || it == "showCardinality"
                                }
                            ) {
                                s = s.withConnectionCardinalityInspectorParity(
                                    prev,
                                    newConn,
                                    clipboardPreviewTextMeasurer,
                                )
                            }
                            pushCommitOnTabAt(tabIdx, s.withNormalizedAttributeMultiValuedCounts())
                            McpProceduralToolApplyOutcome.okFullJson(
                                """{"ok":true,"resourceUri":${mcpJsonStringLiteral(modelResourceUriForSession(tabSessions[tabIdx].id))}}""",
                                McpProceduralToolLayoutQualityScan(
                                    tabIdx,
                                    setOf(prev.elementIdA, prev.elementIdB),
                                ),
                            )
                        }
                    }
                },
                onApplyEditHiddenAttributeAtTab = mcpEditHid@{ tabIdx, holderId, path, patch ->
                    if (tabIdx !in tabSessions.indices) {
                        return@mcpEditHid McpProceduralToolApplyOutcome.err("invalid_tab_index")
                    }
                    val tab = tabSessions[tabIdx]
                    when (val r = applyEditHiddenAttributeAtPath(tab.schema, holderId, path, patch)) {
                        is ConceptualPropertyEditResult.Err ->
                            McpProceduralToolApplyOutcome.err(r.code)
                        is ConceptualPropertyEditResult.Ok -> {
                            pushCommitOnTabAt(tabIdx, r.schema)
                            McpProceduralToolApplyOutcome.okFullJson(
                                """{"ok":true,"resourceUri":${mcpJsonStringLiteral(modelResourceUriForSession(tabSessions[tabIdx].id))}}""",
                                McpProceduralToolLayoutQualityScan(tabIdx, setOf(holderId)),
                            )
                        }
                    }
                },
                onApplyOrganizeAttributesMenuAtTab = mcpOrgMenu@{ tabIdx, attributeSides ->
                    if (tabIdx !in tabSessions.indices) {
                        return@mcpOrgMenu McpProceduralToolApplyOutcome.err("invalid_tab_index")
                    }
                    val tab = tabSessions[tabIdx]
                    if (!canOrganizeAttributesMenuSelection(tab.schema, tab.selection)) {
                        return@mcpOrgMenu McpProceduralToolApplyOutcome.err("organize_attributes_not_applicable")
                    }
                    val beforeOrg = tab.schema
                    val updated = applyOrganizeAttributesMenuAction(beforeOrg, tab.selection, attributeSides)
                        ?: return@mcpOrgMenu McpProceduralToolApplyOutcome.err("organize_attributes_not_applicable")
                    val synced = updated.syncFloatingCardinalityLayoutAfterMutationFromBaseline(
                        baseline = beforeOrg,
                        textMeasurer = clipboardPreviewTextMeasurer,
                        rehomeConnectionsAbsentInBaseline = false,
                    )
                    pushCommitOnTabAt(tabIdx, synced.withNormalizedAttributeMultiValuedCounts())
                    McpProceduralToolApplyOutcome.okFullJson(
                        """{"ok":true,"resourceUri":${mcpJsonStringLiteral(modelResourceUriForSession(tab.id))}}""",
                        McpProceduralToolLayoutQualityScan(
                            tabIdx,
                            canvasElementIdsForLayoutScope(tab.selection),
                        ),
                    )
                },
                onApplyMoveCanvasElementsAtTab = mcpMoveElems@{ tabIdx, seedIds, dx, dy, moveOwned ->
                    if (tabIdx !in tabSessions.indices) {
                        return@mcpMoveElems McpProceduralToolApplyOutcome.err("invalid_tab_index")
                    }
                    when (
                        val r = applyMoveCanvasElementsByTranslation(
                            tabSessions[tabIdx].schema,
                            seedIds,
                            dx,
                            dy,
                            moveOwned,
                        )
                    ) {
                        is ConceptualMoveCanvasElementsApplyResult.Err ->
                            McpProceduralToolApplyOutcome.err(r.code)
                        is ConceptualMoveCanvasElementsApplyResult.Ok -> {
                            val s = r.schema.withCardinalityPositionsAfterElementsMovedByDelta(
                                movedElementIds = r.movedElementIds,
                                dx = dx,
                                dy = dy,
                                selectedCardinalityConnectionIds = emptySet(),
                                textMeasurer = clipboardPreviewTextMeasurer,
                            )
                            pushCommitOnTabAt(tabIdx, s.withNormalizedAttributeMultiValuedCounts())
                            val idsJson = r.movedElementIds.sorted().joinToString(",", prefix = "[", postfix = "]")
                            McpProceduralToolApplyOutcome.okFullJson(
                                """{"ok":true,"resourceUri":${mcpJsonStringLiteral(modelResourceUriForSession(tabSessions[tabIdx].id))},"movedElementIds":$idsJson,"deltaX":$dx,"deltaY":$dy}""",
                                McpProceduralToolLayoutQualityScan(tabIdx, r.movedElementIds),
                            )
                        }
                    }
                },
                onConceptualSearchFind = mcpFind@{ tabIdx, query, filters, scopeText ->
                    val tab = tabSessions.getOrNull(tabIdx)
                        ?: return@mcpFind ConceptualSearchOutcome.Err("invalid_tab_index")
                    tab.schema.searchConceptualModel(query, filters, scopeText)
                },
                onConceptualSearchApplyHit = mcpApplyHit@{ tabIdx, hit ->
                    val tab = tabSessions.getOrNull(tabIdx) ?: return@mcpApplyHit "invalid_tab_index"
                    val action = conceptualSearchNavigateAction(tab.schema, hit, clipboardPreviewTextMeasurer)
                        ?: return@mcpApplyHit "unknown_hit"
                    applyConceptualSearchNavigateOnTab(tabIdx, action)
                    null
                },
                onEncodeTabConceptualMenuExportPng = mcpTabPng@{ tabIdx ->
                    if (tabIdx !in tabSessions.indices) return@mcpTabPng null
                    val tab = tabSessions[tabIdx]
                    encodeConceptualSchemaAsMenuExportPngBytesBlocking(
                        tab.schema,
                        clipboardPreviewTextMeasurer,
                        clipboardPreviewDensity,
                    )
                },
                onEncodeTabConceptualMenuExportJpeg = mcpTabJpg@{ tabIdx ->
                    if (tabIdx !in tabSessions.indices) return@mcpTabJpg null
                    val tab = tabSessions[tabIdx]
                    encodeConceptualSchemaAsMenuExportJpegBytesBlocking(
                        tab.schema,
                        clipboardPreviewTextMeasurer,
                        clipboardPreviewDensity,
                    )
                },
                onEncodeConceptualElementSubsetRaster = mcpSubsetRaster@{ tabIdx, seeds, format ->
                    if (tabIdx !in tabSessions.indices) return@mcpSubsetRaster null
                    val tab = tabSessions[tabIdx]
                    encodeConceptualElementSubsetRasterBlocking(
                        tab.schema,
                        seeds,
                        format,
                        clipboardPreviewTextMeasurer,
                        clipboardPreviewDensity,
                    )
                },
                onApplyCanvasSelectionRectangleAtTab = mcpRect@{ tabIdx, x0, y0, x1, y1, mergeMode, dryRun, requestWindowFocus ->
                    if (tabIdx !in tabSessions.indices) {
                        return@mcpRect McpProceduralToolApplyOutcome.err("invalid_tab_index")
                    }
                    val tab = tabSessions[tabIdx]
                    val schema = tab.schema
                    val selectionBefore = tab.selection
                    val band = ConceptualBulkDeleteBand.fromCorners(
                        x0.toFloat(),
                        y0.toFloat(),
                        x1.toFloat(),
                        y1.toFloat(),
                    )
                    val pick = selectionBandGeometricPick(schema, band, clipboardPreviewTextMeasurer)
                    val bandE = pick.elementIds
                    val bandC = pick.cardinalityConnectionIds
                    val selectionAfter = mergeCanvasRectangleSelection(mergeMode, selectionBefore, bandE, bandC)
                    val (deltaE, deltaC) = canvasSelectionSymmetricPickDelta(selectionBefore, selectionAfter)
                    val setsBefore = selectionBefore.toMultiPickSets()
                    val setsAfter = selectionAfter.toMultiPickSets()
                    val selectionMutated = setsBefore != setsAfter
                    var committed = false
                    if (!dryRun) {
                        if (selectionMutated) {
                            replaceTabAt(
                                tabIdx,
                                tab.copy(selection = selectionAfter, hiddenAttributeRevealPath = null),
                            )
                            committed = true
                        }
                        val focusApplied = requestWindowFocus && isDesktopTarget
                        if (focusApplied) {
                            requestDesktopMainWindowToFront()
                        }
                        if (selectionMutated || focusApplied) {
                            launchMcpUserNotice(
                                McpAgentUserNotice(
                                    selectionChanged = selectionMutated,
                                    windowFocused = focusApplied,
                                ),
                            )
                        }
                    }
                    val focusAppliedForJson = !dryRun && requestWindowFocus && isDesktopTarget
                    val body = McpSelectionRectangleResponseJson.canvasSelectionRectangleSuccess(
                        resourceUri = modelResourceUriForSession(tab.id),
                        dryRun = dryRun,
                        mergeMode = mergeMode,
                        requestWindowFocusRequested = requestWindowFocus,
                        requestWindowFocusApplied = focusAppliedForJson,
                        selectionCommittedToUi = !dryRun && committed,
                        bandElementIds = bandE,
                        bandCardinalityIds = bandC,
                        selectionBefore = selectionBefore,
                        selectionAfterProjection = selectionAfter,
                        selectionSymmetricDeltaElements = deltaE,
                        selectionSymmetricDeltaCardinality = deltaC,
                    )
                    McpProceduralToolApplyOutcome.okFullJson(body)
                },
                onSetCanvasSelectionAtTab = mcpSetSel@{ tabIdx, newSel ->
                    if (tabIdx !in tabSessions.indices) return@mcpSetSel
                    val tab = tabSessions[tabIdx]
                    replaceTabAt(
                        tabIdx,
                        tab.copy(selection = newSel, hiddenAttributeRevealPath = null),
                    )
                },
                onRequestAppWindowFocus = {
                    if (isDesktopTarget) {
                        requestDesktopMainWindowToFront()
                    }
                },
                onShowMcpAgentUserNotice = launchMcpUserNotice,
                onServerRunningChanged = { running -> mcpServerRunning = running },
            )
        } else {
            McpDesktopSync.clearBindings(mcpRuntime)
        }
    }

    LaunchedEffect(tabSessions, selectedTabIndex) {
        if (isDesktopTarget) {
            mcpRuntime.onTabsChanged()
        }
    }

    val ribbonMcp = if (isDesktopTarget) {
        RibbonMcpUi(
            onOpenSettings = { mcpRuntime.openSettingsDialog() },
            onStartServer = {
                mcpRuntime.startServer()
                mcpServerRunning = mcpRuntime.isServerRunning()
            },
            onStopServer = {
                mcpRuntime.stopServer()
                mcpServerRunning = mcpRuntime.isServerRunning()
            },
            onCopyServerAddress = {
                val url = mcpServerUrlFromStoredSettings()
                val ok = brModeloClipboardSetPlainText(url)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (ok) {
                            "Endereço do servidor copiado para a área de transferência."
                        } else {
                            "Não foi possível copiar o endereço."
                        },
                    )
                }
            },
            startServerEnabled = !mcpServerRunning,
            stopServerEnabled = mcpServerRunning,
        )
    } else {
        null
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE3E3E3)) {
            Box(modifier = Modifier.fillMaxSize()) {
                BrModeloScreen(
                    isMainMenuOpen = isMainMenuOpen,
                    activeMenu = activeMenu,
                    selectedTab = selectedRibbonTab,
                    canvasTabs = tabSessions,
                    selectedCanvasTabIndex = selectedTabIndex,
                    onSelectCanvasTab = { selectedTabIndex = it },
                    onRequestCloseCanvasTab = { requestCloseTab(it) },
                    schema = sel.schema,
                    inspectorCommittedSchema = sel.inspectorCommittedSchema,
                    selection = sel.selection,
                    isDragOver = isDragOver,
                    onMainMenuToggle = {
                        isMainMenuOpen = !isMainMenuOpen
                        if (!isMainMenuOpen) activeMenu = null
                    },
                    onMainMenuHover = { activeMenu = it },
                    onTabSelect = { selectedRibbonTab = it },
                    onDismissMenu = {
                        isMainMenuOpen = false
                        activeMenu = null
                    },
                    onOpenFile = openFile,
                    onNewConceptualModel = { addBlankTab() },
                    onDragStateChange = { isDragOverFromCallback = it },
                    onFileDrop = loadPickedFile,
                    onSelectionChange = { selNew ->
                        mutateSelectedTab { it.copy(selection = selNew, hiddenAttributeRevealPath = null) }
                    },
                    onSchemaPreview = onSchemaPreview,
                    onSchemaCommit = onSchemaCommit,
                    onRevertSchemaPreview = onRevertSchemaPreview,
                    onCloseCurrentModel = { requestCloseTab(selectedTabIndex) },
                    onQuitApplication = requestApplicationQuit,
                    onSave = { enqueueSave(saveAs = false) },
                    onSaveAs = { enqueueSave(saveAs = true) },
                    onOpenSchemaDataDictionary = {
                        sel.schema?.let { schemaDataDictionarySchema = it }
                    },
                    schemaDataDictionaryEnabled = sel.schema != null,
                    entityToolBinding = entityToolBinding,
                    observationToolBinding = observationToolBinding,
                    linkObjectsToolBinding = linkObjectsToolBinding,
                    autoSelfRelationshipToolBinding = autoSelfRelationshipToolBinding,
                    specializationToolBinding = specializationToolBinding,
                    attributeToolBinding = attributeToolBinding,
                    bulkDeleteObjectsToolBinding = bulkDeleteObjectsToolBinding,
                    rectangleSelectionToolBinding = rectangleSelectionToolBinding,
                    operationsMenuBinding = operationsMenuBinding,
                    conceptualCanvasTool = conceptualCanvasTool,
                    onConceptualCanvasToolChange = { conceptualCanvasTool = it },
                    onClearConceptualCanvasTool = { conceptualCanvasTool = ConceptualCanvasTool.None },
                    bulkDeleteUiState = bulkDeleteUi,
                    onBulkDeleteUiChange = { bulkDeleteUi = it },
                    selectionBandUiState = selectionBandUi,
                    onSelectionBandUiChange = { selectionBandUi = it },
                    onOrganizeAttributes = onOrganizeAttributes,
                    hiddenAttributeRevealPath = sel.hiddenAttributeRevealPath,
                    onHiddenAttributeRevealPathChange = { p ->
                        mutateSelectedTab { it.copy(hiddenAttributeRevealPath = p) }
                    },
                    onRevealHiddenAttributeInModel = onRevealHiddenAttribute,
                    clipboardRibbonBinding = clipboardRibbonBinding,
                    selectFontRibbonBinding = selectFontRibbonBinding,
                    onCanvasViewStateChange = { schemaCanvasViewState = it },
                    onCopyRequest = onCopyConceptualClipboard,
                    onCutRequest = onCutConceptualClipboard,
                    onPasteRequest = onPasteConceptualClipboard,
                    onUndoRequest = {
                        if (operationsMenuBinding.undoEnabled) operationsMenuBinding.onUndo()
                    },
                    onRedoRequest = {
                        if (operationsMenuBinding.redoEnabled) operationsMenuBinding.onRedo()
                    },
                    requestCenterOnModelBounds = canvasCenterOnBoundsRequest,
                    onRequestCenterOnModelBoundsConsumed = { canvasCenterOnBoundsRequest = null },
                    requestedInspectorTab = inspectorTabRequest,
                    onInspectorTabRequestConsumed = { inspectorTabRequest = null },
                    requestedSelectionFieldFocus = inspectorSelectionFieldFocusRequest,
                    onSelectionFieldFocusRequestConsumed = { inspectorSelectionFieldFocusRequest = null },
                    onConceptualInspectorSelectionFieldEditRequest = { fieldKey ->
                        inspectorFieldFocusRevision++
                        inspectorSelectionFieldFocusRequest =
                            InspectorSelectionFieldFocusRequest(fieldKey, inspectorFieldFocusRevision)
                    },
                    onRequestOpenConceptualFind = { conceptualSearchDialogOpen = true },
                    snackbarHostState = snackbarHostState,
                    ribbonMcp = ribbonMcp,
                )

                if (conceptualSearchDialogOpen) {
                    ConceptualSearchDialog(
                        schema = sel.schema,
                        onDismiss = { conceptualSearchDialogOpen = false },
                        onNavigate = { action ->
                            applyConceptualSearchNavigateAction(action)
                            conceptualSearchDialogOpen = false
                        },
                    )
                }

                schemaDataDictionarySchema?.let { dictSchema ->
                    ConceptualSchemaDictionaryDialog(
                        schema = dictSchema,
                        onDismiss = { schemaDataDictionarySchema = null },
                        onTransientUserMessage = { msg ->
                            scope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        },
                    )
                }

                conceptualLabelFontRequest?.let { fontReq ->
                    fun commitLabelFont(chosen: LabelStyle) {
                        val tabIdx = tabSessions.indexOfFirst { it.id == fontReq.editorTabId }
                        if (tabIdx < 0) return
                        val tab = tabSessions[tabIdx]
                        if (tab.schema.elements[fontReq.elementId] == null) return
                        val normalized = tab.schema
                            .withElementLabelStyle(fontReq.elementId, chosen)
                            .withNormalizedAttributeMultiValuedCounts()
                        tab.history.push(normalized)
                        val committed = tab.history.current ?: return
                        replaceTabAt(
                            tabIdx,
                            tab.copy(
                                schema = committed,
                                inspectorCommittedSchema = committed,
                            ),
                        )
                    }
                    ConceptualLabelFontChooserDialog(
                        request = fontReq,
                        onDismiss = { conceptualLabelFontRequest = null },
                        onResetToDefault = {
                            commitLabelFont(ConceptualPlacementDefaults.labelStyle)
                            conceptualLabelFontRequest = null
                        },
                        onConfirm = { chosen ->
                            commitLabelFont(chosen)
                            conceptualLabelFontRequest = null
                        },
                    )
                }

                bulkDataDictionaryRows?.let { dictRows ->
                    BulkDataDictionaryDialog(
                        rows = dictRows,
                        onDismiss = { bulkDataDictionaryRows = null },
                        onCommit = { writes ->
                            val idx = selectedTabIndex
                            val tab = tabSessions.getOrNull(idx)
                            if (tab != null && writes.isNotEmpty()) {
                                val next = applyDictionarySlots(tab.schema, writes)
                                    ?.withNormalizedAttributeMultiValuedCounts()
                                if (next != null) {
                                    pushCommitOnSelected(next)
                                }
                            }
                            bulkDataDictionaryRows = null
                        },
                    )
                }

                if (showMcpSettings && isDesktopTarget) {
                    val (mh, mp, ma) = McpSettingsStore.load()
                    McpSettingsDialog(
                        initialBindHost = mh,
                        initialPort = mp,
                        initialAllowLanHosts = ma,
                        onDismiss = { showMcpSettings = false },
                        onConfirm = { nh, np, na ->
                            McpSettingsStore.save(nh, np, na)
                            showMcpSettings = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Configurações MCP salvas.")
                            }
                        },
                    )
                }

                pendingCloseTabIndex?.let { closeIdx ->
                    if (closeIdx in tabSessions.indices) {
                        val title = tabSessions[closeIdx].displayTitle()
                        CloseTabUnsavedDialog(
                            documentTitle = title,
                            onSave = {
                                scope.launch {
                                    if (saveTabAt(closeIdx, false)) {
                                        pendingCloseTabIndex = null
                                        performRemoveTab(closeIdx)
                                    }
                                }
                            },
                            onDiscard = {
                                pendingCloseTabIndex = null
                                performRemoveTab(closeIdx)
                            },
                            onCancel = { pendingCloseTabIndex = null },
                        )
                    } else {
                        pendingCloseTabIndex = null
                    }
                }

                if (pendingApplicationQuit) {
                    QuitApplicationUnsavedDialog(
                        showSaveAll = isDesktopTarget,
                        onSaveAll = {
                            scope.launch {
                                val indices = tabSessions.indices.filter { tabSessions[it].needsCloseConfirmation() }
                                for (i in indices) {
                                    if (!saveTabAt(i, false)) return@launch
                                }
                                pendingApplicationQuit = false
                                quitApplicationCompletely()
                            }
                        },
                        onQuitWithoutSaving = {
                            pendingApplicationQuit = false
                            quitApplicationCompletely()
                        },
                        onCancel = { pendingApplicationQuit = false },
                    )
                }
            }
        }
    }
}

/**
 * Desktop window caption and browser tab title: app name, version, and the loaded model name when present.
 */
internal fun formatApplicationWindowTitle(modelDisplayName: String?): String {
    val appName = "Power brModelo ${BuildInfo.displayVersion}"
    val name = modelDisplayName?.trim().orEmpty()
    return if (name.isEmpty()) appName else "$name — $appName"
}

private fun mergeLoadedModel(parsed: ConceptualSchema, openedFromBrm: Boolean, picked: PickedFile): ConceptualSchema =
    parsed.copy(
        name = picked.name,
        filePath = picked.diskPath ?: "",
        openedFromBrm = openedFromBrm,
    )

/** Non-null return value is an MCP error code for [onOpenXmlAsUnsavedTab]. */
private fun validateMcpOpenXmlBasename(fileName: String): String? {
    val t = fileName.trim()
    if (t.isEmpty()) return "fileName_required"
    if (t.any { it == '/' || it == '\\' }) return "fileName_must_be_basename_no_path"
    return null
}

/** Display name for [ConceptualSchema.name] from MCP `fileName` (basename, may include extension). */
private fun schemaNameFromMcpXmlBasename(fileName: String): String {
    val t = fileName.trim()
    val dot = t.lastIndexOf('.')
    val stem = if (dot > 0) t.substring(0, dot) else t
    return stem.ifBlank { "modelo" }
}

internal fun parseModelBytes(bytes: ByteArray): ConceptualSchema =
    parseModelBytesWithSource(bytes).first

internal fun parseModelBytesWithSource(bytes: ByteArray): Pair<ConceptualSchema, Boolean> {
    val isBrm = bytes.size > 10 &&
        bytes[6] == 'T'.code.toByte() &&
        bytes[7] == 'P'.code.toByte() &&
        bytes[8] == 'F'.code.toByte() &&
        bytes[9] == '0'.code.toByte()
    val schema = if (isBrm) ConceptualSchemaBrmParser.parse(bytes)
    else ConceptualSchemaXmlParser.parse(bytes)
    return schema to isBrm
}
