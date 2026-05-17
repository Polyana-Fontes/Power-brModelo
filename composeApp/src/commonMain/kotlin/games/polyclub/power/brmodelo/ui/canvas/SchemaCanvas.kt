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

package games.polyclub.power.brmodelo.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import games.polyclub.power.brmodelo.domain.ConceptualBulkDeleteBand
import games.polyclub.power.brmodelo.domain.bulkDeleteCategoryCounts
import games.polyclub.power.brmodelo.domain.bulkDeleteResolvedIds
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import games.polyclub.power.brmodelo.domain.applyConceptualAttributeTool
import games.polyclub.power.brmodelo.domain.applyConceptualSpecializationTool
import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.mergeCanvasBandPick
import games.polyclub.power.brmodelo.domain.toggleCardinalityInMultiSelection
import games.polyclub.power.brmodelo.domain.toggleElementInMultiSelection
import games.polyclub.power.brmodelo.domain.toMultiPickSets
import games.polyclub.power.brmodelo.domain.ConceptualLinkValidationResult
import games.polyclub.power.brmodelo.domain.ConceptualAttributeToolResult
import games.polyclub.power.brmodelo.domain.ConceptualAttributeToolVariant
import games.polyclub.power.brmodelo.domain.ConceptualLinkPick
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ConceptualSpecializationToolResult
import games.polyclub.power.brmodelo.domain.SchemaElement
import games.polyclub.power.brmodelo.domain.ConceptualSpecializationToolVariant
import games.polyclub.power.brmodelo.domain.hiddenAttributesTooltipText
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.organizeAttributesOnOwnerSide
import games.polyclub.power.brmodelo.domain.relayoutCompositeSubtree
import games.polyclub.power.brmodelo.domain.placeConceptualItem
import games.polyclub.power.brmodelo.domain.validateAndBuildConceptualLink
import games.polyclub.power.brmodelo.ui.BulkDeleteUiState
import games.polyclub.power.brmodelo.ui.ConceptualCanvasTool
import games.polyclub.power.brmodelo.ui.InspectorSelectionFieldKeys
import games.polyclub.power.brmodelo.ui.SelectionBandUiState
import games.polyclub.power.brmodelo.ui.canvasPointerScrollPanGain
import games.polyclub.power.brmodelo.ui.invertCanvasPointerScrollPan
import games.polyclub.power.brmodelo.ui.isDesktopTarget
import games.polyclub.power.brmodelo.ui.toPlacementKindOrNull
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Latest canvas layout / pan / pointer state for paste anchoring and cross-tab behaviour. */
internal data class SchemaCanvasViewState(
    val layoutWidthPx: Float = 0f,
    val layoutHeightPx: Float = 0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    /** View scale applied to model coordinates (1 = 100%). */
    val zoom: Float = 1f,
    val pointerViewX: Float? = null,
    val pointerViewY: Float? = null,
    val isPointerOverCanvas: Boolean = false,
) {
    fun pointerModelX(): Float? = pointerViewX?.let { (it - panX) / zoom.coerceAtLeast(1e-4f) }
    fun pointerModelY(): Float? = pointerViewY?.let { (it - panY) / zoom.coerceAtLeast(1e-4f) }
}

// Background colour of the canvas (light grey, matching the original brModelo canvas background)
private val CANVAS_BG = Color(0xFFE8E8E8)
// Dot-grid colour (subtle)
private val GRID_DOT = Color(0xFFCCCCCC)
private const val GRID_STEP = 20f

private const val CANVAS_ZOOM_MIN = 0.25f
private const val CANVAS_ZOOM_MAX = 4f
private const val CANVAS_ZOOM_KEYBOARD_STEP = 1.12f
private const val CANVAS_ZOOM_WHEEL_FACTOR = 1.09f

/** Converts a point in the canvas view to conceptual model coordinates ([pan] + model×[zoom]). */
internal fun viewOffsetToModel(view: Offset, pan: Offset, zoom: Float): Offset {
    val z = zoom.coerceAtLeast(1e-4f)
    return Offset((view.x - pan.x) / z, (view.y - pan.y) / z)
}

private fun panKeepingModelUnderViewPoint(
    viewFocus: Offset,
    pan: Offset,
    oldZoom: Float,
    newZoom: Float,
): Offset {
    val z0 = oldZoom.coerceAtLeast(1e-4f)
    val z1 = newZoom.coerceAtLeast(1e-4f)
    val mx = (viewFocus.x - pan.x) / z0
    val my = (viewFocus.y - pan.y) / z0
    return Offset(viewFocus.x - mx * z1, viewFocus.y - my * z1)
}
private val BULK_BAND_FILL = Color(0x40FF3B3B)
private val BULK_BAND_STROKE = Color(0xFFCC0000)
private val SELECTION_BAND_FILL = Color(0x402E7DFF)
private val SELECTION_BAND_STROKE = Color(0xFF0060C0)

/**
 * Interactive canvas that renders a [games.polyclub.power.brmodelo.domain.ConceptualSchema] using Compose [Canvas].
 *
 * Supports:
 * - **Pan**: dragging empty canvas moves the view; **wheel / trackpad scroll** always pans (independent of
 *   the active tool). **Two-finger touch** pans by centroid. On **desktop**, if the stack only delivers
 *   vertical scroll, hold **Shift** and scroll to pan horizontally (Shift is read from AWT via
 *   [games.polyclub.power.brmodelo.ui.rememberDesktopModifierKeysRemapVerticalScrollToHorizontal] when pointer
 *   events omit modifiers). **Middle-button drag** always pans (same as dragging empty canvas with the left button).
 *   With **Excluir objetos** armed, **right-button drag** also pans.
 *   **Wasm** applies [games.polyclub.power.brmodelo.ui.invertCanvasPointerScrollPan] so pan matches finger direction.
 * - **Hover**: when the pointer is over a diagram element that has [games.polyclub.power.brmodelo.domain.SchemaElement.hiddenAttributes],
 *   a balloon lists **Atributos Ocultos:** and a monospace tree of oculto names (hit uses element bounds under the pointer,
 *   same order as the inspector: composite [HiddenAttribute.children] then [HiddenAttribute.nestedHiddenAttributes]).
 * - **Move**: drag a selected element (or any element) to reposition it.
 * - **Move cardinality label**: drag a selected cardinality label.
 * - **Resize**: drag a corner handle of the selected element to resize it.
 *
 * Selection follows the original Pascal behaviour: clicking on an element
 * selects it immediately on pointer-down (not pointer-up), so the user can
 * start dragging the newly-selected element in the same gesture.
 *
 * @param schema              The model to render, or null for an empty canvas.
 * @param selection           The currently selected object.
 * @param onSelectionChange   Called when the selection should change.
 * @param onSchemaPreview     Called with intermediate schema states during drag (no undo entry).
 * @param onSchemaCommit      Called when a drag or resize is committed (creates undo entry).
 * @param conceptualCanvasTool When set to an entity placement variant, a tap on empty canvas
 *                             inserts an element ([games.polyclub.power.brmodelo.domain.placeConceptualItem])
 *                             with incremental names and default geometry from [games.polyclub.power.brmodelo.domain.ConceptualPlacementDefaults].
 * @param onConceptualCanvasToolChange Updates the active canvas tool ([ConceptualCanvasTool.LinkObjects],
 *                             [ConceptualCanvasTool.AutoSelfRelationship]).
 * @param onTransientUserMessage Short user feedback (e.g. invalid link).
 * @param toolCursorModifier Optional pointer icon (entity / link tools) applied to the drawing surface.
 * @param canvasFocusRequester When set, receives focus on pointer down so parent shortcuts (e.g. Escape) apply after interacting with the canvas.
 * @param bulkDeleteUiState Rubber-band preview (view rect + marked ids); drawn on top of the canvas.
 * @param onBulkDeleteUiChange Updates [bulkDeleteUiState] while dragging with [ConceptualCanvasTool.BulkDeleteObjects].
 * @param selectionBandUiState Blue rubber-band preview for [ConceptualCanvasTool.RectangleSelection].
 * @param onSelectionBandUiChange Updates [selectionBandUiState] during that gesture.
 * @param editorTabSessionId [games.polyclub.power.brmodelo.ui.EditorTabSession.id] for the canvas tab (for "Ligar objetos" second-click validation).
 * @param keyboardRemapVerticalScrollPanToHorizontal Desktop only: when true, vertical scroll maps to horizontal pan;
 *   fed from AWT (Shift only).
 * @param zoom View scale for model coordinates (1 = 100%). Updated by pinch and Ctrl+wheel; keyboard shortcuts
 *   are handled in [games.polyclub.power.brmodelo.ui.MainCanvasPanel] and passed via [onZoomChange].
 * @param onZoomChange Notifies parent when [zoom] should change (pinch / Ctrl+scroll).
 * @param onViewStateChange Optional hook for layout, pan, and hover pointer (view coordinates) —
 *   used to anchor clipboard paste in model space.
 * @param requestCenterOnModelBounds When non-null, recentres the viewport on these model-space bounds
 *   (after layout size is known); then [onRequestCenterOnModelBoundsConsumed] runs so the parent can clear the request.
 * @param onConceptualInspectorSelectionFieldEditRequest When the user double-clicks a diagram element or a
 *   cardinality label while [conceptualCanvasTool] is [ConceptualCanvasTool.None], delivers an inspector grid key
 *   ([InspectorSelectionFieldKeys]) so the host can open the **Seleção** tab and start inline edit there.
 */
@Composable
internal fun SchemaCanvas(
    schema: ConceptualSchema?,
    selection: CanvasSelection = CanvasSelection.None,
    onSelectionChange: (CanvasSelection) -> Unit = {},
    onSchemaPreview: (ConceptualSchema) -> Unit = {},
    onSchemaCommit: (ConceptualSchema) -> Unit = {},
    conceptualCanvasTool: ConceptualCanvasTool = ConceptualCanvasTool.None,
    onConceptualCanvasToolChange: (ConceptualCanvasTool) -> Unit = {},
    onTransientUserMessage: (String) -> Unit = {},
    bulkDeleteUiState: BulkDeleteUiState? = null,
    onBulkDeleteUiChange: (BulkDeleteUiState?) -> Unit = {},
    selectionBandUiState: SelectionBandUiState? = null,
    onSelectionBandUiChange: (SelectionBandUiState?) -> Unit = {},
    editorTabSessionId: Long = -1L,
    keyboardRemapVerticalScrollPanToHorizontal: Boolean = false,
    zoom: Float = 1f,
    onZoomChange: (Float) -> Unit = {},
    toolCursorModifier: Modifier = Modifier,
    canvasFocusRequester: FocusRequester? = null,
    onViewStateChange: ((SchemaCanvasViewState) -> Unit)? = null,
    requestCenterOnModelBounds: ElementPosition? = null,
    onRequestCenterOnModelBoundsConsumed: () -> Unit = {},
    onConceptualInspectorSelectionFieldEditRequest: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var panOffset by remember { mutableStateOf(Offset(8f, 8f)) }
    var layoutSize by remember { mutableStateOf(Size.Zero) }
    var pointerView by remember { mutableStateOf<Offset?>(null) }
    var pointerOverCanvas by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()
    val layoutDirection = LocalLayoutDirection.current
    var hiddenAttributesTooltipAnchor by remember { mutableStateOf<Pair<Offset, String>?>(null) }
    var linkToolHoverPick by remember { mutableStateOf<ConceptualLinkPick?>(null) }
    val hoverSchemaForTooltip by rememberUpdatedState(schema)
    val hoverPanForTooltip by rememberUpdatedState(panOffset)
    val hoverZoomForTooltip by rememberUpdatedState(zoom)

    // rememberUpdatedState lets the gesture handler always see the latest values
    // without restarting the gesture on every recomposition.
    val currentSchema by rememberUpdatedState(schema)
    val currentSelection by rememberUpdatedState(selection)
    val currentOnSelectionChange by rememberUpdatedState(onSelectionChange)
    val currentOnSchemaPreview by rememberUpdatedState(onSchemaPreview)
    val currentOnSchemaCommit by rememberUpdatedState(onSchemaCommit)
    val currentPanOffset by rememberUpdatedState(panOffset)
    val currentLayoutSize by rememberUpdatedState(layoutSize)
    val currentCanvasFocusRequester by rememberUpdatedState(canvasFocusRequester)
    val currentConceptualTool by rememberUpdatedState(conceptualCanvasTool)
    val currentOnConceptualCanvasToolChange by rememberUpdatedState(onConceptualCanvasToolChange)
    val currentOnTransientUserMessage by rememberUpdatedState(onTransientUserMessage)
    val currentOnBulkDeleteUiChange by rememberUpdatedState(onBulkDeleteUiChange)
    val currentOnSelectionBandUiChange by rememberUpdatedState(onSelectionBandUiChange)
    val currentTextMeasurer by rememberUpdatedState(textMeasurer)
    val currentLayoutDirection by rememberUpdatedState(layoutDirection)
    val currentEditorTabSessionId by rememberUpdatedState(editorTabSessionId)
    val currentKeyboardRemapVerticalScrollPan by rememberUpdatedState(keyboardRemapVerticalScrollPanToHorizontal)
    val currentZoom by rememberUpdatedState(zoom)
    val onZoomChangeCb by rememberUpdatedState(onZoomChange)
    val onViewStateChangeCb by rememberUpdatedState(onViewStateChange)
    val onCenterBoundsConsumed by rememberUpdatedState(onRequestCenterOnModelBoundsConsumed)
    val onConceptualInspectorSelectionFieldEditCb by rememberUpdatedState(onConceptualInspectorSelectionFieldEditRequest)
    val selectionDoubleClickMemo = remember { ConceptualSelectionDoubleClickMemo() }

    LaunchedEffect(conceptualCanvasTool) {
        if (conceptualCanvasTool !is ConceptualCanvasTool.LinkObjects) {
            linkToolHoverPick = null
        }
    }

    LaunchedEffect(requestCenterOnModelBounds, layoutSize.width, layoutSize.height, zoom) {
        val bounds = requestCenterOnModelBounds ?: return@LaunchedEffect
        val w = layoutSize.width
        val h = layoutSize.height
        if (w <= 0f || h <= 0f) return@LaunchedEffect
        val b = bounds.coercedToMinimumDimensions()
        val cx = b.x + b.width * 0.5f
        val cy = b.y + b.height * 0.5f
        val z = zoom.coerceAtLeast(1e-4f)
        panOffset = Offset(w * 0.5f - cx * z, h * 0.5f - cy * z)
        onCenterBoundsConsumed()
    }

    /** Pushes pointer/layout to the parent immediately (avoids one-frame lag vs [SideEffect] only). */
    fun pushClipboardViewStateToParent(pointerLocal: Offset?, over: Boolean) {
        onViewStateChangeCb?.invoke(
            SchemaCanvasViewState(
                layoutWidthPx = currentLayoutSize.width,
                layoutHeightPx = currentLayoutSize.height,
                panX = currentPanOffset.x,
                panY = currentPanOffset.y,
                zoom = currentZoom,
                pointerViewX = pointerLocal?.x,
                pointerViewY = pointerLocal?.y,
                isPointerOverCanvas = over,
            ),
        )
    }

    SideEffect {
        onViewStateChangeCb?.invoke(
            SchemaCanvasViewState(
                layoutWidthPx = layoutSize.width,
                layoutHeightPx = layoutSize.height,
                panX = panOffset.x,
                panY = panOffset.y,
                zoom = zoom,
                pointerViewX = pointerView?.x,
                pointerViewY = pointerView?.y,
                isPointerOverCanvas = pointerOverCanvas,
            ),
        )
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                layoutSize = Size(
                    coords.size.width.toFloat(),
                    coords.size.height.toFloat(),
                )
            }
            .then(toolCursorModifier)
            .clipToBounds()
            .background(CANVAS_BG)
            // This block is the OUTER pointerInput so scroll/multitouch runs without starving the inner
            // gesture detector (middle/right-button pan uses [awaitCanvasGestureFirstDown] in the inner block).
            .pointerInput(
                invertCanvasPointerScrollPan,
                canvasPointerScrollPanGain,
                currentZoom,
                currentPanOffset,
            ) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()

                        val pressedForPan = event.changes.filter { it.pressed }
                        if (pressedForPan.size >= 2) {
                            if (currentConceptualTool is ConceptualCanvasTool.BulkDeleteObjects) {
                                currentOnBulkDeleteUiChange(null)
                            }
                            currentOnSelectionBandUiChange(null)
                            val pr0 = pressedForPan.take(2)
                            val p0a = pr0[0].position
                            val p0b = pr0[1].position
                            val dist0 = (p0b - p0a).getDistance().coerceAtLeast(1f)
                            val cent0 = centroidOfOffsets(listOf(p0a, p0b))
                            val pan0 = currentPanOffset
                            val zoom0 = currentZoom
                            event.changes.forEach { it.consume() }
                            while (true) {
                                val inner = awaitPointerEvent()
                                val pr = inner.changes.filter { it.pressed }
                                if (pr.size < 2) break
                                val p1a = pr[0].position
                                val p1b = pr[1].position
                                val dist = (p1b - p1a).getDistance().coerceAtLeast(1f)
                                val cent = centroidOfOffsets(listOf(p1a, p1b))
                                val newZoom = (zoom0 * dist / dist0).coerceIn(CANVAS_ZOOM_MIN, CANVAS_ZOOM_MAX)
                                val model = viewOffsetToModel(cent0, pan0, zoom0)
                                panOffset = Offset(cent.x - model.x * newZoom, cent.y - model.y * newZoom)
                                onZoomChangeCb(newZoom)
                                inner.changes.forEach { it.consume() }
                            }
                            continue
                        }

                        if (event.type == PointerEventType.Scroll) {
                            var rawScroll = Offset.Zero
                            for (change in event.changes) {
                                rawScroll += change.scrollDelta
                            }
                            if (rawScroll != Offset.Zero) {
                                val ctrlZoom =
                                    event.keyboardModifiers.isCtrlPressed ||
                                        event.keyboardModifiers.isMetaPressed
                                if (ctrlZoom) {
                                    var wheel = rawScroll.y
                                    if (abs(rawScroll.x) > abs(rawScroll.y)) {
                                        wheel = rawScroll.x
                                    }
                                    if (invertCanvasPointerScrollPan) {
                                        wheel = -wheel
                                    }
                                    val factor = if (wheel < 0f) {
                                        CANVAS_ZOOM_WHEEL_FACTOR
                                    } else {
                                        1f / CANVAS_ZOOM_WHEEL_FACTOR
                                    }
                                    val newZ = (currentZoom * factor).coerceIn(CANVAS_ZOOM_MIN, CANVAS_ZOOM_MAX)
                                    if (abs(newZ - currentZoom) > 1e-4f) {
                                        val focal = centroidOfOffsets(event.changes.map { it.position })
                                        panOffset = panKeepingModelUnderViewPoint(
                                            focal,
                                            currentPanOffset,
                                            currentZoom,
                                            newZ,
                                        )
                                        onZoomChangeCb(newZ)
                                    }
                                    event.changes.forEach { ch -> ch.consume() }
                                } else {
                                    val panDelta = scrollDeltaForCanvasPan(
                                        rawScroll,
                                        pointerShiftPressed = event.keyboardModifiers.isShiftPressed,
                                        keyboardRemapVerticalToHorizontal = currentKeyboardRemapVerticalScrollPan,
                                    )
                                    if (panDelta != Offset.Zero) {
                                        panOffset += panDelta
                                        event.changes.forEach { ch -> ch.consume() }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Re-evaluate on every pointer event while waiting for a down (see [conceptualToolAllowsRightButtonCanvasPan]).
                    val down = awaitCanvasGestureFirstDown(
                        bulkDeleteAllowsSecondaryPan = {
                            conceptualToolAllowsRightButtonCanvasPan(currentConceptualTool)
                        },
                    )
                    currentCanvasFocusRequester?.requestFocus()

                    if (currentEvent.buttons.isTertiaryPressed) {
                        val panAtStart = currentPanOffset
                        val startPointer = down.position
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            panOffset = panAtStart + (change.position - startPointer)
                        }
                        return@awaitEachGesture
                    }

                    if (conceptualToolAllowsRightButtonCanvasPan(currentConceptualTool) &&
                        currentEvent.buttons.isSecondaryPressed
                    ) {
                        val panAtStart = currentPanOffset
                        val startPointer = down.position
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            panOffset = panAtStart + (change.position - startPointer)
                        }
                        return@awaitEachGesture
                    }

                    val panAtGestureStart = currentPanOffset
                    val zoomAtGestureStart = currentZoom
                    val schemaAtGestureStart = currentSchema
                    val selAtGestureStart = currentSelection

                    val bulkDeleteTool = currentConceptualTool as? ConceptualCanvasTool.BulkDeleteObjects
                    if (bulkDeleteTool != null && schemaAtGestureStart != null && currentEvent.buttons.isPrimaryPressed) {
                        val startPointer = down.position
                        val slop = viewConfiguration.touchSlop
                        var isDraggingBand = false
                        var lastPointer = startPointer
                        val onUi = currentOnBulkDeleteUiChange
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressedChanges = event.changes.filter { it.pressed }
                            if (pressedChanges.size >= 2) {
                                onUi(null)
                                currentOnSelectionBandUiChange(null)
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                lastPointer = change.position
                                if (!change.pressed) {
                                    if (isDraggingBand) {
                                        val band = conceptualBulkDeleteBandFromViewDiagonal(
                                            startPointer,
                                            lastPointer,
                                            panAtGestureStart,
                                            zoomAtGestureStart,
                                        )
                                        val ids = bulkDeleteResolvedIds(schemaAtGestureStart, band)
                                        if (ids.isNotEmpty()) {
                                            val next = schemaAtGestureStart.withoutElements(ids)
                                                .withNormalizedAttributeMultiValuedCounts()
                                            currentOnSchemaCommit(next)
                                            currentOnSelectionChange(CanvasSelection.None)
                                            currentOnConceptualCanvasToolChange(ConceptualCanvasTool.None)
                                        }
                                    }
                                    onUi(null)
                                    break
                                }
                                continue
                            }
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            lastPointer = change.position
                            if (!change.pressed) {
                                if (isDraggingBand) {
                                    val band = conceptualBulkDeleteBandFromViewDiagonal(
                                        startPointer,
                                        lastPointer,
                                        panAtGestureStart,
                                        zoomAtGestureStart,
                                    )
                                    val ids = bulkDeleteResolvedIds(schemaAtGestureStart, band)
                                    if (ids.isNotEmpty()) {
                                        val next = schemaAtGestureStart.withoutElements(ids)
                                            .withNormalizedAttributeMultiValuedCounts()
                                        currentOnSchemaCommit(next)
                                        currentOnSelectionChange(CanvasSelection.None)
                                        currentOnConceptualCanvasToolChange(ConceptualCanvasTool.None)
                                    }
                                }
                                onUi(null)
                                break
                            }
                            val dragVec = lastPointer - startPointer
                            if (!isDraggingBand && dragVec.getDistance() > slop) {
                                isDraggingBand = true
                            }
                            if (isDraggingBand) {
                                change.consume()
                                val band = conceptualBulkDeleteBandFromViewDiagonal(
                                    startPointer,
                                    lastPointer,
                                    panAtGestureStart,
                                    zoomAtGestureStart,
                                )
                                val marked = bulkDeleteResolvedIds(schemaAtGestureStart, band)
                                val counts = bulkDeleteCategoryCounts(schemaAtGestureStart, marked)
                                val viewRect = normalizedBulkDeleteViewRect(startPointer, lastPointer)
                                onUi(BulkDeleteUiState(viewRect, marked, counts))
                            }
                        }
                        return@awaitEachGesture
                    }

                    val rectangleTool = currentConceptualTool is ConceptualCanvasTool.RectangleSelection
                    val shiftHeldOnPrimary = currentEvent.keyboardModifiers.isShiftPressed
                    if (schemaAtGestureStart != null &&
                        currentEvent.buttons.isPrimaryPressed &&
                        rectangleTool
                    ) {
                        runRectangleSelectionGesture(
                            down = down,
                            panAtGestureStart = panAtGestureStart,
                            zoomAtGestureStart = zoomAtGestureStart,
                            schema = schemaAtGestureStart,
                            selectionAtStart = selAtGestureStart,
                            additive = shiftHeldOnPrimary,
                            slop = viewConfiguration.touchSlop,
                            textMeasurer = currentTextMeasurer,
                            onBandUi = currentOnSelectionBandUiChange,
                            onSelectionChange = currentOnSelectionChange,
                        )
                        return@awaitEachGesture
                    }

                    if (schemaAtGestureStart != null &&
                        currentConceptualTool is ConceptualCanvasTool.None &&
                        currentEvent.keyboardModifiers.isShiftPressed &&
                        currentEvent.buttons.isPrimaryPressed
                    ) {
                        runRectangleSelectionGesture(
                            down = down,
                            panAtGestureStart = panAtGestureStart,
                            zoomAtGestureStart = zoomAtGestureStart,
                            schema = schemaAtGestureStart,
                            selectionAtStart = selAtGestureStart,
                            additive = true,
                            slop = viewConfiguration.touchSlop,
                            textMeasurer = currentTextMeasurer,
                            onBandUi = currentOnSelectionBandUiChange,
                            onSelectionChange = currentOnSelectionChange,
                        )
                        return@awaitEachGesture
                    }

                    val schemaPoint = viewOffsetToModel(down.position, panAtGestureStart, zoomAtGestureStart)
                    val shiftHeldAtGestureStart = currentEvent.keyboardModifiers.isShiftPressed

                    val autoRelTool = currentConceptualTool as? ConceptualCanvasTool.AutoSelfRelationship
                    if (autoRelTool != null && schemaAtGestureStart != null) {
                        val startPointer = down.position
                        val slop = viewConfiguration.touchSlop
                        var totalDrag = Offset.Zero
                        var isDragging = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!isDragging) {
                                    processAutoSelfRelationshipTap(
                                        schema = schemaAtGestureStart,
                                        schemaPoint = schemaPoint,
                                        textMeasurer = currentTextMeasurer,
                                        onMessage = currentOnTransientUserMessage,
                                        onSchemaCommit = currentOnSchemaCommit,
                                        onSelectionChange = currentOnSelectionChange,
                                    )
                                }
                                break
                            }
                            totalDrag = change.position - startPointer
                            if (!isDragging && totalDrag.getDistance() > slop) {
                                isDragging = true
                            }
                            if (isDragging) {
                                change.consume()
                                panOffset = panAtGestureStart + totalDrag
                            }
                        }
                        return@awaitEachGesture
                    }

                    val specializationTool = currentConceptualTool as? ConceptualCanvasTool.Specialization
                    if (specializationTool != null && schemaAtGestureStart != null) {
                        val startPointer = down.position
                        val slop = viewConfiguration.touchSlop
                        var totalDrag = Offset.Zero
                        var isDragging = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!isDragging) {
                                    processSpecializationToolTap(
                                        schema = schemaAtGestureStart,
                                        schemaPoint = schemaPoint,
                                        variant = specializationTool.variant,
                                        onMessage = currentOnTransientUserMessage,
                                        onSchemaCommit = currentOnSchemaCommit,
                                        onSelectionChange = currentOnSelectionChange,
                                    )
                                }
                                break
                            }
                            totalDrag = change.position - startPointer
                            if (!isDragging && totalDrag.getDistance() > slop) {
                                isDragging = true
                            }
                            if (isDragging) {
                                change.consume()
                                panOffset = panAtGestureStart + totalDrag
                            }
                        }
                        return@awaitEachGesture
                    }

                    val linkTool = currentConceptualTool as? ConceptualCanvasTool.LinkObjects
                    if (linkTool != null && schemaAtGestureStart != null) {
                        val startPointer = down.position
                        val slop = viewConfiguration.touchSlop
                        var totalDrag = Offset.Zero
                        var isDragging = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!isDragging) {
                                    processLinkObjectsTap(
                                        schema = schemaAtGestureStart,
                                        schemaPoint = schemaPoint,
                                        toolState = linkTool,
                                        editorTabSessionId = currentEditorTabSessionId,
                                        textMeasurer = currentTextMeasurer,
                                        onToolChange = currentOnConceptualCanvasToolChange,
                                        onMessage = currentOnTransientUserMessage,
                                        onSchemaCommit = currentOnSchemaCommit,
                                        onSelectionChange = currentOnSelectionChange,
                                    )
                                }
                                break
                            }
                            totalDrag = change.position - startPointer
                            if (!isDragging && totalDrag.getDistance() > slop) {
                                isDragging = true
                            }
                            if (isDragging) {
                                change.consume()
                                panOffset = panAtGestureStart + totalDrag
                            }
                        }
                        return@awaitEachGesture
                    }

                    val attributeTool = currentConceptualTool as? ConceptualCanvasTool.Attribute
                    if (attributeTool != null && schemaAtGestureStart != null) {
                        val startPointer = down.position
                        val slop = viewConfiguration.touchSlop
                        var totalDrag = Offset.Zero
                        var isDragging = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!isDragging) {
                                    processAttributeToolTap(
                                        schema = schemaAtGestureStart,
                                        schemaPoint = schemaPoint,
                                        variant = attributeTool.variant,
                                        textMeasurer = currentTextMeasurer,
                                        layoutDirection = currentLayoutDirection,
                                        onMessage = currentOnTransientUserMessage,
                                        onSchemaCommit = currentOnSchemaCommit,
                                        onSelectionChange = currentOnSelectionChange,
                                    )
                                }
                                break
                            }
                            totalDrag = change.position - startPointer
                            if (!isDragging && totalDrag.getDistance() > slop) {
                                isDragging = true
                            }
                            if (isDragging) {
                                change.consume()
                                panOffset = panAtGestureStart + totalDrag
                            }
                        }
                        return@awaitEachGesture
                    }

                    // Determine what is under the pointer.
                    val hitResult = schemaAtGestureStart?.let {
                        hitTest(
                            it,
                            schemaPoint,
                            currentTextMeasurer,
                        )
                    }
                        ?: CanvasSelection.None

                    // Check if the pointer is on a resize handle of the currently selected element
                    // or of the cardinality label (manual size).
                    val selectedElem: SchemaElement? = when (val sel = selAtGestureStart) {
                        is CanvasSelection.Element ->
                            schemaAtGestureStart?.elements?.get(sel.id)
                        is CanvasSelection.Multiple -> {
                            val id = (hitResult as? CanvasSelection.Element)?.id
                            if (id != null && id in sel.elementIds) {
                                schemaAtGestureStart?.elements?.get(id)
                            } else {
                                null
                            }
                        }
                        else -> null
                    }
                    val cardinalityConn = when (val sel = selAtGestureStart) {
                        is CanvasSelection.Cardinality ->
                            schemaAtGestureStart?.connections?.firstOrNull { it.id == sel.connectionId }
                        is CanvasSelection.Multiple -> {
                            val cid = (hitResult as? CanvasSelection.Cardinality)?.connectionId
                            if (cid != null && cid in sel.cardinalityConnectionIds) {
                                schemaAtGestureStart?.connections?.firstOrNull { it.id == cid }
                            } else {
                                null
                            }
                        }
                        else -> null
                    }
                    val cardinalityResizeBox =
                        if (cardinalityConn != null &&
                            !cardinalityConn.cardinalityAutoSize &&
                            cardinalityConn.showCardinality &&
                            cardinalityConn.cardinality != null &&
                            schemaAtGestureStart != null
                        ) {
                            cardinalityLabelHighlightElementPosition(
                                schemaAtGestureStart,
                                cardinalityConn,
                                currentTextMeasurer,
                            )
                        } else {
                            null
                        }
                    val hitHandleElem = selectedElem
                        ?.takeUnless { it is SchemaElement.Attribute && it.autoSize }
                        ?.let { getResizeHandleAt(it.position, schemaPoint) }
                    val hitHandleCard = cardinalityResizeBox?.let {
                        getResizeHandleAt(it, schemaPoint)
                    }
                    val hitHandle = hitHandleElem ?: hitHandleCard
                    val gestureStartedOnResizeHandle = hitHandle != null

                    // Pascal behaviour: select immediately on pointer-down, not on pointer-up.
                    // Only skip if we're about to resize (the selection stays as-is).
                    if (hitHandle == null) {
                        val shouldReplaceSelection = when {
                            hitResult == CanvasSelection.None -> true
                            selAtGestureStart is CanvasSelection.Multiple &&
                                hitResult is CanvasSelection.Element ->
                                hitResult.id !in selAtGestureStart.elementIds
                            selAtGestureStart is CanvasSelection.Multiple &&
                                hitResult is CanvasSelection.Cardinality ->
                                hitResult.connectionId !in selAtGestureStart.cardinalityConnectionIds
                            else -> hitResult != selAtGestureStart
                        }
                        if (shouldReplaceSelection) {
                            currentOnSelectionChange(hitResult)
                        }
                    }

                    // Snapshot the element/connection to be dragged at gesture start,
                    // so we can apply absolute deltas instead of cumulative per-frame ones.
                    val resizeCardinalityConnId =
                        if (hitHandleCard != null) cardinalityConn?.id else null
                    val dragElementId = (hitResult as? CanvasSelection.Element)?.id
                        ?: (hitHandleElem?.let {
                            when (val sel = selAtGestureStart) {
                                is CanvasSelection.Element -> sel.id
                                is CanvasSelection.Multiple -> selectedElem?.id
                                else -> null
                            }
                        })
                    val dragConnectionId =
                        if (resizeCardinalityConnId != null) {
                            null
                        } else {
                            (hitResult as? CanvasSelection.Cardinality)?.connectionId
                        }

                    val startElementPos: ElementPosition? =
                        dragElementId?.let { schemaAtGestureStart?.elements?.get(it)?.position }
                    val startCardinalityResizePos: ElementPosition? =
                        resizeCardinalityConnId?.let { id ->
                            val s = schemaAtGestureStart ?: return@let null
                            val c = s.connections.firstOrNull { it.id == id } ?: return@let null
                            cardinalityLabelHighlightElementPosition(s, c, currentTextMeasurer)
                        }
                    val startCardPos: ElementPosition? =
                        dragConnectionId?.let { id ->
                            val s = schemaAtGestureStart ?: return@let null
                            val conn = s.connections.firstOrNull { it.id == id } ?: return@let null
                            conn.cardinalityPosition
                                ?: cardinalityLabelInteractionRect(s, conn, currentTextMeasurer)?.let { r ->
                                    ElementPosition(
                                        x = r.left.toInt(),
                                        y = r.top.toInt(),
                                        width = r.width.toInt().coerceAtLeast(1),
                                        height = r.height.toInt().coerceAtLeast(1),
                                    )
                                }
                        }

                    /** When moving an element (not a resize handle), every selected element moves by the same delta. */
                    val multiElementDragSnapshot: Pair<Set<Int>, Map<Int, ElementPosition>>? =
                        if (dragElementId != null && hitHandle == null && schemaAtGestureStart != null) {
                            val ids = when (val sel = selAtGestureStart) {
                                is CanvasSelection.Multiple -> sel.elementIds
                                is CanvasSelection.Element -> setOf(sel.id)
                                else -> null
                            }
                            if (ids != null && dragElementId in ids) {
                                val posMap = ids.mapNotNull { id ->
                                    schemaAtGestureStart.elements[id]?.let { id to it.position }
                                }.toMap()
                                if (posMap.size == ids.size) ids to posMap else null
                            } else {
                                null
                            }
                        } else {
                            null
                        }

                    val startPointer = down.position
                    val slop = viewConfiguration.touchSlop
                    var totalDrag = Offset.Zero
                    var isDragging = false
                    /** True only when this gesture called [onSchemaPreview] (move/resize/cardinality), not canvas pan. */
                    var didMutateSchemaDuringDrag = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            // Pointer up: commit only if the drag changed the model (not pure canvas pan).
                            if (isDragging && didMutateSchemaDuringDrag) {
                                selectionDoubleClickMemo.clear()
                                val finalSchema = currentSchema
                                if (finalSchema != null) {
                                    currentOnSchemaCommit(finalSchema)
                                }
                            } else if (hitResult == CanvasSelection.None) {
                                selectionDoubleClickMemo.clear()
                                val placementKind = currentConceptualTool.toPlacementKindOrNull()
                                val baseSchema = schemaAtGestureStart
                                if (placementKind != null && baseSchema != null) {
                                    val topLeftX = schemaPoint.x.toInt()
                                    val topLeftY = schemaPoint.y.toInt()
                                    val (newSchema, newId) = baseSchema.placeConceptualItem(
                                        placementKind,
                                        topLeftX,
                                        topLeftY,
                                    )
                                    currentOnSchemaCommit(newSchema)
                                    currentOnSelectionChange(CanvasSelection.Element(newId))
                                } else if (!shiftHeldAtGestureStart) {
                                    // Tap on empty canvas (no tool or no model) → deselect
                                    currentOnSelectionChange(CanvasSelection.None)
                                }
                            } else {
                                if (isDragging) {
                                    selectionDoubleClickMemo.clear()
                                } else {
                                    when (hitResult) {
                                        is CanvasSelection.Multiple -> selectionDoubleClickMemo.clear()
                                        is CanvasSelection.Element,
                                        is CanvasSelection.Cardinality,
                                        -> {
                                            if (schemaAtGestureStart != null &&
                                                currentConceptualTool == ConceptualCanvasTool.None &&
                                                !gestureStartedOnResizeHandle
                                            ) {
                                                val nowMark = TimeSource.Monotonic.markNow()
                                                val field = selectionDoubleClickMemo.recordTapUp(
                                                    hit = hitResult,
                                                    downPosition = down.position,
                                                    nowMark = nowMark,
                                                )
                                                if (field != null) {
                                                    onConceptualInspectorSelectionFieldEditCb(field)
                                                }
                                            } else {
                                                selectionDoubleClickMemo.clear()
                                            }
                                        }
                                        CanvasSelection.None -> Unit
                                    }
                                }
                            }
                            break
                        }

                        val delta = change.position - startPointer
                        totalDrag = delta

                        if (!isDragging && (abs(totalDrag.x) > slop || abs(totalDrag.y) > slop)) {
                            isDragging = true
                        }

                        if (isDragging) {
                            change.consume()
                            val schemaSnapshot = schemaAtGestureStart

                            if (schemaSnapshot == null) {
                                panOffset = panAtGestureStart + totalDrag
                            } else {
                                val s = schemaSnapshot
                                val zInv = 1f / zoomAtGestureStart.coerceAtLeast(1e-4f)
                                when {
                                    // ── Resize cardinality label (manual size) ────────
                                    hitHandle != null &&
                                        resizeCardinalityConnId != null &&
                                        startCardinalityResizePos != null -> {
                                        val newPos =
                                            applyResize(
                                                handle = hitHandle,
                                                startPos = startCardinalityResizePos,
                                                totalDelta = Offset(totalDrag.x * zInv, totalDrag.y * zInv),
                                            )
                                        didMutateSchemaDuringDrag = true
                                        currentOnSchemaPreview(
                                            s.copy(
                                                connections = s.connections.map {
                                                    if (it.id == resizeCardinalityConnId) {
                                                        it.copy(cardinalityPosition = newPos)
                                                    } else {
                                                        it
                                                    }
                                                },
                                            ),
                                        )
                                    }

                                    // ── Resize element ─────────────────────────────────
                                    hitHandle != null &&
                                        dragElementId != null &&
                                        startElementPos != null -> {
                                        val newPos =
                                            applyResize(
                                                handle = hitHandle,
                                                startPos = startElementPos,
                                                totalDelta = Offset(totalDrag.x * zInv, totalDrag.y * zInv),
                                            )
                                        val elem = s.elements[dragElementId]
                                        if (elem != null) {
                                            didMutateSchemaDuringDrag = true
                                            val schemaResized = s.withElement(elem.withPosition(newPos))
                                            currentOnSchemaPreview(
                                                schemaResized.withRecalculatedFloatingCardinalityPositions(
                                                    onlyIncidentToElementId = dragElementId,
                                                    textMeasurer = currentTextMeasurer,
                                                ),
                                            )
                                        }
                                    }

                                    // ── Move element(s) — entire selection translates together ─────
                                    multiElementDragSnapshot != null -> {
                                        didMutateSchemaDuringDrag = true
                                        val movingSchema: ConceptualSchema = schemaSnapshot
                                        val (ids, startPositions) = multiElementDragSnapshot
                                        val dx = (totalDrag.x * zInv).toInt()
                                        val dy = (totalDrag.y * zInv).toInt()
                                        var schemaMoved: ConceptualSchema = movingSchema
                                        for ((id, startPos) in startPositions) {
                                            val el = schemaMoved.elements[id] ?: continue
                                            schemaMoved = schemaMoved.withElement(
                                                el.withPosition(
                                                    startPos.copy(
                                                        x = startPos.x + dx,
                                                        y = startPos.y + dy,
                                                    ),
                                                ),
                                            )
                                        }
                                        val selectedCardIds = when (val sel = selAtGestureStart) {
                                            is CanvasSelection.Multiple -> sel.cardinalityConnectionIds
                                            else -> emptySet()
                                        }
                                        currentOnSchemaPreview(
                                            schemaMoved.withCardinalityPositionsAfterElementsMovedByDelta(
                                                movedElementIds = ids,
                                                dx = dx,
                                                dy = dy,
                                                selectedCardinalityConnectionIds = selectedCardIds,
                                                textMeasurer = currentTextMeasurer,
                                            ),
                                        )
                                    }

                                    // ── Move cardinality label ────────────────────────
                                    dragConnectionId != null -> {
                                        val conn = s.connections.firstOrNull { it.id == dragConnectionId }
                                        if (conn != null) {
                                            val basePos = startCardPos ?: run {
                                                val ep = s.elements[conn.elementIdB]?.position
                                                    ?: s.elements[conn.elementIdA]?.position
                                                if (ep != null) {
                                                    ElementPosition(
                                                        x = ep.x + ep.width / 2,
                                                        y = ep.y - 20,
                                                        width = 50,
                                                        height = 20,
                                                    )
                                                } else null
                                            }
                                            if (basePos != null) {
                                                didMutateSchemaDuringDrag = true
                                                val newPos = basePos.copy(
                                                    x = basePos.x + (totalDrag.x * zInv).toInt(),
                                                    y = basePos.y + (totalDrag.y * zInv).toInt(),
                                                )
                                                val newConn = conn.copy(cardinalityPosition = newPos)
                                                val newConns = s.connections.map {
                                                    if (it.id == dragConnectionId) newConn else it
                                                }
                                                currentOnSchemaPreview(s.copy(connections = newConns))
                                            }
                                        }
                                    }

                                    // ── Pan canvas ────────────────────────────────────
                                    else -> {
                                        panOffset = panAtGestureStart + totalDrag
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(hoverSchemaForTooltip, hoverPanForTooltip, hoverZoomForTooltip) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Move,
                            PointerEventType.Enter,
                            -> {
                                val pos = event.changes.firstOrNull()?.position ?: continue
                                pointerOverCanvas = true
                                pointerView = pos
                                pushClipboardViewStateToParent(pos, over = true)
                                val sch = hoverSchemaForTooltip
                                if (sch == null) {
                                    hiddenAttributesTooltipAnchor = null
                                    linkToolHoverPick = null
                                    continue
                                }
                                val schemaPoint = viewOffsetToModel(pos, hoverPanForTooltip, hoverZoomForTooltip)
                                if (currentConceptualTool is ConceptualCanvasTool.LinkObjects) {
                                    val lp = hitTestConceptualLinkPick(sch, schemaPoint)
                                    if (lp != linkToolHoverPick) {
                                        linkToolHoverPick = lp
                                    }
                                } else if (linkToolHoverPick != null) {
                                    linkToolHoverPick = null
                                }
                                val hit = hitTestElement(sch, schemaPoint)
                                val id = (hit as? CanvasSelection.Element)?.id
                                if (id == null) {
                                    hiddenAttributesTooltipAnchor = null
                                    continue
                                }
                                val el = sch.elements[id]
                                if (el == null) {
                                    hiddenAttributesTooltipAnchor = null
                                    continue
                                }
                                val text = hiddenAttributesTooltipText(el.hiddenAttributes)
                                hiddenAttributesTooltipAnchor =
                                    if (text != null) pos to text else null
                            }
                            PointerEventType.Exit -> {
                                hiddenAttributesTooltipAnchor = null
                                linkToolHoverPick = null
                                pointerOverCanvas = false
                                pointerView = null
                                pushClipboardViewStateToParent(pointerLocal = null, over = false)
                            }
                            else -> {}
                        }
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val z = zoom.coerceAtLeast(1e-4f)
            translate(panOffset.x, panOffset.y) {
                scale(z, z, Offset.Zero) {
                    val vpMinX = (-panOffset.x) / z
                    val vpMaxX = (size.width - panOffset.x) / z
                    val vpMinY = (-panOffset.y) / z
                    val vpMaxY = (size.height - panOffset.y) / z
                    var gx = floor(vpMinX / GRID_STEP) * GRID_STEP
                    while (gx <= vpMaxX + GRID_STEP) {
                        var gy = floor(vpMinY / GRID_STEP) * GRID_STEP
                        while (gy <= vpMaxY + GRID_STEP) {
                            drawCircle(GRID_DOT, radius = 1f, center = Offset(gx, gy))
                            gy += GRID_STEP
                        }
                        gx += GRID_STEP
                    }

                    if (schema != null) {
                        val linkHighlightId =
                            when (val t = conceptualCanvasTool) {
                                is ConceptualCanvasTool.LinkObjects.AwaitingSecond -> t.first.elementId
                                else -> null
                            }
                        val linkHoverPick =
                            if (conceptualCanvasTool is ConceptualCanvasTool.LinkObjects) linkToolHoverPick else null
                        val bulkHighlightIds = bulkDeleteUiState?.markedElementIds ?: emptySet()
                        val selectionBandHighlightIds = selectionBandUiState?.markedElementIds ?: emptySet()
                        val selectionBandCardinalityIds =
                            selectionBandUiState?.markedCardinalityConnectionIds ?: emptySet()
                        drawSchema(
                            schema,
                            textMeasurer,
                            selection,
                            linkHighlightId,
                            linkHoverPick,
                            bulkHighlightIds,
                            selectionBandHighlightIds,
                            selectionBandHighlightCardinalityConnectionIds = selectionBandCardinalityIds,
                        )
                    }
                }
            }

            bulkDeleteUiState?.viewSelectionRect?.let { vr ->
                drawRect(
                    color = BULK_BAND_FILL,
                    topLeft = Offset(vr.left, vr.top),
                    size = Size(vr.width, vr.height),
                )
                drawRect(
                    color = BULK_BAND_STROKE,
                    topLeft = Offset(vr.left, vr.top),
                    size = Size(vr.width, vr.height),
                    style = Stroke(width = 2f),
                )
            }
            selectionBandUiState?.viewSelectionRect?.let { vr ->
                drawRect(
                    color = SELECTION_BAND_FILL,
                    topLeft = Offset(vr.left, vr.top),
                    size = Size(vr.width, vr.height),
                )
                drawRect(
                    color = SELECTION_BAND_STROKE,
                    topLeft = Offset(vr.left, vr.top),
                    size = Size(vr.width, vr.height),
                    style = Stroke(width = 2f),
                )
            }
        }

        hiddenAttributesTooltipAnchor?.let { (anchor, tipText) ->
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(
                    anchor.x.roundToInt() + 14,
                    anchor.y.roundToInt() + 14,
                ),
                properties = PopupProperties(focusable = false),
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Text(
                        text = tipText,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF1A1A1A),
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
        }

        if (schema == null) {
            Text(
                text = "Abra um arquivo para visualizar o modelo",
                fontSize = 14.sp,
                color = Color(0xFF888888),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
            )
        }
    }
}

/**
 * Rectangle multi-select: geometric hits only (no bulk-delete closure). When [additive] is true,
 * band picks union with [selectionAtStart] (Shift+ additive click / Shift+ additive drag).
 */
private suspend fun AwaitPointerEventScope.runRectangleSelectionGesture(
    down: PointerInputChange,
    panAtGestureStart: Offset,
    zoomAtGestureStart: Float,
    schema: ConceptualSchema,
    selectionAtStart: CanvasSelection,
    additive: Boolean,
    slop: Float,
    textMeasurer: TextMeasurer,
    onBandUi: (SelectionBandUiState?) -> Unit,
    onSelectionChange: (CanvasSelection) -> Unit,
) {
    fun commitBandSelection(start: Offset, end: Offset) {
        val band = conceptualBulkDeleteBandFromViewDiagonal(start, end, panAtGestureStart, zoomAtGestureStart)
        val pick = selectionBandGeometricPick(schema, band, textMeasurer)
        onSelectionChange(
            mergeCanvasBandPick(additive, selectionAtStart, pick.elementIds, pick.cardinalityConnectionIds),
        )
    }

    val startPointer = down.position
    var isDraggingBand = false
    var lastPointer = startPointer
    while (true) {
        val event = awaitPointerEvent()
        val pressedChanges = event.changes.filter { it.pressed }
        if (pressedChanges.size >= 2) {
            onBandUi(null)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            lastPointer = change.position
            if (!change.pressed) {
                if (isDraggingBand) {
                    commitBandSelection(startPointer, lastPointer)
                }
                onBandUi(null)
                break
            }
            continue
        }
        val change = event.changes.firstOrNull { it.id == down.id } ?: break
        lastPointer = change.position
        if (!change.pressed) {
            onBandUi(null)
            if (isDraggingBand) {
                commitBandSelection(startPointer, lastPointer)
            } else {
                val schemaPoint = viewOffsetToModel(down.position, panAtGestureStart, zoomAtGestureStart)
                val hit = hitTest(schema, schemaPoint, textMeasurer)
                if (additive) {
                    when (hit) {
                        is CanvasSelection.Element ->
                            onSelectionChange(toggleElementInMultiSelection(selectionAtStart, hit.id))
                        is CanvasSelection.Cardinality ->
                            onSelectionChange(
                                toggleCardinalityInMultiSelection(selectionAtStart, hit.connectionId),
                            )
                        CanvasSelection.None ->
                            onSelectionChange(selectionAtStart)
                        is CanvasSelection.Multiple -> Unit
                    }
                } else {
                    when (hit) {
                        is CanvasSelection.Element,
                        is CanvasSelection.Cardinality,
                        -> onSelectionChange(hit)
                        else -> onSelectionChange(CanvasSelection.None)
                    }
                }
            }
            break
        }
        val dragVec = lastPointer - startPointer
        if (!isDraggingBand && dragVec.getDistance() > slop) {
            isDraggingBand = true
        }
        if (isDraggingBand) {
            change.consume()
            val band = conceptualBulkDeleteBandFromViewDiagonal(
                startPointer,
                lastPointer,
                panAtGestureStart,
                zoomAtGestureStart,
            )
            val pick = selectionBandGeometricPick(schema, band, textMeasurer)
            val (e0, c0) = selectionAtStart.toMultiPickSets()
            val displayE = if (additive) e0 + pick.elementIds else pick.elementIds
            val displayC = if (additive) c0 + pick.cardinalityConnectionIds else pick.cardinalityConnectionIds
            val counts = bulkDeleteCategoryCounts(schema, displayE, displayC)
            val viewRect = normalizedBulkDeleteViewRect(startPointer, lastPointer)
            onBandUi(
                SelectionBandUiState(
                    viewSelectionRect = viewRect,
                    markedElementIds = displayE,
                    markedCardinalityConnectionIds = displayC,
                    counts = counts,
                ),
            )
        }
    }
}

private fun conceptualToolAllowsRightButtonCanvasPan(tool: ConceptualCanvasTool): Boolean =
    tool is ConceptualCanvasTool.BulkDeleteObjects || tool is ConceptualCanvasTool.RectangleSelection

/**
 * Same idea as [androidx.compose.foundation.gestures.awaitFirstDown], but [awaitFirstDown] only reacts to the
 * **primary** mouse button; middle and right never start a gesture. This variant also accepts **tertiary**
 * (middle) always, and **secondary** (right) when [bulkDeleteAllowsSecondaryPan] is true for that frame
 * (bulk delete and rectangle-selection tools).
 */
private fun PointerEvent.isCanvasGestureStartDown(
    requireUnconsumed: Boolean,
    bulkDeleteAllowsSecondaryPan: () -> Boolean,
): Boolean {
    val primaryButtonCausesDown = changes.all { it.type == PointerType.Mouse }
    val changedToDown = changes.all {
        if (requireUnconsumed) it.changedToDown() else it.changedToDownIgnoreConsumed()
    }
    if (!changedToDown) return false
    if (!primaryButtonCausesDown) return true
    val b = buttons
    return b.isPrimaryPressed ||
        b.isTertiaryPressed ||
        (bulkDeleteAllowsSecondaryPan() && b.isSecondaryPressed)
}

private suspend fun AwaitPointerEventScope.awaitCanvasGestureFirstDown(
    bulkDeleteAllowsSecondaryPan: () -> Boolean,
): PointerInputChange {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        if (event.isCanvasGestureStartDown(
                requireUnconsumed = false,
                bulkDeleteAllowsSecondaryPan = bulkDeleteAllowsSecondaryPan,
            )
        ) {
            return event.changes[0]
        }
    }
}

/** Arithmetic mean of [positions]; caller must pass a non-empty list. */
private fun centroidOfOffsets(positions: List<Offset>): Offset {
    var sx = 0f
    var sy = 0f
    for (p in positions) {
        sx += p.x
        sy += p.y
    }
    val n = positions.size.toFloat().coerceAtLeast(1f)
    return Offset(sx / n, sy / n)
}

/** Maps summed [androidx.compose.ui.input.pointer.PointerInputChange.scrollDelta] to canvas pan. */
private fun scrollDeltaForCanvasPan(
    raw: Offset,
    pointerShiftPressed: Boolean,
    keyboardRemapVerticalToHorizontal: Boolean,
): Offset {
    var x = raw.x
    var y = raw.y
    val remapVerticalToHorizontal = isDesktopTarget &&
        (pointerShiftPressed || keyboardRemapVerticalToHorizontal) &&
        abs(y) >= abs(x)
    if (remapVerticalToHorizontal) {
        x = y
        y = 0f
    }
    var out = Offset(x * canvasPointerScrollPanGain, y * canvasPointerScrollPanGain)
    if (invertCanvasPointerScrollPan) {
        out = Offset(-out.x, -out.y)
    }
    return out
}

private fun normalizedBulkDeleteViewRect(a: Offset, b: Offset): Rect {
    val left = min(a.x, b.x)
    val top = min(a.y, b.y)
    val right = max(a.x, b.x)
    val bottom = max(a.y, b.y)
    return Rect(left, top, right, bottom)
}

private fun conceptualBulkDeleteBandFromViewDiagonal(
    viewA: Offset,
    viewB: Offset,
    pan: Offset,
    zoom: Float,
): ConceptualBulkDeleteBand {
    val ma = viewOffsetToModel(viewA, pan, zoom)
    val mb = viewOffsetToModel(viewB, pan, zoom)
    return ConceptualBulkDeleteBand.fromCorners(ma.x, ma.y, mb.x, mb.y)
}

private fun processSpecializationToolTap(
    schema: ConceptualSchema,
    schemaPoint: Offset,
    variant: ConceptualSpecializationToolVariant,
    onMessage: (String) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
    onSelectionChange: (CanvasSelection) -> Unit,
) {
    val entityId = hitTestPlainEntityId(schema, schemaPoint)
    if (entityId == null) {
        onMessage("Clique na entidade que será especializada.")
        return
    }
    when (val r = applyConceptualSpecializationTool(schema, entityId, variant)) {
        is ConceptualSpecializationToolResult.Ok -> {
            onSchemaCommit(r.schema)
            onSelectionChange(CanvasSelection.Element(r.newSpecializationId))
        }
        is ConceptualSpecializationToolResult.Error -> onMessage(r.message)
    }
}

private fun processLinkObjectsTap(
    schema: ConceptualSchema,
    schemaPoint: Offset,
    toolState: ConceptualCanvasTool.LinkObjects,
    editorTabSessionId: Long,
    textMeasurer: TextMeasurer,
    onToolChange: (ConceptualCanvasTool) -> Unit,
    onMessage: (String) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
    onSelectionChange: (CanvasSelection) -> Unit,
) {
    val pick = hitTestConceptualLinkPick(schema, schemaPoint)
    when (toolState) {
        is ConceptualCanvasTool.LinkObjects.AwaitingFirst -> {
            if (pick == null) {
                onMessage("Selecione uma entidade, um relacionamento ou uma especialização.")
            } else {
                onSelectionChange(CanvasSelection.None)
                onToolChange(
                    ConceptualCanvasTool.LinkObjects.AwaitingSecond(
                        first = pick,
                        startedOnEditorTabId = editorTabSessionId,
                    ),
                )
            }
        }
        is ConceptualCanvasTool.LinkObjects.AwaitingSecond -> {
            if (editorTabSessionId != -1L &&
                toolState.startedOnEditorTabId != -1L &&
                toolState.startedOnEditorTabId != editorTabSessionId
            ) {
                onMessage(
                    "O primeiro objeto foi escolhido em outra aba. Conclua a ligação na mesma aba ou cancele (Esc) e inicie de novo.",
                )
                onToolChange(ConceptualCanvasTool.LinkObjects.AwaitingFirst)
                return
            }
            if (schema.elements[toolState.first.elementId] == null) {
                onMessage("O primeiro objeto da ligação não existe mais neste modelo. Inicie a ligação novamente.")
                onToolChange(ConceptualCanvasTool.LinkObjects.AwaitingFirst)
                return
            }
            if (pick == null) {
                onToolChange(ConceptualCanvasTool.LinkObjects.AwaitingFirst)
                return
            }
            when (val r = validateAndBuildConceptualLink(schema, toolState.first, pick)) {
                is ConceptualLinkValidationResult.Ok -> {
                    val beforeConnIds = schema.connections.map { it.id }.toSet()
                    var committed = r.schema.withFloatingCardinalityLayoutForgotten()
                    for (conn in committed.connections.filter { it.id !in beforeConnIds }) {
                        val enriched =
                            enrichConnectionWithInitialCardinalityPosition(committed, conn, textMeasurer)
                        committed = committed.copy(
                            connections = committed.connections.map {
                                if (it.id == conn.id) enriched else it
                            },
                        )
                    }
                    committed = committed.withRecalculatedFloatingCardinalityPositions(textMeasurer = textMeasurer)
                    onSchemaCommit(committed)
                    onSelectionChange(CanvasSelection.None)
                    onToolChange(ConceptualCanvasTool.LinkObjects.AwaitingFirst)
                }
                is ConceptualLinkValidationResult.Error -> {
                    onMessage(r.message)
                    onToolChange(ConceptualCanvasTool.LinkObjects.AwaitingFirst)
                }
            }
        }
    }
}

private fun processAutoSelfRelationshipTap(
    schema: ConceptualSchema,
    schemaPoint: Offset,
    textMeasurer: TextMeasurer,
    onMessage: (String) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
    onSelectionChange: (CanvasSelection) -> Unit,
) {
    val pick = hitTestConceptualLinkPick(schema, schemaPoint)
    if (pick == null) {
        onMessage("Selecione uma entidade.")
        return
    }
    when (val r = validateAndBuildConceptualLink(schema, pick, pick, schemaPoint)) {
        is ConceptualLinkValidationResult.Ok -> {
            val beforeConnIds = schema.connections.map { it.id }.toSet()
            var committed = r.schema.withFloatingCardinalityLayoutForgotten()
            for (conn in committed.connections.filter { it.id !in beforeConnIds }) {
                val enriched =
                    enrichConnectionWithInitialCardinalityPosition(committed, conn, textMeasurer)
                committed = committed.copy(
                    connections = committed.connections.map {
                        if (it.id == conn.id) enriched else it
                    },
                )
            }
            committed = committed.withRecalculatedFloatingCardinalityPositions(textMeasurer = textMeasurer)
            onSchemaCommit(committed)
            val newSelfRel = committed.selfRelationships.singleOrNull { it.id !in schema.elements }
            if (newSelfRel != null) {
                onSelectionChange(CanvasSelection.Element(newSelfRel.id))
            }
        }
        is ConceptualLinkValidationResult.Error -> onMessage(r.message)
    }
}

private fun processAttributeToolTap(
    schema: ConceptualSchema,
    schemaPoint: Offset,
    variant: ConceptualAttributeToolVariant,
    textMeasurer: TextMeasurer,
    layoutDirection: LayoutDirection,
    onMessage: (String) -> Unit,
    onSchemaCommit: (ConceptualSchema) -> Unit,
    onSelectionChange: (CanvasSelection) -> Unit,
) {
    val pickId = (hitTestElement(schema, schemaPoint) as? CanvasSelection.Element)?.id
    if (pickId == null) {
        onMessage("Clique no objeto que receberá o atributo.")
        return
    }
    when (val r = applyConceptualAttributeTool(schema, pickId, schemaPoint, variant)) {
        is ConceptualAttributeToolResult.Ok -> {
            var committed = r.schema.withAutoSizedAttributeSubtree(
                r.newPrimaryAttributeId,
                textMeasurer,
                layoutDirection,
            )
            committed = organizeAttributesOnOwnerSide(
                committed,
                r.ownerElementId,
                r.attachSide,
            )
            val placed = committed.elements[r.newPrimaryAttributeId] as? SchemaElement.Attribute
            if (placed?.isComposite == true) {
                committed = relayoutCompositeSubtree(committed, placed.id)
            }
            committed = committed.syncFloatingCardinalityLayoutAfterMutationFromBaseline(
                baseline = schema,
                textMeasurer = textMeasurer,
                rehomeConnectionsAbsentInBaseline = true,
            )
            onSchemaCommit(committed)
            onSelectionChange(CanvasSelection.Element(r.newPrimaryAttributeId))
        }
        is ConceptualAttributeToolResult.Error -> {
            onMessage(r.message)
        }
    }
}

// ── Resize helper ──────────────────────────────────────────────────────────────

/**
 * Applies a resize gesture to [startPos] based on which corner [handle] is being dragged
 * and the cumulative [totalDelta] since the start of the gesture.
 *
 * The minimum size is clamped to [ElementPosition.MIN_WIDTH_PX] × [ElementPosition.MIN_HEIGHT_PX].
 */
private fun applyResize(
    handle: ResizeHandle,
    startPos: ElementPosition,
    totalDelta: Offset,
): ElementPosition {
    val dx = totalDelta.x.toInt()
    val dy = totalDelta.y.toInt()
    val minSize = ElementPosition.MIN_WIDTH_PX

    return when (handle) {
        ResizeHandle.TOP_LEFT -> ElementPosition(
            x = startPos.x + dx,
            y = startPos.y + dy,
            width = (startPos.width - dx).coerceAtLeast(minSize),
            height = (startPos.height - dy).coerceAtLeast(minSize),
        )
        ResizeHandle.TOP_RIGHT -> ElementPosition(
            x = startPos.x,
            y = startPos.y + dy,
            width = (startPos.width + dx).coerceAtLeast(minSize),
            height = (startPos.height - dy).coerceAtLeast(minSize),
        )
        ResizeHandle.BOTTOM_LEFT -> ElementPosition(
            x = startPos.x + dx,
            y = startPos.y,
            width = (startPos.width - dx).coerceAtLeast(minSize),
            height = (startPos.height + dy).coerceAtLeast(minSize),
        )
        ResizeHandle.BOTTOM_RIGHT -> ElementPosition(
            x = startPos.x,
            y = startPos.y,
            width = (startPos.width + dx).coerceAtLeast(minSize),
            height = (startPos.height + dy).coerceAtLeast(minSize),
        )
    }
}

private val conceptualInspectorDoubleClickMaxGap = 550.milliseconds
private const val CONCEPTUAL_INSPECTOR_DOUBLE_CLICK_MAX_DISTANCE_PX = 48f

/**
 * Tracks two quick, close primary taps on the same [CanvasSelection] (element or cardinality label)
 * to trigger inspector inline edit.
 */
private class ConceptualSelectionDoubleClickMemo {
    private var pendingHit: CanvasSelection? = null
    private var pendingDownPosition: Offset = Offset.Zero
    private var pendingTimeMark: TimeMark? = null

    fun clear() {
        pendingHit = null
        pendingTimeMark = null
    }

    /**
     * @return Inspector row key ([InspectorSelectionFieldKeys]) when this pointer-up completes a double-tap.
     */
    fun recordTapUp(
        hit: CanvasSelection,
        downPosition: Offset,
        nowMark: TimeMark,
    ): String? {
        when (hit) {
            is CanvasSelection.Element,
            is CanvasSelection.Cardinality,
            -> Unit
            else -> {
                clear()
                return null
            }
        }
        val pHit = pendingHit
        val pMark = pendingTimeMark
        if (pHit != null && pMark != null &&
            pHit == hit &&
            (downPosition - pendingDownPosition).getDistance() <= CONCEPTUAL_INSPECTOR_DOUBLE_CLICK_MAX_DISTANCE_PX &&
            pMark.elapsedNow() <= conceptualInspectorDoubleClickMaxGap
        ) {
            clear()
            return when (hit) {
                is CanvasSelection.Element -> InspectorSelectionFieldKeys.Name
                is CanvasSelection.Cardinality -> InspectorSelectionFieldKeys.CardinalityRole
                else -> null
            }
        }
        pendingHit = hit
        pendingDownPosition = downPosition
        pendingTimeMark = nowMark
        return null
    }
}
