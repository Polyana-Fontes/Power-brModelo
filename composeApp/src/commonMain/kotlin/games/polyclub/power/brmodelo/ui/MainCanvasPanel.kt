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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.focusable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.canOrganizeAttributesMenuSelection
import games.polyclub.power.brmodelo.domain.canvasSelectionSelectAllElements
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.deleteCanvasSelection
import games.polyclub.power.brmodelo.ui.ConceptualCanvasTool
import games.polyclub.power.brmodelo.ui.canvas.applyCanvasKeyboardArrow
import games.polyclub.power.brmodelo.ui.canvas.SchemaCanvas
import games.polyclub.power.brmodelo.ui.canvas.SchemaCanvasViewState
import games.polyclub.power.brmodelo.ui.canvas.rememberConceptualCanvasToolCursorModifier
import games.polyclub.power.brmodelo.ui.components.CHROMIUM_TAB_ACTIVE_HEIGHT
import games.polyclub.power.brmodelo.ui.components.CHROMIUM_TAB_INACTIVE_HEIGHT
import games.polyclub.power.brmodelo.ui.components.CHROMIUM_TAB_STRIP_HEIGHT
import games.polyclub.power.brmodelo.ui.components.ChromiumTab
import games.polyclub.power.brmodelo.ui.components.ChromiumTabShape
import games.polyclub.power.brmodelo.ui.theme.AppColorPalette
import games.polyclub.power.brmodelo.ui.theme.LocalAppColorPalette
import games.polyclub.power.brmodelo.ui.theme.LocalConceptualModelColorPalette
import games.polyclub.power.brmodelo.generated.resources.Res
import games.polyclub.power.brmodelo.generated.resources.modelo_conceitual_2s
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun MainCanvasPanel(
    canvasTabs: List<EditorTabSession>,
    selectedCanvasTabIndex: Int,
    onSelectCanvasTab: (Int) -> Unit,
    onRequestCloseCanvasTab: (Int) -> Unit,
    schema: ConceptualSchema?,
    selection: CanvasSelection,
    isDragOver: Boolean = false,
    onDragStateChange: (Boolean) -> Unit = {},
    onFileDrop: (PickedFile) -> Unit = {},
    onSelectionChange: (CanvasSelection) -> Unit = {},
    onSchemaPreview: (ConceptualSchema) -> Unit = {},
    onSchemaCommit: (ConceptualSchema) -> Unit = {},
    conceptualCanvasTool: ConceptualCanvasTool = ConceptualCanvasTool.None,
    onConceptualCanvasToolChange: (ConceptualCanvasTool) -> Unit = {},
    onTransientUserMessage: (String) -> Unit = {},
    onClearConceptualCanvasTool: () -> Unit = {},
    bulkDeleteUiState: BulkDeleteUiState? = null,
    onBulkDeleteUiChange: (BulkDeleteUiState?) -> Unit = {},
    selectionBandUiState: SelectionBandUiState? = null,
    onSelectionBandUiChange: (SelectionBandUiState?) -> Unit = {},
    onOrganizeAttributes: () -> Unit = {},
    onCanvasViewStateChange: (SchemaCanvasViewState) -> Unit = {},
    requestCenterOnModelBounds: ElementPosition? = null,
    onRequestCenterOnModelBoundsConsumed: () -> Unit = {},
    onRequestOpenConceptualFind: () -> Unit = {},
    onConceptualInspectorSelectionFieldEditRequest: (String) -> Unit = {},
    onCopyRequest: () -> Unit = {},
    onCutRequest: () -> Unit = {},
    onPasteRequest: () -> Unit = {},
    onUndoRequest: () -> Unit = {},
    onRedoRequest: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val app = LocalAppColorPalette.current
    val model = LocalConceptualModelColorPalette.current
    val selectedTab = canvasTabs.getOrNull(selectedCanvasTabIndex)
    val canvasKey = selectedTab?.id ?: 0L

    val focusRequester = remember { FocusRequester() }
    val toolCursorModifier = rememberConceptualCanvasToolCursorModifier(conceptualCanvasTool)
    val textMeasurer = rememberTextMeasurer()

    val desktopAwtModifierRemapVerticalScroll = rememberDesktopModifierKeysRemapVerticalScrollToHorizontal()
    val keyboardRemapVerticalScrollPan = isDesktopTarget && desktopAwtModifierRemapVerticalScroll

    var canvasZoom by remember(canvasKey) { mutableFloatStateOf(1f) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(app.canvasTabStripBackground)
    ) {
        CanvasTabStrip(
            tabs = canvasTabs,
            selectedIndex = selectedCanvasTabIndex,
            onSelectTab = onSelectCanvasTab,
            onCloseTab = onRequestCloseCanvasTab,
            app = app,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 2.dp, end = 2.dp, bottom = 2.dp)
                .border(
                    width = if (isDragOver) 3.dp else 1.dp,
                    color = if (isDragOver) model.fileDropOverlayBorder else app.canvasPanelBorderIdle,
                )
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        if (event.isCtrlPressed || event.isMetaPressed) {
                            when {
                                event.key == Key.Z && event.isShiftPressed -> {
                                    onRedoRequest()
                                    return@onPreviewKeyEvent true
                                }
                                event.key == Key.Z && !event.isShiftPressed -> {
                                    onUndoRequest()
                                    return@onPreviewKeyEvent true
                                }
                                event.key == Key.Y -> {
                                    onRedoRequest()
                                    return@onPreviewKeyEvent true
                                }
                                event.key == Key.Zero -> {
                                    canvasZoom = 1f
                                    return@onPreviewKeyEvent true
                                }
                                event.key == Key.Equals || event.key == Key.Plus -> {
                                    canvasZoom = (canvasZoom * 1.12f).coerceAtMost(4f)
                                    return@onPreviewKeyEvent true
                                }
                                event.key == Key.Minus -> {
                                    canvasZoom = (canvasZoom / 1.12f).coerceAtLeast(0.25f)
                                    return@onPreviewKeyEvent true
                                }
                            }
                        }
                        val sch = schema
                        if (sch != null &&
                            (event.isCtrlPressed || event.isMetaPressed) &&
                            event.key == Key.C
                        ) {
                            onCopyRequest()
                            return@onPreviewKeyEvent true
                        }
                        if (sch != null &&
                            (event.isCtrlPressed || event.isMetaPressed) &&
                            event.key == Key.X
                        ) {
                            onCutRequest()
                            return@onPreviewKeyEvent true
                        }
                        if (sch != null &&
                            (event.isCtrlPressed || event.isMetaPressed) &&
                            event.key == Key.V
                        ) {
                            onPasteRequest()
                            return@onPreviewKeyEvent true
                        }
                        if (sch != null &&
                            (event.isCtrlPressed || event.isMetaPressed) &&
                            event.key == Key.A
                        ) {
                            onSelectionChange(canvasSelectionSelectAllElements(sch))
                            return@onPreviewKeyEvent true
                        }
                        if (sch != null &&
                            (event.isCtrlPressed || event.isMetaPressed) &&
                            event.key == Key.F
                        ) {
                            onRequestOpenConceptualFind()
                            return@onPreviewKeyEvent true
                        }
                        if (sch != null &&
                            event.isCtrlPressed &&
                            event.key == Key.O
                        ) {
                            if (canOrganizeAttributesMenuSelection(sch, selection)) {
                                onOrganizeAttributes()
                                return@onPreviewKeyEvent true
                            }
                        }
                        if (sch != null) {
                            val nudged = sch.applyCanvasKeyboardArrow(
                                selection = selection,
                                key = event.key,
                                isCtrlPressed = event.isCtrlPressed,
                                isShiftPressed = event.isShiftPressed,
                                textMeasurer = textMeasurer,
                            )
                            if (nudged != null) {
                                onSchemaCommit(nudged)
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        when {
                            conceptualCanvasTool != ConceptualCanvasTool.None -> {
                                onClearConceptualCanvasTool()
                                true
                            }
                            selection != CanvasSelection.None -> {
                                onSelectionChange(CanvasSelection.None)
                                true
                            }
                            else -> false
                        }
                    } else if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Delete || event.key == Key.Backspace)
                    ) {
                        val currentSchema = schema ?: return@onPreviewKeyEvent false
                        val next = deleteCanvasSelection(currentSchema, selection)
                        if (next != null) {
                            onSchemaCommit(next.withNormalizedAttributeMultiValuedCounts())
                            onSelectionChange(CanvasSelection.None)
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
                .fileDragDropTarget(
                    onDragStateChange = onDragStateChange,
                    onFileDrop = onFileDrop,
                ),
        ) {
            key(canvasKey) {
                SchemaCanvas(
                    schema = schema,
                    selection = selection,
                    onSelectionChange = onSelectionChange,
                    onSchemaPreview = onSchemaPreview,
                    onSchemaCommit = onSchemaCommit,
                    conceptualCanvasTool = conceptualCanvasTool,
                    onConceptualCanvasToolChange = onConceptualCanvasToolChange,
                    onTransientUserMessage = onTransientUserMessage,
                    bulkDeleteUiState = bulkDeleteUiState,
                    onBulkDeleteUiChange = onBulkDeleteUiChange,
                    selectionBandUiState = selectionBandUiState,
                    onSelectionBandUiChange = onSelectionBandUiChange,
                    editorTabSessionId = selectedTab?.id ?: -1L,
                    keyboardRemapVerticalScrollPanToHorizontal = keyboardRemapVerticalScrollPan,
                    zoom = canvasZoom,
                    onZoomChange = { canvasZoom = it },
                    toolCursorModifier = toolCursorModifier,
                    canvasFocusRequester = focusRequester,
                    onViewStateChange = onCanvasViewStateChange,
                    requestCenterOnModelBounds = requestCenterOnModelBounds,
                    onRequestCenterOnModelBoundsConsumed = onRequestCenterOnModelBoundsConsumed,
                    onConceptualInspectorSelectionFieldEditRequest = onConceptualInspectorSelectionFieldEditRequest,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (isDragOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(model.fileDropOverlayFill),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Solte o arquivo para abrir o modelo",
                        fontSize = 16.sp,
                        color = model.fileDropOverlayPrompt,
                    )
                }
            }
        }
    }
}

@Composable
private fun CanvasTabStrip(
    tabs: List<EditorTabSession>,
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit,
    onCloseTab: (Int) -> Unit,
    app: AppColorPalette,
) {
    val modelIcon = painterResource(Res.drawable.modelo_conceitual_2s)

    val density = LocalDensity.current
    val topCornerPx   = with(density) { 5.dp.toPx() }
    val bottomCurvePx = with(density) { 4.dp.toPx() }
    val tabShape = remember(topCornerPx, bottomCurvePx) {
        ChromiumTabShape(
            topCornerRadius = topCornerPx,
            bottomCurveRadius = bottomCurvePx
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CHROMIUM_TAB_STRIP_HEIGHT)
            .background(app.canvasTabStripBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            tabs.forEachIndexed { index, tab ->
                val baseLabel = tab.displayTitle()
                val dirty = tab.hasUnsavedChanges()
                val tabLabel = if (dirty) "$baseLabel*" else baseLabel
                val selected = index == selectedIndex

                if (index > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                ChromiumTab(
                    label = tabLabel,
                    selected = selected,
                    tabShape = tabShape,
                    activeTabBg = app.canvasTabActiveBackground,
                    inactiveTabBg = app.canvasTabInactiveBackground,
                    borderColor = app.canvasTabStripBorder,
                    leadingIcon = modelIcon,
                    onClose = { onCloseTab(index) },
                    modifier = Modifier
                        .widthIn(min = 60.dp, max = 240.dp)
                        .height(if (selected) CHROMIUM_TAB_ACTIVE_HEIGHT else CHROMIUM_TAB_INACTIVE_HEIGHT)
                        .zIndex(if (selected) 2f else 1f),
                    onClick = { onSelectTab(index) },
                )
            }
        }
    }
}
