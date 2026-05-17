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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.ui.AttributeToolRibbonBinding
import games.polyclub.power.brmodelo.ui.AutoSelfRelationshipToolRibbonBinding
import games.polyclub.power.brmodelo.ui.BulkDeleteObjectsToolRibbonBinding
import games.polyclub.power.brmodelo.ui.BulkDeleteUiState
import games.polyclub.power.brmodelo.ui.ClipboardRibbonBinding
import games.polyclub.power.brmodelo.ui.ConceptualCanvasTool
import games.polyclub.power.brmodelo.ui.EditorTabSession
import games.polyclub.power.brmodelo.ui.EntityToolRibbonBinding
import games.polyclub.power.brmodelo.ui.LinkObjectsToolRibbonBinding
import games.polyclub.power.brmodelo.ui.MainMenuType
import games.polyclub.power.brmodelo.ui.ObservationToolRibbonBinding
import games.polyclub.power.brmodelo.ui.OperationsMenuRibbonBinding
import games.polyclub.power.brmodelo.ui.PickedFile
import games.polyclub.power.brmodelo.ui.RectangleSelectionToolRibbonBinding
import games.polyclub.power.brmodelo.ui.SelectFontRibbonBinding
import games.polyclub.power.brmodelo.ui.RibbonTab
import games.polyclub.power.brmodelo.ui.SelectionBandUiState
import games.polyclub.power.brmodelo.ui.SpecializationToolRibbonBinding
import games.polyclub.power.brmodelo.ui.canvas.SchemaCanvasViewState
import games.polyclub.power.brmodelo.ui.canvas.renderSchemaToImageBitmap
import games.polyclub.power.brmodelo.ui.components.ribbon.HeaderRibbon
import games.polyclub.power.brmodelo.ui.saveExportedImage
import kotlinx.coroutines.launch

private val MENU_TOP_OFFSET = 30.dp

@Composable
internal fun BrModeloScreen(
    isMainMenuOpen: Boolean,
    activeMenu: MainMenuType?,
    selectedTab: RibbonTab,
    canvasTabs: List<EditorTabSession>,
    selectedCanvasTabIndex: Int,
    onSelectCanvasTab: (Int) -> Unit,
    onRequestCloseCanvasTab: (Int) -> Unit,
    schema: ConceptualSchema?,
    inspectorCommittedSchema: ConceptualSchema? = null,
    selection: CanvasSelection = CanvasSelection.None,
    isDragOver: Boolean = false,
    onMainMenuToggle: () -> Unit,
    onMainMenuHover: (MainMenuType) -> Unit,
    onTabSelect: (RibbonTab) -> Unit,
    onDismissMenu: () -> Unit,
    onOpenFile: () -> Unit,
    onNewConceptualModel: () -> Unit = {},
    onDragStateChange: (Boolean) -> Unit = {},
    onFileDrop: (PickedFile) -> Unit = {},
    onSelectionChange: (CanvasSelection) -> Unit = {},
    onSchemaPreview: (ConceptualSchema) -> Unit = {},
    onSchemaCommit: (ConceptualSchema) -> Unit = {},
    onRevertSchemaPreview: () -> Unit = {},
    onCloseCurrentModel: () -> Unit = {},
    onQuitApplication: () -> Unit = {},
    onSave: () -> Unit = {},
    onSaveAs: () -> Unit = {},
    onOpenSchemaDataDictionary: () -> Unit = {},
    schemaDataDictionaryEnabled: Boolean = false,
    entityToolBinding: EntityToolRibbonBinding? = null,
    observationToolBinding: ObservationToolRibbonBinding? = null,
    linkObjectsToolBinding: LinkObjectsToolRibbonBinding? = null,
    autoSelfRelationshipToolBinding: AutoSelfRelationshipToolRibbonBinding? = null,
    specializationToolBinding: SpecializationToolRibbonBinding? = null,
    attributeToolBinding: AttributeToolRibbonBinding? = null,
    bulkDeleteObjectsToolBinding: BulkDeleteObjectsToolRibbonBinding? = null,
    rectangleSelectionToolBinding: RectangleSelectionToolRibbonBinding? = null,
    operationsMenuBinding: OperationsMenuRibbonBinding? = null,
    conceptualCanvasTool: ConceptualCanvasTool = ConceptualCanvasTool.None,
    onConceptualCanvasToolChange: (ConceptualCanvasTool) -> Unit = {},
    onClearConceptualCanvasTool: () -> Unit = {},
    bulkDeleteUiState: BulkDeleteUiState? = null,
    onBulkDeleteUiChange: (BulkDeleteUiState?) -> Unit = {},
    selectionBandUiState: SelectionBandUiState? = null,
    onSelectionBandUiChange: (SelectionBandUiState?) -> Unit = {},
    onOrganizeAttributes: () -> Unit = {},
    hiddenAttributeRevealPath: List<Int>? = null,
    onHiddenAttributeRevealPathChange: (List<Int>?) -> Unit = {},
    onRevealHiddenAttributeInModel: () -> Unit = {},
    clipboardRibbonBinding: ClipboardRibbonBinding? = null,
    selectFontRibbonBinding: SelectFontRibbonBinding? = null,
    onCanvasViewStateChange: (SchemaCanvasViewState) -> Unit = {},
    onCopyRequest: () -> Unit = {},
    onCutRequest: () -> Unit = {},
    onPasteRequest: () -> Unit = {},
    onUndoRequest: () -> Unit = {},
    onRedoRequest: () -> Unit = {},
    requestCenterOnModelBounds: ElementPosition? = null,
    onRequestCenterOnModelBoundsConsumed: () -> Unit = {},
    requestedInspectorTab: InspectorTab? = null,
    onInspectorTabRequestConsumed: () -> Unit = {},
    requestedSelectionFieldFocus: InspectorSelectionFieldFocusRequest? = null,
    onSelectionFieldFocusRequestConsumed: () -> Unit = {},
    onRequestOpenConceptualFind: () -> Unit = {},
    onConceptualInspectorSelectionFieldEditRequest: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState,
    ribbonMcp: RibbonMcpUi? = null,
) {
    var exportCounter by remember { mutableIntStateOf(0) }
    var exportIsJpeg by remember { mutableStateOf(true) }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(exportCounter) {
        if (exportCounter == 0 || schema == null) return@LaunchedEffect
        val bitmap = renderSchemaToImageBitmap(
            schema = schema,
            textMeasurer = textMeasurer,
            density = density,
            withBackground = exportIsJpeg,
        )
        saveExportedImage(bitmap, exportIsJpeg, schema.name.ifBlank { "modelo" })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderRibbon(
                selectedTab = selectedTab,
                entityToolBinding = entityToolBinding,
                observationToolBinding = observationToolBinding,
                linkObjectsToolBinding = linkObjectsToolBinding,
                autoSelfRelationshipToolBinding = autoSelfRelationshipToolBinding,
                specializationToolBinding = specializationToolBinding,
                attributeToolBinding = attributeToolBinding,
                bulkDeleteObjectsToolBinding = bulkDeleteObjectsToolBinding,
                rectangleSelectionToolBinding = rectangleSelectionToolBinding,
                operationsMenuBinding = operationsMenuBinding,
                clipboardRibbonBinding = clipboardRibbonBinding,
                selectFontRibbonBinding = selectFontRibbonBinding,
                ribbonMcp = ribbonMcp,
                onRibbonUserMessage = { msg ->
                    scope.launch {
                        snackbarHostState.showSnackbar(msg)
                    }
                },
                onMainMenuClick = onMainMenuToggle,
                onTabSelect = onTabSelect,
            )
            WorkspaceArea(
                canvasTabs = canvasTabs,
                selectedCanvasTabIndex = selectedCanvasTabIndex,
                onSelectCanvasTab = onSelectCanvasTab,
                onRequestCloseCanvasTab = onRequestCloseCanvasTab,
                schema = schema,
                inspectorCommittedSchema = inspectorCommittedSchema,
                selection = selection,
                isDragOver = isDragOver,
                onDragStateChange = onDragStateChange,
                onFileDrop = onFileDrop,
                onSelectionChange = onSelectionChange,
                onSchemaPreview = onSchemaPreview,
                onSchemaCommit = onSchemaCommit,
                onRevertSchemaPreview = onRevertSchemaPreview,
                conceptualCanvasTool = conceptualCanvasTool,
                onConceptualCanvasToolChange = onConceptualCanvasToolChange,
                onTransientUserMessage = { msg ->
                    scope.launch {
                        snackbarHostState.showSnackbar(msg)
                    }
                },
                onClearConceptualCanvasTool = onClearConceptualCanvasTool,
                bulkDeleteUiState = bulkDeleteUiState,
                onBulkDeleteUiChange = onBulkDeleteUiChange,
                selectionBandUiState = selectionBandUiState,
                onSelectionBandUiChange = onSelectionBandUiChange,
                onOrganizeAttributes = onOrganizeAttributes,
                hiddenAttributeRevealPath = hiddenAttributeRevealPath,
                onHiddenAttributeRevealPathChange = onHiddenAttributeRevealPathChange,
                onRevealHiddenAttributeInModel = onRevealHiddenAttributeInModel,
                onCanvasViewStateChange = onCanvasViewStateChange,
                onCopyRequest = onCopyRequest,
                onCutRequest = onCutRequest,
                onPasteRequest = onPasteRequest,
                onUndoRequest = onUndoRequest,
                onRedoRequest = onRedoRequest,
                requestCenterOnModelBounds = requestCenterOnModelBounds,
                onRequestCenterOnModelBoundsConsumed = onRequestCenterOnModelBoundsConsumed,
                requestedInspectorTab = requestedInspectorTab,
                onInspectorTabRequestConsumed = onInspectorTabRequestConsumed,
                requestedSelectionFieldFocus = requestedSelectionFieldFocus,
                onSelectionFieldFocusRequestConsumed = onSelectionFieldFocusRequestConsumed,
                onRequestOpenConceptualFind = onRequestOpenConceptualFind,
                onConceptualInspectorSelectionFieldEditRequest = onConceptualInspectorSelectionFieldEditRequest,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
        )

        if (isMainMenuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onDismissMenu() },
            )
            FunctionalMainMenu(
                modifier = Modifier
                    .padding(start = 4.dp, top = MENU_TOP_OFFSET)
                    .height(340.dp),
                activeMenu = activeMenu,
                onMenuHover = onMainMenuHover,
                onOpenFile = {
                    onDismissMenu()
                    onOpenFile()
                },
                onNewConceptualModel = {
                    onDismissMenu()
                    onNewConceptualModel()
                },
                onCloseCurrentModel = {
                    onDismissMenu()
                    onCloseCurrentModel()
                },
                onQuitApplication = {
                    onDismissMenu()
                    onQuitApplication()
                },
                onExportJpeg = {
                    onDismissMenu()
                    exportIsJpeg = true
                    exportCounter++
                },
                onExportPng = {
                    onDismissMenu()
                    exportIsJpeg = false
                    exportCounter++
                },
                onOpenSchemaDataDictionary = {
                    onDismissMenu()
                    onOpenSchemaDataDictionary()
                },
                schemaDataDictionaryEnabled = schemaDataDictionaryEnabled,
                onSave = {
                    onDismissMenu()
                    onSave()
                },
                onSaveAs = {
                    onDismissMenu()
                    onSaveAs()
                },
            )
        }
    }
}
