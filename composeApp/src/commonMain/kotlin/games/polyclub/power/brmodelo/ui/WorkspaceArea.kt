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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.ui.canvas.SchemaCanvasViewState

@Composable
internal fun WorkspaceArea(
    canvasTabs: List<EditorTabSession>,
    selectedCanvasTabIndex: Int,
    onSelectCanvasTab: (Int) -> Unit,
    onRequestCloseCanvasTab: (Int) -> Unit,
    schema: ConceptualSchema?,
    inspectorCommittedSchema: ConceptualSchema? = null,
    selection: CanvasSelection = CanvasSelection.None,
    isDragOver: Boolean = false,
    onDragStateChange: (Boolean) -> Unit = {},
    onFileDrop: (PickedFile) -> Unit = {},
    onSelectionChange: (CanvasSelection) -> Unit = {},
    onSchemaPreview: (ConceptualSchema) -> Unit = {},
    onSchemaCommit: (ConceptualSchema) -> Unit = {},
    onRevertSchemaPreview: () -> Unit = {},
    conceptualCanvasTool: ConceptualCanvasTool = ConceptualCanvasTool.None,
    onConceptualCanvasToolChange: (ConceptualCanvasTool) -> Unit = {},
    onTransientUserMessage: (String) -> Unit = {},
    onClearConceptualCanvasTool: () -> Unit = {},
    bulkDeleteUiState: BulkDeleteUiState? = null,
    onBulkDeleteUiChange: (BulkDeleteUiState?) -> Unit = {},
    selectionBandUiState: SelectionBandUiState? = null,
    onSelectionBandUiChange: (SelectionBandUiState?) -> Unit = {},
    onOrganizeAttributes: () -> Unit = {},
    hiddenAttributeRevealPath: List<Int>? = null,
    onHiddenAttributeRevealPathChange: (List<Int>?) -> Unit = {},
    onRevealHiddenAttributeInModel: () -> Unit = {},
    onCanvasViewStateChange: (SchemaCanvasViewState) -> Unit = {},
    requestCenterOnModelBounds: ElementPosition? = null,
    onRequestCenterOnModelBoundsConsumed: () -> Unit = {},
    requestedInspectorTab: InspectorTab? = null,
    onInspectorTabRequestConsumed: () -> Unit = {},
    onRequestOpenConceptualFind: () -> Unit = {},
    onCopyRequest: () -> Unit = {},
    onCutRequest: () -> Unit = {},
    onPasteRequest: () -> Unit = {},
    onUndoRequest: () -> Unit = {},
    onRedoRequest: () -> Unit = {},
) {
    Row(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        MainCanvasPanel(
            canvasTabs = canvasTabs,
            selectedCanvasTabIndex = selectedCanvasTabIndex,
            onSelectCanvasTab = onSelectCanvasTab,
            onRequestCloseCanvasTab = onRequestCloseCanvasTab,
            schema = schema,
            selection = selection,
            isDragOver = isDragOver,
            onDragStateChange = onDragStateChange,
            onFileDrop = onFileDrop,
            onSelectionChange = onSelectionChange,
            onSchemaPreview = onSchemaPreview,
            onSchemaCommit = onSchemaCommit,
            conceptualCanvasTool = conceptualCanvasTool,
            onConceptualCanvasToolChange = onConceptualCanvasToolChange,
            onTransientUserMessage = onTransientUserMessage,
            onClearConceptualCanvasTool = onClearConceptualCanvasTool,
            bulkDeleteUiState = bulkDeleteUiState,
            onBulkDeleteUiChange = onBulkDeleteUiChange,
            selectionBandUiState = selectionBandUiState,
            onSelectionBandUiChange = onSelectionBandUiChange,
            onOrganizeAttributes = onOrganizeAttributes,
            onCanvasViewStateChange = onCanvasViewStateChange,
            requestCenterOnModelBounds = requestCenterOnModelBounds,
            onRequestCenterOnModelBoundsConsumed = onRequestCenterOnModelBoundsConsumed,
            onRequestOpenConceptualFind = onRequestOpenConceptualFind,
            onCopyRequest = onCopyRequest,
            onCutRequest = onCutRequest,
            onPasteRequest = onPasteRequest,
            onUndoRequest = onUndoRequest,
            onRedoRequest = onRedoRequest,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        InspectorPanel(
            schema = schema,
            inspectorCommittedSchema = inspectorCommittedSchema,
            selection = selection,
            conceptualCanvasTool = conceptualCanvasTool,
            bulkDeleteUiState = bulkDeleteUiState,
            selectionBandUiState = selectionBandUiState,
            onSchemaPreview = onSchemaPreview,
            onSchemaCommit = onSchemaCommit,
            onRevertSchemaPreview = onRevertSchemaPreview,
            hiddenAttributeRevealPath = hiddenAttributeRevealPath,
            onHiddenAttributeRevealPathChange = onHiddenAttributeRevealPathChange,
            onRevealHiddenAttributeInModel = onRevealHiddenAttributeInModel,
            requestedInspectorTab = requestedInspectorTab,
            onInspectorTabRequestConsumed = onInspectorTabRequestConsumed,
        )
    }
}
