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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
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
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.expandBulkDeleteClosure
import games.polyclub.power.brmodelo.domain.singleElementDeletionClosure
import games.polyclub.power.brmodelo.ui.ConceptualCanvasTool
import games.polyclub.power.brmodelo.ui.canvas.applyCanvasKeyboardArrow
import games.polyclub.power.brmodelo.ui.canvas.SchemaCanvas
import games.polyclub.power.brmodelo.ui.canvas.rememberConceptualCanvasToolCursorModifier
import games.polyclub.power.brmodelo.ui.components.CHROMIUM_TAB_ACTIVE_HEIGHT
import games.polyclub.power.brmodelo.ui.components.CHROMIUM_TAB_INACTIVE_HEIGHT
import games.polyclub.power.brmodelo.ui.components.CHROMIUM_TAB_STRIP_HEIGHT
import games.polyclub.power.brmodelo.ui.components.ChromiumTab
import games.polyclub.power.brmodelo.ui.components.ChromiumTabShape
import games.polyclub.power.brmodelo.generated.resources.Res
import games.polyclub.power.brmodelo.generated.resources.modelo_conceitual_2s
import org.jetbrains.compose.resources.painterResource

private val DRAG_OVERLAY_BG     = Color(0x882C7BE8)
private val DRAG_OVERLAY_BORDER = Color(0xFF1E5CC7)

private val CANVAS_STRIP_BG        = Color(0xFFD7D7D7)
private val CANVAS_TAB_ACTIVE_BG   = Color(0xFFEEEEEE)
private val CANVAS_TAB_INACTIVE_BG = Color(0xFFC8C8C8)
private val CANVAS_STRIP_BORDER    = Color(0xFF888888)

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
    modifier: Modifier = Modifier,
) {
    val selectedTab = canvasTabs.getOrNull(selectedCanvasTabIndex)
    val canvasKey = selectedTab?.id ?: 0L

    val focusRequester = remember { FocusRequester() }
    val toolCursorModifier = rememberConceptualCanvasToolCursorModifier(conceptualCanvasTool)
    val textMeasurer = rememberTextMeasurer()

    val desktopAwtModifierRemapVerticalScroll = rememberDesktopModifierKeysRemapVerticalScrollToHorizontal()
    val keyboardRemapVerticalScrollPan = isDesktopTarget && desktopAwtModifierRemapVerticalScroll

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFFD7D7D7))
    ) {
        CanvasTabStrip(
            tabs = canvasTabs,
            selectedIndex = selectedCanvasTabIndex,
            onSelectTab = onSelectCanvasTab,
            onCloseTab = onRequestCloseCanvasTab,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 2.dp, end = 2.dp, bottom = 2.dp)
                .border(
                    width = if (isDragOver) 3.dp else 1.dp,
                    color = if (isDragOver) DRAG_OVERLAY_BORDER else Color(0xFF7A7A7A),
                )
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        val sch = schema
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
                        val sch = schema
                        val multi = selection as? CanvasSelection.Multiple
                        if (sch != null && multi != null && multi.elementIds.isNotEmpty()) {
                            val ids = expandBulkDeleteClosure(sch, multi.elementIds)
                            if (ids.isNotEmpty()) {
                                onSchemaCommit(
                                    sch.withoutElements(ids).withNormalizedAttributeMultiValuedCounts(),
                                )
                                onSelectionChange(CanvasSelection.None)
                                true
                            } else {
                                false
                            }
                        } else {
                            val elemId = (selection as? CanvasSelection.Element)?.id
                            if (sch != null && elemId != null) {
                                val ids = singleElementDeletionClosure(sch, elemId)
                                if (ids.isNotEmpty()) {
                                    onSchemaCommit(
                                        sch.withoutElements(ids).withNormalizedAttributeMultiValuedCounts(),
                                    )
                                    onSelectionChange(CanvasSelection.None)
                                    true
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
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
                    toolCursorModifier = toolCursorModifier,
                    canvasFocusRequester = focusRequester,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (isDragOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DRAG_OVERLAY_BG),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Solte o arquivo para abrir o modelo",
                        fontSize = 16.sp,
                        color = Color.White,
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
            .background(CANVAS_STRIP_BG),
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
                    activeTabBg = CANVAS_TAB_ACTIVE_BG,
                    inactiveTabBg = CANVAS_TAB_INACTIVE_BG,
                    borderColor = CANVAS_STRIP_BORDER,
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
