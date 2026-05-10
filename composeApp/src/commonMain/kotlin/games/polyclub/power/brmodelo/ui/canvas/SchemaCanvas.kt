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
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.ConceptualLinkValidationResult
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.SchemaElement
import games.polyclub.power.brmodelo.domain.placeConceptualItem
import games.polyclub.power.brmodelo.domain.validateAndBuildConceptualLink
import games.polyclub.power.brmodelo.ui.ConceptualCanvasTool
import games.polyclub.power.brmodelo.ui.toPlacementKindOrNull
import games.polyclub.power.brmodelo.ui.canvas.drawSchema
import games.polyclub.power.brmodelo.ui.canvas.withPosition
import kotlin.math.abs

// Background colour of the canvas (light grey, matching the original brModelo canvas background)
private val CANVAS_BG = Color(0xFFE8E8E8)
// Dot-grid colour (subtle)
private val GRID_DOT = Color(0xFFCCCCCC)
private const val GRID_STEP = 20f

/**
 * Interactive canvas that renders a [games.polyclub.power.brmodelo.domain.ConceptualSchema] using Compose [Canvas].
 *
 * Supports:
 * - **Pan**: drag on empty space moves the viewport.
 * - **Select**: tap on an element or cardinality label selects it.
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
 * @param modifier            Layout modifier applied to the outer Box.
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
    toolCursorModifier: Modifier = Modifier,
    canvasFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    var panOffset by remember { mutableStateOf(Offset(8f, 8f)) }
    val textMeasurer = rememberTextMeasurer()

    // rememberUpdatedState lets the gesture handler always see the latest values
    // without restarting the gesture on every recomposition.
    val currentSchema by rememberUpdatedState(schema)
    val currentSelection by rememberUpdatedState(selection)
    val currentOnSelectionChange by rememberUpdatedState(onSelectionChange)
    val currentOnSchemaPreview by rememberUpdatedState(onSchemaPreview)
    val currentOnSchemaCommit by rememberUpdatedState(onSchemaCommit)
    val currentPanOffset by rememberUpdatedState(panOffset)
    val currentCanvasFocusRequester by rememberUpdatedState(canvasFocusRequester)
    val currentConceptualTool by rememberUpdatedState(conceptualCanvasTool)
    val currentOnConceptualCanvasToolChange by rememberUpdatedState(onConceptualCanvasToolChange)
    val currentOnTransientUserMessage by rememberUpdatedState(onTransientUserMessage)
    val currentTextMeasurer by rememberUpdatedState(textMeasurer)

    Box(
        modifier = modifier
            .then(toolCursorModifier)
            .clipToBounds()
            .background(CANVAS_BG)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    currentCanvasFocusRequester?.requestFocus()

                    val panAtGestureStart = currentPanOffset
                    val schemaAtGestureStart = currentSchema
                    val selAtGestureStart = currentSelection

                    val schemaPoint = down.position - panAtGestureStart

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
                    val selectedElem = (selAtGestureStart as? CanvasSelection.Element)
                        ?.let { schemaAtGestureStart?.elements?.get(it.id) }
                    val cardinalitySel = selAtGestureStart as? CanvasSelection.Cardinality
                    val cardinalityConn = cardinalitySel?.let { sid ->
                        schemaAtGestureStart?.connections?.firstOrNull { it.id == sid.connectionId }
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
                    val hitHandleElem = selectedElem?.let {
                        getResizeHandleAt(it.position, schemaPoint)
                    }
                    val hitHandleCard = cardinalityResizeBox?.let {
                        getResizeHandleAt(it, schemaPoint)
                    }
                    val hitHandle = hitHandleElem ?: hitHandleCard

                    // Pascal behaviour: select immediately on pointer-down, not on pointer-up.
                    // Only skip if we're about to resize (the selection stays as-is).
                    if (hitHandle == null && hitResult != selAtGestureStart) {
                        currentOnSelectionChange(hitResult)
                    }

                    // Snapshot the element/connection to be dragged at gesture start,
                    // so we can apply absolute deltas instead of cumulative per-frame ones.
                    val resizeCardinalityConnId =
                        if (hitHandleCard != null) cardinalityConn?.id else null
                    val dragElementId = (hitResult as? CanvasSelection.Element)?.id
                        ?: (hitHandleElem?.let { (selAtGestureStart as? CanvasSelection.Element)?.id })
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

                    val startPointer = down.position
                    val slop = viewConfiguration.touchSlop
                    var totalDrag = Offset.Zero
                    var isDragging = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            // Pointer up: commit if we were dragging.
                            if (isDragging) {
                                val finalSchema = currentSchema
                                if (finalSchema != null) {
                                    currentOnSchemaCommit(finalSchema)
                                }
                            } else if (hitResult == CanvasSelection.None) {
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
                                } else {
                                    // Tap on empty canvas (no tool or no model) → deselect
                                    currentOnSelectionChange(CanvasSelection.None)
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
                            val s = schemaAtGestureStart

                            if (s == null) {
                                panOffset = panAtGestureStart + totalDrag
                            } else {
                                when {
                                    // ── Resize cardinality label (manual size) ────────
                                    hitHandle != null &&
                                        resizeCardinalityConnId != null &&
                                        startCardinalityResizePos != null -> {
                                        val newPos =
                                            applyResize(
                                                handle = hitHandle,
                                                startPos = startCardinalityResizePos,
                                                totalDelta = totalDrag,
                                            )
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
                                                totalDelta = totalDrag,
                                            )
                                        val elem = s.elements[dragElementId]
                                        if (elem != null) {
                                            val schemaResized = s.withElement(elem.withPosition(newPos))
                                            currentOnSchemaPreview(
                                                schemaResized.withRecalculatedFloatingCardinalityPositions(
                                                    onlyIncidentToElementId = dragElementId,
                                                    textMeasurer = currentTextMeasurer,
                                                ),
                                            )
                                        }
                                    }

                                    // ── Move element ─────────────────────────────────
                                    dragElementId != null && startElementPos != null -> {
                                        val newPos = startElementPos.copy(
                                            x = startElementPos.x + totalDrag.x.toInt(),
                                            y = startElementPos.y + totalDrag.y.toInt(),
                                        )
                                        val elem = s.elements[dragElementId]
                                        if (elem != null) {
                                            val schemaMoved = s.withElement(elem.withPosition(newPos))
                                            currentOnSchemaPreview(
                                                schemaMoved.withRecalculatedFloatingCardinalityPositions(
                                                    onlyIncidentToElementId = dragElementId,
                                                    textMeasurer = currentTextMeasurer,
                                                ),
                                            )
                                        }
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
                                                val newPos = basePos.copy(
                                                    x = basePos.x + totalDrag.x.toInt(),
                                                    y = basePos.y + totalDrag.y.toInt(),
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
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize().then(toolCursorModifier)) {
            // Optional dot grid (subtle background reference grid)
            val cols = (size.width / GRID_STEP).toInt() + 2
            val rows = (size.height / GRID_STEP).toInt() + 2
            val offsetX = panOffset.x % GRID_STEP
            val offsetY = panOffset.y % GRID_STEP
            for (col in 0..cols) {
                for (row in 0..rows) {
                    drawCircle(
                        GRID_DOT,
                        radius = 1f,
                        center = Offset(offsetX + col * GRID_STEP, offsetY + row * GRID_STEP),
                    )
                }
            }

            if (schema != null) {
                translate(panOffset.x, panOffset.y) {
                    val linkHighlightId =
                        when (val t = conceptualCanvasTool) {
                            is ConceptualCanvasTool.LinkObjects.AwaitingSecond -> t.first.elementId
                            else -> null
                        }
                    drawSchema(schema, textMeasurer, selection, linkHighlightId)
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

private fun processLinkObjectsTap(
    schema: ConceptualSchema,
    schemaPoint: Offset,
    toolState: ConceptualCanvasTool.LinkObjects,
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
                onMessage("Selecione uma entidade ou um relacionamento.")
            } else {
                onSelectionChange(CanvasSelection.None)
                onToolChange(ConceptualCanvasTool.LinkObjects.AwaitingSecond(pick))
            }
        }
        is ConceptualCanvasTool.LinkObjects.AwaitingSecond -> {
            if (pick == null) {
                onToolChange(ConceptualCanvasTool.LinkObjects.AwaitingFirst)
                return
            }
            when (val r = validateAndBuildConceptualLink(schema, toolState.first, pick)) {
                is ConceptualLinkValidationResult.Ok -> {
                    val beforeConnIds = schema.connections.map { it.id }.toSet()
                    var committed = r.schema
                    for (conn in committed.connections.filter { it.id !in beforeConnIds }) {
                        val enriched =
                            enrichConnectionWithInitialCardinalityPosition(committed, conn, textMeasurer)
                        committed = committed.copy(
                            connections = committed.connections.map {
                                if (it.id == conn.id) enriched else it
                            },
                        )
                    }
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
    when (val r = validateAndBuildConceptualLink(schema, pick, pick)) {
        is ConceptualLinkValidationResult.Ok -> {
            val beforeConnIds = schema.connections.map { it.id }.toSet()
            var committed = r.schema
            for (conn in committed.connections.filter { it.id !in beforeConnIds }) {
                val enriched =
                    enrichConnectionWithInitialCardinalityPosition(committed, conn, textMeasurer)
                committed = committed.copy(
                    connections = committed.connections.map {
                        if (it.id == conn.id) enriched else it
                    },
                )
            }
            onSchemaCommit(committed)
            val newSelfRel = committed.selfRelationships.singleOrNull { it.id !in schema.elements }
            if (newSelfRel != null) {
                onSelectionChange(CanvasSelection.Element(newSelfRel.id))
            }
        }
        is ConceptualLinkValidationResult.Error -> onMessage(r.message)
    }
}

// ── Resize helper ──────────────────────────────────────────────────────────────

/**
 * Applies a resize gesture to [startPos] based on which corner [handle] is being dragged
 * and the cumulative [totalDelta] since the start of the gesture.
 *
 * The minimum size is clamped to 10×10 to avoid negative/zero dimensions.
 */
private fun applyResize(
    handle: ResizeHandle,
    startPos: ElementPosition,
    totalDelta: Offset,
): ElementPosition {
    val dx = totalDelta.x.toInt()
    val dy = totalDelta.y.toInt()
    val minSize = 10

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
