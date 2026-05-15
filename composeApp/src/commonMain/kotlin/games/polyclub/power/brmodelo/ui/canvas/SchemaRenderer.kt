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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import games.polyclub.power.brmodelo.domain.AnnotationBackgroundColorPresets
import games.polyclub.power.brmodelo.domain.AnnotationType
import games.polyclub.power.brmodelo.domain.ArrowDirection
import games.polyclub.power.brmodelo.domain.CanvasSelection
import games.polyclub.power.brmodelo.domain.Connection
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.conceptualAttributeAttachPonto
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.SchemaElement
import games.polyclub.power.brmodelo.ui.vclColorRefToCompose
import games.polyclub.power.brmodelo.ui.canvas.drawAnnotation
import games.polyclub.power.brmodelo.ui.canvas.drawAssociativeEntity
import games.polyclub.power.brmodelo.ui.canvas.drawAttribute
import games.polyclub.power.brmodelo.ui.canvas.drawCardinalityLabel
import games.polyclub.power.brmodelo.ui.canvas.drawCardinalitySelectionHighlight
import games.polyclub.power.brmodelo.ui.canvas.drawCenteredLabel
import games.polyclub.power.brmodelo.ui.canvas.drawConnectionLine
import games.polyclub.power.brmodelo.ui.canvas.drawDirectionArrow
import games.polyclub.power.brmodelo.ui.canvas.drawElement
import games.polyclub.power.brmodelo.ui.canvas.drawElementSelectionHandles
import games.polyclub.power.brmodelo.ui.canvas.drawEntity
import games.polyclub.power.brmodelo.ui.canvas.drawEntityRectangle
import games.polyclub.power.brmodelo.ui.canvas.drawRelationship
import games.polyclub.power.brmodelo.ui.canvas.drawRelationshipDiamond
import games.polyclub.power.brmodelo.ui.canvas.drawSpecialization
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

// ── Colors translated from the original Pascal source ─────────────────────────
// Pen.Color := TColor(-5) → COLORREF $FFFFFFFB → R=251, G=255, B=255 (near-white outline)
private val BORDER_NEAR_WHITE = Color(0xFFFBFFFF)
// $00707070 → RGB(112,112,112)
private val SHADOW_DARK = Color(0xFF707070)
// $00B3B3B3 → RGB(179,179,179)
private val SHADOW_LIGHT = Color(0xFFB3B3B3)
// $00963636 → RGB(150,54,54)  — identifier attribute fill
private val IDENTIFIER_FILL = Color(0xFF963636)
// Font.Color = 32896 = 0x8080 (COLORREF BGR) → R=0, G=0x80, B=0x80 → teal
private val SPEC_LABEL_COLOR = Color(0xFF008080)
// $00363636 → dark gray for text box border
private val TEXT_BOX_DARK = Color(0xFF363636)

private val CANVAS_TEXT_STYLE = TextStyle(fontSize = 11.sp, color = Color.Black)
private val MULTIVALUE_CARD_STYLE = TextStyle(fontSize = 11.sp, color = Color.Black)

private val SELECTION_COLOR = Color(0xFF0060C0)
/** Connection polyline when the cardinality label (or specialization) is selected — darker than [SELECTION_COLOR]. */
private val SELECTION_CONNECTION_LINE_COLOR = Color(0xFF003060)

/**
 * Connection ids whose link lines should use [SELECTION_CONNECTION_LINE_COLOR]: cardinality picks,
 * and every link touching a selected [SchemaElement.Specialization].
 */
private fun connectionIdsForSelectionLinkedLineHighlight(
    selection: CanvasSelection,
    schema: ConceptualSchema,
): Set<Int> = when (selection) {
    CanvasSelection.None -> emptySet()
    is CanvasSelection.Cardinality -> setOf(selection.connectionId)
    is CanvasSelection.Element -> {
        val el = schema.elements[selection.id]
        if (el is SchemaElement.Specialization) {
            schema.connections
                .asSequence()
                .filter { it.elementIdA == selection.id || it.elementIdB == selection.id }
                .map { it.id }
                .toSet()
        } else {
            emptySet()
        }
    }
    is CanvasSelection.Multiple -> buildSet {
        addAll(selection.cardinalityConnectionIds)
        for (eid in selection.elementIds) {
            if (schema.elements[eid] is SchemaElement.Specialization) {
                schema.connections.forEach { c ->
                    if (c.elementIdA == eid || c.elementIdB == eid) add(c.id)
                }
            }
        }
    }
}

// ── Main entry point ──────────────────────────────────────────────────────────

/**
 * Draws the full [games.polyclub.power.brmodelo.domain.ConceptualSchema] into this [DrawScope].
 *
 * Rendering order mirrors VCL z-order (back → front):
 * 1. Non-assoc connection lines — behind all elements (selected cardinality / specialization links use a darker blue stroke).
 * 2. All elements (entities, relationships, attributes, AssociativeEntity outer+inner).
 * 3. AssociativeEntity connection lines — redrawn on top of outer rect white fill,
 *    faithfully replicating the VCL behaviour where [TLinha] components have a
 *    higher z-order than [TEntidadeAssoss]/[TChildRelacao].
 * 4. Inner diamonds of AssociativeEntity — redrawn on top of those connection lines.
 * 4b. Self-relationship diamonds — redrawn on top (same idea as the inner rhombus).
 * 5. Cardinality labels — floating on top of everything.
 * 6. Optional link-tool highlight (orange border, no resize handles) for the first picked element.
 * 7. Selection (blue border; corner resize squares only when the element allows manual resize —
 *    e.g. not for attributes with [SchemaElement.Attribute.autoSize]).
 *
 * @param selection When not [games.polyclub.power.brmodelo.domain.CanvasSelection.None], draws selection handles for the
 *   selected element or cardinality label. Defaults to [games.polyclub.power.brmodelo.domain.CanvasSelection.None] so that
 *   off-screen exporters that call this function without a selection continue to work.
 * @param linkToolHighlightElementId When set, draws a highlight border around that element (used by "Ligar objetos").
 * @param bulkDeleteHighlightIds When non-empty, draws a strong red overlay on each listed element (bulk-delete preview).
 * @param selectionBandHighlightCardinalityConnectionIds Blue overlay on cardinality labels during rectangle preview.
 */
fun DrawScope.drawSchema(
    schema: ConceptualSchema,
    textMeasurer: TextMeasurer,
    selection: CanvasSelection = CanvasSelection.None,
    linkToolHighlightElementId: Int? = null,
    bulkDeleteHighlightIds: Set<Int> = emptySet(),
    selectionBandHighlightIds: Set<Int> = emptySet(),
    selectionBandHighlightCardinalityConnectionIds: Set<Int> = emptySet(),
) {
    val dividedPoints = computeDividedPoints(schema)
    val selectionLinkedConnectionIds = connectionIdsForSelectionLinkedLineHighlight(selection, schema)

    // 1. Connection lines that do NOT involve an AssociativeEntity
    schema.connections.forEach { conn ->
        val a = schema.elements[conn.elementIdA]
        val b = schema.elements[conn.elementIdB]
        if (a !is SchemaElement.AssociativeEntity && b !is SchemaElement.AssociativeEntity) {
            val lineColor =
                if (conn.id in selectionLinkedConnectionIds) SELECTION_CONNECTION_LINE_COLOR else null
            drawConnectionLine(conn, schema, dividedPoints, lineHighlightColor = lineColor)
        }
    }
    // 2. All elements (including AssociativeEntity outer rect + inner diamond)
    schema.elements.values.forEach { element ->
        drawElement(element, schema, textMeasurer, bulkDeleteHighlightIds, selectionBandHighlightIds)
    }
    // 3. Re-draw connection lines that involve an AssociativeEntity, now on top of the
    //    outer rect white fill — only entity/relationship connections (not attributes),
    //    since attribute stubs are handled visually by the attribute's own rendering.
    schema.connections.forEach { conn ->
        val a = schema.elements[conn.elementIdA]
        val b = schema.elements[conn.elementIdB]
        if (a is SchemaElement.AssociativeEntity || b is SchemaElement.AssociativeEntity) {
            val lineColor =
                if (conn.id in selectionLinkedConnectionIds) SELECTION_CONNECTION_LINE_COLOR else null
            drawConnectionLine(conn, schema, dividedPoints, lineHighlightColor = lineColor)
        }
    }
    // 4. Re-draw the inner diamonds on top of the connection lines so that the diamond
    //    outline remains visible above lines that enter the outer rect area.
    schema.elements.values.filterIsInstance<SchemaElement.AssociativeEntity>().forEach { assoc ->
        val innerPos = assocInnerDiamondPos(assoc.position)
        drawRelationshipDiamond(
            innerPos,
            assoc.relationshipName,
            showName = true,
            textMeasurer,
        )
        // Bulk-delete tint was drawn on the outer rect in step 2; the inner diamond is redrawn here
        // with opaque fill on top of those lines, so it must be tinted again (same idea as step 4b).
        if (assoc.id in bulkDeleteHighlightIds) {
            drawBulkDeleteThreatHighlight(innerPos)
        } else if (assoc.id in selectionBandHighlightIds) {
            drawSelectionBandHighlight(innerPos)
        }
    }
    // 4b. Self-relationship diamonds on top of lines (outline + fill), like VCL z-order.
    schema.elements.values.filterIsInstance<SchemaElement.SelfRelationship>().forEach { selfRel ->
        drawRelationshipDiamond(selfRel.position, selfRel.name, showName = true, textMeasurer)
        if (selfRel.id in bulkDeleteHighlightIds) {
            drawBulkDeleteThreatHighlight(selfRel.position)
        } else if (selfRel.id in selectionBandHighlightIds) {
            drawSelectionBandHighlight(selfRel.position)
        }
    }
    // 5. Cardinality labels on top
    schema.connections.forEach { conn ->
        drawCardinalityLabel(conn, schema, dividedPoints, textMeasurer)
    }
    for (conn in schema.connections) {
        if (conn.id !in selectionBandHighlightCardinalityConnectionIds) continue
        cardinalityLabelHighlightElementPosition(schema, conn, textMeasurer)?.let { pos ->
            drawSelectionBandHighlight(pos)
        }
    }
    // 6. Link-tool first-target highlight (no corner handles)
    linkToolHighlightElementId?.let { hid ->
        schema.elements[hid]?.let { el ->
            drawLinkToolFirstTargetHighlight(el.position)
        }
    }
    // 7. Selection handles — drawn last so they are always on top of diagram content
    when (selection) {
        is CanvasSelection.Element -> {
            schema.elements[selection.id]?.let { el ->
                val showResizeHandles = el !is SchemaElement.Attribute || !el.autoSize
                drawElementSelectionHandles(el.position, showResizeHandles = showResizeHandles)
            }
        }
        is CanvasSelection.Cardinality -> {
            val conn = schema.connections.firstOrNull { it.id == selection.connectionId }
            if (conn != null && conn.showCardinality && conn.cardinality != null) {
                cardinalityLabelHighlightElementPosition(schema, conn, textMeasurer)?.let { pos ->
                    drawCardinalitySelectionHighlight(pos)
                    if (!conn.cardinalityAutoSize) {
                        drawElementSelectionHandles(pos)
                    }
                }
            }
        }
        is CanvasSelection.Multiple -> {
            for (id in selection.elementIds) {
                schema.elements[id]?.let { el ->
                    val showResizeHandles = el !is SchemaElement.Attribute || !el.autoSize
                    drawElementSelectionHandles(el.position, showResizeHandles = showResizeHandles)
                }
            }
            for (cid in selection.cardinalityConnectionIds) {
                val conn = schema.connections.firstOrNull { it.id == cid }
                if (conn != null && conn.showCardinality && conn.cardinality != null) {
                    cardinalityLabelHighlightElementPosition(schema, conn, textMeasurer)?.let { pos ->
                        drawCardinalitySelectionHighlight(pos)
                        if (!conn.cardinalityAutoSize) {
                            drawElementSelectionHandles(pos)
                        }
                    }
                }
            }
        }
        CanvasSelection.None -> Unit
    }
}

// ── Selection handles ─────────────────────────────────────────────────────────

private val HANDLE_FILL     = SELECTION_COLOR
private val HANDLE_SIZE     = HANDLE_SIZE_PX

/**
 * Draws a blue selection border around [position] and, when [showResizeHandles] is true,
 * four corner resize handles.
 *
 * Matches the visual style shown in the brModelo 3.0 screenshots:
 * - Thin blue dashed-style outline around the bounding box.
 * - Four solid blue squares at the corners (resize handles), unless [showResizeHandles] is false.
 */
private fun DrawScope.drawElementSelectionHandles(
    position: ElementPosition,
    showResizeHandles: Boolean = true,
) {
    val x = position.x.toFloat()
    val y = position.y.toFloat()
    val w = position.width.toFloat()
    val h = position.height.toFloat()

    // Selection border
    drawRect(
        color    = SELECTION_COLOR,
        topLeft  = Offset(x - 1f, y - 1f),
        size     = Size(w + 2f, h + 2f),
        style    = Stroke(1.5f),
    )

    if (!showResizeHandles) return

    // Corner handles
    val half = HANDLE_SIZE / 2f
    listOf(
        Offset(x,     y),      // top-left
        Offset(x + w, y),      // top-right
        Offset(x,     y + h),  // bottom-left
        Offset(x + w, y + h),  // bottom-right
    ).forEach { center ->
        drawRect(
            color   = HANDLE_FILL,
            topLeft = Offset(center.x - half, center.y - half),
            size    = Size(
                HANDLE_SIZE,
                HANDLE_SIZE
            ),
        )
    }
}

/** Highlight colour for the first endpoint while using "Ligar objetos" (distinct from selection blue). */
private val LINK_TOOL_FIRST_TARGET_COLOR = Color(0xFFFF6600)

/**
 * Draws a thick border around [position] without resize handles — used during the link tool's second click phase.
 */
private fun DrawScope.drawLinkToolFirstTargetHighlight(position: ElementPosition) {
    val x = position.x.toFloat()
    val y = position.y.toFloat()
    val w = position.width.toFloat()
    val h = position.height.toFloat()
    drawRect(
        color = LINK_TOOL_FIRST_TARGET_COLOR,
        topLeft = Offset(x - 2f, y - 2f),
        size = Size(w + 4f, h + 4f),
        style = Stroke(2.5f),
    )
}

/**
 * Draws a thin blue border around the cardinality label at [position].
 */
private fun DrawScope.drawCardinalitySelectionHighlight(position: ElementPosition) {
    drawRect(
        color   = SELECTION_COLOR,
        topLeft = Offset(position.x.toFloat() - 1f, position.y.toFloat() - 1f),
        size    = Size(position.width + 2f, position.height + 2f),
        style   = Stroke(1.5f),
    )
}

/** Strong red overlay for elements marked for bulk deletion (preview). */
private val BULK_DELETE_FILL = Color(0x66FF2D2D)
private val BULK_DELETE_STROKE = Color(0xFFAA0000)

private fun DrawScope.drawBulkDeleteThreatHighlight(position: ElementPosition) {
    val x = position.x.toFloat()
    val y = position.y.toFloat()
    val w = position.width.toFloat().coerceAtLeast(1f)
    val h = position.height.toFloat().coerceAtLeast(1f)
    drawRect(
        color = BULK_DELETE_FILL,
        topLeft = Offset(x, y),
        size = Size(w, h),
    )
    drawRect(
        color = BULK_DELETE_STROKE,
        topLeft = Offset(x, y),
        size = Size(w, h),
        style = Stroke(width = 2.5f),
    )
}

/** Blue overlay for elements inside the rectangle multi-select preview. */
private val SELECTION_BAND_FILL = Color(0x662E7DFF)
private val SELECTION_BAND_STROKE = Color(0xFF0060C0)

private fun DrawScope.drawSelectionBandHighlight(position: ElementPosition) {
    val x = position.x.toFloat()
    val y = position.y.toFloat()
    val w = position.width.toFloat().coerceAtLeast(1f)
    val h = position.height.toFloat().coerceAtLeast(1f)
    drawRect(
        color = SELECTION_BAND_FILL,
        topLeft = Offset(x, y),
        size = Size(w, h),
    )
    drawRect(
        color = SELECTION_BAND_STROKE,
        topLeft = Offset(x, y),
        size = Size(w, h),
        style = Stroke(width = 2.5f),
    )
}

// ── Element dispatch ──────────────────────────────────────────────────────────

private fun DrawScope.drawElement(
    element: SchemaElement,
    schema: ConceptualSchema,
    textMeasurer: TextMeasurer,
    bulkDeleteHighlightIds: Set<Int> = emptySet(),
    selectionBandHighlightIds: Set<Int> = emptySet(),
) {
    when (element) {
        is SchemaElement.Entity -> drawEntity(element, textMeasurer)
        is SchemaElement.Relationship -> drawRelationship(element, textMeasurer)
        is SchemaElement.AssociativeEntity -> drawAssociativeEntity(element, textMeasurer)
        is SchemaElement.Attribute -> drawAttribute(element, schema, textMeasurer)
        is SchemaElement.Specialization -> drawSpecialization(element, schema, textMeasurer)
        is SchemaElement.SelfRelationship -> Unit // drawn in drawSchema step 4b on top of lines
        is SchemaElement.Annotation -> drawAnnotation(element, textMeasurer)
    }
    when {
        element.id in bulkDeleteHighlightIds && element !is SchemaElement.SelfRelationship ->
            drawBulkDeleteThreatHighlight(element.position)
        element.id in selectionBandHighlightIds && element !is SchemaElement.SelfRelationship ->
            drawSelectionBandHighlight(element.position)
    }
}

// ── Entity ────────────────────────────────────────────────────────────────────

/**
 * Draws a rectangular entity element.
 *
 * Faithfully reproduces [TBaseEntidade.Paint] from mer.pas:
 * - White fill
 * - Near-white main border to (width-3, height-3)
 * - Dark shadow at (width-2) and (height-2)
 * - Light shadow at (width-1) and (height-1)
 * - Centered text label via [TBase.Paint]
 *
 * Weak entities get a double border (extra inner line at +2 px offset).
 */
private fun DrawScope.drawEntity(entity: SchemaElement.Entity, textMeasurer: TextMeasurer) {
    val p = entity.position
    drawEntityRectangle(p, isWeak = entity.isWeak)
    drawCenteredLabel(entity.name, p, textMeasurer, entity.labelStyle.bold, entity.labelStyle.italic)
}

/**
 * Draws the rectangular border of an entity (or associative entity outer rect).
 * In the original, weak entities have a double-line border.
 */
internal fun DrawScope.drawEntityRectangle(p: ElementPosition, isWeak: Boolean = false) {
    val x = p.x.toFloat()
    val y = p.y.toFloat()
    val w = p.width.toFloat()
    val h = p.height.toFloat()

    // White background fill
    drawRect(Color.White, topLeft = Offset(x, y), size = Size(w, h))

    // Main border: black rectangle inset by 3px on right/bottom for relief effect
    val stroke1 = Stroke(1f)
    val rectInner = Rect(x, y, x + w - 3f, y + h - 3f)
    drawRect(Color.Black, topLeft = rectInner.topLeft, size = rectInner.size, style = stroke1)

    // Double border for weak entity (inner rect inset 3px more)
    if (isWeak) {
        val inner2 = Rect(x + 3f, y + 3f, x + w - 6f, y + h - 6f)
        drawRect(Color.Black, topLeft = inner2.topLeft, size = inner2.size, style = stroke1)
    }

    // Shadow at -2 (right and bottom edges only)
    drawLine(SHADOW_DARK, Offset(x + w - 2f, y), Offset(x + w - 2f, y + h - 2f))
    drawLine(SHADOW_DARK, Offset(x, y + h - 2f), Offset(x + w - 2f, y + h - 2f))

    // Shadow at -1
    drawLine(SHADOW_LIGHT, Offset(x + w - 1f, y), Offset(x + w - 1f, y + h - 1f))
    drawLine(SHADOW_LIGHT, Offset(x, y + h - 1f), Offset(x + w - 1f, y + h - 1f))
}

// ── Relationship (diamond) ────────────────────────────────────────────────────

/**
 * Draws a diamond-shaped relationship element.
 *
 * Reproduces [TBaseRelacao.Paint] from mer.pas:
 * - Diamond polygon with 4 vertices at mid-edges
 * - Black pen
 * - Shadow segment at bottom-right
 * - Centred text label
 */
private fun DrawScope.drawRelationship(rel: SchemaElement.Relationship, textMeasurer: TextMeasurer) {
    drawRelationshipDiamond(rel.position, rel.name, showName = rel.showName, textMeasurer,
        rel.labelStyle.bold, rel.labelStyle.italic)
    drawDirectionArrow(rel.arrowDirection, rel.position)
}

internal fun DrawScope.drawRelationshipDiamond(
    p: ElementPosition,
    name: String,
    showName: Boolean,
    textMeasurer: TextMeasurer,
    bold: Boolean = false,
    italic: Boolean = false,
) {
    val x = p.x.toFloat()
    val y = p.y.toFloat()
    val w = p.width.toFloat()
    val h = p.height.toFloat()
    val cx = x + w / 2f
    val cy = y + h / 2f

    // White fill
    val diamond = Path().apply {
        moveTo(cx, y)
        lineTo(x + w - 1f, cy - 1f)
        lineTo(cx - 1f, y + h - 1f)
        lineTo(x, cy)
        close()
    }
    drawPath(diamond, Color.White, style = Fill)
    drawPath(diamond, Color.Black, style = Stroke(1f))

    // Shadow segment (bottom-right, matching Pascal $B3B3B3 two-pixel segment)
    drawLine(SHADOW_LIGHT, Offset(x + w, cy), Offset(cx, y + h))

    if (showName) {
        drawCenteredLabel(name, p, textMeasurer, bold, italic)
    }
}

/**
 * Draws the direction-arrow indicator (TSeta) beside a relationship diamond.
 *
 * Reproduces [TSeta.Paint] and [TSeta.Alinhe] from mer.pas (lines 10887–10993).
 *
 * The indicator is a 9 px wide strip placed just outside one of the four diamond edges
 * (2 px gap). It contains a full-length guide line through its centre and a small
 * arrowhead (4 strokes) at one end, exactly as Pascal draws with MoveTo/LineTo.
 *
 * Pascal layout:
 *  - posicao 1,2 → LEFT of diamond  (9 × h strip)
 *  - posicao 3,4 → TOP of diamond   (w × 9 strip)
 *  - posicao 5,6 → RIGHT of diamond (9 × h strip)
 *  - posicao 7,8 → BOTTOM of diamond(w × 9 strip)
 */
private fun DrawScope.drawDirectionArrow(direction: ArrowDirection, pos: ElementPosition) {
    if (direction == ArrowDirection.NONE) return
    val x = pos.x.toFloat()
    val y = pos.y.toFloat()
    val w = pos.width.toFloat()
    val h = pos.height.toFloat()
    val STRIP = 9f        // Largura = 9 in Pascal
    val GAP   = 2f        // gap between diamond edge and strip
    val L     = 5f        // Largura div 2 + 1 = 5 (arrowhead offset from edge)

    // The 4 arrowhead shapes translated directly from TSeta.Paint's MoveTo/LineTo sequences.
    // All coordinates are in absolute canvas space. cl=component left, ct=component top.
    // W=L=5 (Largura div 2 + 1) for 9px strips; STRIP=Largura=9.
    //
    // posicao 1,6: (W,1)→(1,L)→(L,L-2)→(Largura,L)→(W,1)  — UP arrowhead
    fun arrowUp(lx: Float, cl: Float, y: Float) {
        drawLine(Color.Black, Offset(lx,        y + 1f), Offset(cl + 1f,    y + L))
        drawLine(Color.Black, Offset(cl + 1f,   y + L),  Offset(lx,         y + L - 2f))
        drawLine(Color.Black, Offset(lx,        y + L - 2f), Offset(cl + STRIP, y + L))
        drawLine(Color.Black, Offset(cl + STRIP, y + L), Offset(lx,          y + 1f))
    }
    // posicao 2,5: (W,H-1)→(1,H-L)→(L,H-L+3)→(Largura,H-L)→(W,H-1)  — DOWN arrowhead
    // H = diamond height h; H-L = h-5; H-L+3 = h-2.
    fun arrowDown(lx: Float, cl: Float, y: Float, h: Float) {
        drawLine(Color.Black, Offset(lx,        y + h - 1f), Offset(cl + 1f,    y + h - L))
        drawLine(Color.Black, Offset(cl + 1f,   y + h - L),  Offset(lx,         y + h - L + 3f))
        drawLine(Color.Black, Offset(lx,        y + h - L + 3f), Offset(cl + STRIP, y + h - L))
        drawLine(Color.Black, Offset(cl + STRIP, y + h - L), Offset(lx,           y + h - 1f))
    }
    // posicao 3,8: (W-1,H)→(W-L,1)→(W-L+3,H)→(W-L,Height)→(W-1,H)  — RIGHT arrowhead
    // For horizontal strips W = diamond width, Height = STRIP = 9; H = 5.
    fun arrowRight(x: Float, ct: Float, w: Float, ly: Float) {
        val tipX  = x + w - 1f        // Width-1
        val baseX = x + w - L         // Width-L = w-5
        val notchX = x + w - L + 3f  // Width-L+3 = w-2
        drawLine(Color.Black, Offset(tipX,   ly), Offset(baseX, ct + 1f))
        drawLine(Color.Black, Offset(baseX,  ct + 1f), Offset(notchX, ly))
        drawLine(Color.Black, Offset(notchX, ly), Offset(baseX, ct + STRIP))
        drawLine(Color.Black, Offset(baseX,  ct + STRIP), Offset(tipX, ly))
    }
    // posicao 4,7: (1,H)→(L,1)→(L-2,H)→(L,Height)→(1,H)  — LEFT arrowhead
    fun arrowLeft(x: Float, ct: Float, ly: Float) {
        val tipX   = x + 1f       // 1
        val baseX  = x + L        // L=5
        val notchX = x + L - 2f  // L-2=3
        drawLine(Color.Black, Offset(tipX,   ly), Offset(baseX, ct + 1f))
        drawLine(Color.Black, Offset(baseX,  ct + 1f), Offset(notchX, ly))
        drawLine(Color.Black, Offset(notchX, ly), Offset(baseX, ct + STRIP))
        drawLine(Color.Black, Offset(baseX,  ct + STRIP), Offset(tipX, ly))
    }

    when (direction) {
        // posicao 1, 2 — strip on the LEFT side (9 × h, spanning full diamond height)
        ArrowDirection.LEFT_UP, ArrowDirection.LEFT_DOWN -> {
            val cl = x - GAP - STRIP    // component left edge
            val lx = cl + L             // guide line X  (W = Largura div 2 + 1 = 5)
            drawLine(Color.Black, Offset(lx, y + 1f), Offset(lx, y + h - 1f))
            if (direction == ArrowDirection.LEFT_UP) arrowUp(lx, cl, y)
            else                                     arrowDown(lx, cl, y, h)
        }
        // posicao 3, 4 — strip on TOP (w × 9, spanning full diamond width)
        ArrowDirection.TOP_RIGHT, ArrowDirection.TOP_LEFT -> {
            val ct = y - GAP - STRIP    // component top edge
            val ly = ct + L             // guide line Y
            drawLine(Color.Black, Offset(x + 1f, ly), Offset(x + w - 1f, ly))
            if (direction == ArrowDirection.TOP_RIGHT) arrowRight(x, ct, w, ly)
            else                                       arrowLeft(x, ct, ly)
        }
        // posicao 5, 6 — strip on the RIGHT side
        ArrowDirection.RIGHT_DOWN, ArrowDirection.RIGHT_UP -> {
            val cl = x + w + GAP        // component left edge (right of diamond)
            val lx = cl + L
            drawLine(Color.Black, Offset(lx, y + 1f), Offset(lx, y + h - 1f))
            if (direction == ArrowDirection.RIGHT_UP) arrowUp(lx, cl, y)
            else                                      arrowDown(lx, cl, y, h)
        }
        // posicao 7, 8 — strip on the BOTTOM
        ArrowDirection.BOTTOM_LEFT, ArrowDirection.BOTTOM_RIGHT -> {
            val ct = y + h + GAP        // component top edge (below diamond)
            val ly = ct + L
            drawLine(Color.Black, Offset(x + 1f, ly), Offset(x + w - 1f, ly))
            if (direction == ArrowDirection.BOTTOM_RIGHT) arrowRight(x, ct, w, ly)
            else                                          arrowLeft(x, ct, ly)
        }
        ArrowDirection.NONE -> Unit
    }
}

// ── Associative Entity ────────────────────────────────────────────────────────

/**
 * Draws an associative entity: outer entity rectangle + inner relationship diamond.
 *
 * Reproduces [TEntidadeAssoss.Paint] from mer.pas.
 * The inner diamond is inset by 15 px on all sides (matching the original's InflateRect -15).
 */
private fun DrawScope.drawAssociativeEntity(assoc: SchemaElement.AssociativeEntity, textMeasurer: TextMeasurer) {
    val p = assoc.position
    drawEntityRectangle(p)

    // Inner diamond (child relationship) — inset by 15px
    val innerPos = ElementPosition(
        x = p.x + 15,
        y = p.y + 15,
        width = (p.width - 30).coerceAtLeast(10),
        height = (p.height - 30).coerceAtLeast(10),
    )
    drawRelationshipDiamond(innerPos, assoc.relationshipName, showName = true, textMeasurer)
    drawDirectionArrow(assoc.arrowDirection, innerPos)

    // Entity name is drawn right-aligned at top (DT_RIGHT | DT_WORDBREAK in original)
    if (assoc.name.isNotBlank()) {
        val maxW = (p.width - 8).coerceAtLeast(1)
        val layout = textMeasurer.measure(
            assoc.name,
            style = CANVAS_TEXT_STYLE.copy(textAlign = TextAlign.Right),
            constraints = Constraints(maxWidth = maxW),
        )
        drawText(layout, topLeft = Offset(p.x + p.width - 4f - layout.size.width, p.y + 2f))
    }
}

// ── Attribute (ellipse) ───────────────────────────────────────────────────────

/**
 * Draws an attribute element: small ellipse + connecting stub + label.
 *
 * Reproduces [TAtributo.Paint] from mer.pas.
 *
 * Orientation (which side the ellipse is on) is inferred from the attribute position
 * relative to its owner element, since [games.polyclub.power.brmodelo.domain.SchemaElement.Attribute] stores coordinates
 * but not the VCL [Orientacao] enum value (computed dynamically in the original).
 *
 * Uses [games.polyclub.power.brmodelo.domain.conceptualAttributeAttachPonto] (same rules as legacy `attrPontoByPosition`).
 *
 * - Identifier attributes: ellipse filled with [games.polyclub.power.brmodelo.ui.canvas.IDENTIFIER_FILL] (#963636).
 * - Multi-valued: cardinality string appended to label.
 * - Optional attributes: ellipse outline drawn with a dashed stroke.
 */
private fun DrawScope.drawAttribute(
    attr: SchemaElement.Attribute,
    schema: ConceptualSchema,
    textMeasurer: TextMeasurer,
) {
    val p = attr.position
    val x = p.x.toFloat()
    val y = p.y.toFloat()
    val w = p.width.toFloat()
    val h = p.height.toFloat()

    // Ellipse diameter = Height - 3, matching "P := Height - 3" in Pascal
    val diameter = (h - 3f).coerceAtLeast(8f)
    val meio = (diameter - 1f) / 2f + 2f  // vertical centre of ellipse, matches Pascal

    // Orientation follows TBase.OrganizeAtributos from mer.pas:
    //   P=1 (owner LEFT side) → OrientacaoD → ellipse on RIGHT
    //   P≠1 (TOP/RIGHT/BOTTOM) → OrientacaoE → ellipse on LEFT
    val owner = schema.elements[attr.ownerId]
    val ellipseOnLeft = if (owner != null) {
        conceptualAttributeAttachPonto(owner.position, p) != 1
    } else false  // default: ellipse on right

    val textLabel = buildString {
        append(attr.name)
        if (attr.isMultiValued) {
            val card = attr.cardinality
            if (card.minCardinality != 0 || card.maxCardinality != 0) {
                append(" ${card.toLabel()}")
            }
        }
    }

    val ellipseFill = if (attr.isIdentifier) IDENTIFIER_FILL else Color.White
    val ellipseStroke = if (attr.isOptional) {
        Stroke(
            width = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 2f), 0f),
        )
    } else {
        Stroke(1f)
    }

    if (ellipseOnLeft) {
        // Stub: short horizontal line from left edge to x+5
        drawLine(Color.Black, Offset(x, y + meio), Offset(x + 5f, y + meio))

        // Ellipse at (x+5, y, x+5+diameter, y+diameter)
        val ellipseTopLeft = Offset(x + 5f, y)
        drawOval(ellipseFill, topLeft = ellipseTopLeft, size = Size(diameter, diameter))
        drawOval(Color.Black, topLeft = ellipseTopLeft, size = Size(diameter, diameter), style = ellipseStroke)

        // Composite marker: 4x4 outline rectangle at right edge, matching Pascal's
        // Rectangle(Width-4, (Height-4)/2, Width, (Height+4)/2) with bsClear brush.
        if (attr.isComposite) {
            val markerTop = (h - 4f) / 2f + y
            if (attr.isIdentifier) drawRect(IDENTIFIER_FILL, topLeft = Offset(x + w - 4f, markerTop), size = Size(4f, 4f))
            drawRect(Color.Black, topLeft = Offset(x + w - 4f, markerTop), size = Size(4f, 4f), style = Stroke(1f))
        }

        // Text to the right of the ellipse — single line, matching Pascal's single-line-height
        // DrawText rect (Bottom := Top + TextHeight('W') prevents actual wrapping).
        val textX = x + h + 5f
        val textMaxW = (w - h - 5f).toInt().coerceAtLeast(1)
        if (textLabel.isNotBlank() && textMaxW > 0) {
            val layout = textMeasurer.measure(
                textLabel,
                style = CANVAS_TEXT_STYLE,
                constraints = Constraints(maxWidth = textMaxW),
                softWrap = false,
            )
            val textY = y + (h - layout.size.height) / 2f - 1f
            drawText(layout, topLeft = Offset(textX, textY))
        }

        // Composite asterisk at top-left (clBlue in original)
        if (attr.isComposite) {
            val asterisk = textMeasurer.measure("*", style = CANVAS_TEXT_STYLE.copy(color = Color.Blue))
            drawText(asterisk, topLeft = Offset(x, y))
        }

        // TBarraDeAtributos: vertical bar at the right edge connecting child attachment pts
        if (attr.isComposite && attr.childAttributeIds.size >= 2) {
            val childCount = attr.childAttributeIds.size
            val barH = (h * childCount + childCount * 2 - h).coerceAtLeast(2f)
            val barTop = y + h / 2f - barH / 2f
            val barX = x + w - 2f  // bar centre X = attr.right - 2
            drawLine(Color.Black, Offset(barX, barTop), Offset(barX, barTop + barH))
        }
    } else {
        // Ellipse on right side (OrientacaoD): stub goes right, text to the left
        val ellipseLeft = x + w - 5f - diameter
        drawLine(Color.Black, Offset(x + w - 5f, y + meio), Offset(x + w, y + meio))

        val ellipseTopLeft = Offset(ellipseLeft, y)
        drawOval(ellipseFill, topLeft = ellipseTopLeft, size = Size(diameter, diameter))
        drawOval(Color.Black, topLeft = ellipseTopLeft, size = Size(diameter, diameter), style = ellipseStroke)

        // Composite marker: 4x4 outline rectangle at left edge (same Pascal logic, OrientacaoD side).
        if (attr.isComposite) {
            val markerTop = (h - 4f) / 2f + y
            if (attr.isIdentifier) drawRect(IDENTIFIER_FILL, topLeft = Offset(x, markerTop), size = Size(4f, 4f))
            drawRect(Color.Black, topLeft = Offset(x, markerTop), size = Size(4f, 4f), style = Stroke(1f))
        }

        // Text to the left of the ellipse — single line (same Pascal single-line-height rect logic).
        val textMaxW = (w - diameter - 10f).toInt().coerceAtLeast(1)
        if (textLabel.isNotBlank() && textMaxW > 0) {
            val layout = textMeasurer.measure(
                textLabel,
                style = CANVAS_TEXT_STYLE.copy(textAlign = TextAlign.Right),
                constraints = Constraints(maxWidth = textMaxW),
                softWrap = false,
            )
            val textY = y + (h - layout.size.height) / 2f - 1f
            drawText(layout, topLeft = Offset(x, textY))
        }

        // Composite asterisk at top-right
        if (attr.isComposite) {
            val asterisk = textMeasurer.measure("*", style = CANVAS_TEXT_STYLE.copy(color = Color.Blue))
            val asteriskX = x + w - asterisk.size.width
            drawText(asterisk, topLeft = Offset(asteriskX, y))
        }

        // TBarraDeAtributos: vertical bar at the left edge (OrientacaoD)
        if (attr.isComposite && attr.childAttributeIds.size >= 2) {
            val childCount = attr.childAttributeIds.size
            val barH = (h * childCount + childCount * 2 - h).coerceAtLeast(2f)
            val barTop = y + h / 2f - barH / 2f
            val barX = x + 2f  // bar centre X = attr.left + 2
            drawLine(Color.Black, Offset(barX, barTop), Offset(barX, barTop + barH))
        }
    }
}

// ── Specialization (triangle) ─────────────────────────────────────────────────

/**
 * Draws a specialization node (ISA hierarchy triangle).
 *
 * Reproduces [TEspecializacao.Paint] from mer.pas:
 * - Filled triangle using 3 MoveTo/LineTo segments, Pen.Width=2
 * - 'p' label inside (italic+bold, colour [games.polyclub.power.brmodelo.ui.canvas.SPEC_LABEL_COLOR]) when [isPartial]
 *
 * In the original, [FalsasBases] stores the 3 computed vertices, derived from the
 * specialisation's position and width/height. We reconstruct them here from the
 * stored [games.polyclub.power.brmodelo.domain.ElementPosition].
 */
private fun DrawScope.drawSpecialization(
    spec: SchemaElement.Specialization,
    schema: ConceptualSchema,
    textMeasurer: TextMeasurer,
) {
    val p = spec.position
    val x = p.x.toFloat()
    val y = p.y.toFloat()
    val w = p.width.toFloat()
    val h = p.height.toFloat()

    // Pascal `Redesenhe` (mer.pas ~8643) integer arithmetic, NOT centre-of-bbox:
    //   meio := aLeft + ((aWidth-3) div 2)
    //   H    := aTop  + (aHeight-3)
    //   W    := aLeft + (aWidth-3)
    // The triangle's three vertices come straight from `FalsasBases`, which align with
    // the snap points that `specializationEncaixes` returns — keeping connection lines
    // pixel-perfect with parent/child entity centres.
    val meio = ((p.width - 3) / 2).toFloat()
    val hOff = (p.height - 3).toFloat()
    val wOff = (p.width - 3).toFloat()
    val baseEntity = schema.elements[spec.baseEntityId]
    val isAcima = baseEntity != null && p.y < baseEntity.position.y

    val path = Path().apply {
        if (isAcima) {
            // Apex pointing DOWN: base on top, apex at (meio, hOff)
            moveTo(x, y)
            lineTo(x + wOff, y)
            lineTo(x + meio, y + hOff)
        } else {
            // Apex pointing UP (default POSI_ABAIXO): base on bottom, apex at (meio, 0)
            moveTo(x + meio, y)
            lineTo(x + wOff, y + hOff)
            lineTo(x, y + hOff)
        }
        close()
    }
    drawPath(path, Color.White, style = Fill)
    drawPath(path, Color.Black, style = Stroke(2f))

    // 'p' label for partial specialization (italic+bold, teal colour, Font.Color = 32896)
    if (spec.isPartial) {
        val pStyle = CANVAS_TEXT_STYLE.copy(
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            color = SPEC_LABEL_COLOR,
        )
        val pLayout = textMeasurer.measure("p", style = pStyle)
        val pX = x + w - pLayout.size.width - 7f
        val pY = y + (h - pLayout.size.height - 3f) / 2f
        drawText(pLayout, topLeft = Offset(pX, pY))
    }
}

// ── Annotation (text box) ─────────────────────────────────────────────────────

/**
 * Draws a free-text annotation element.
 *
 * Reproduces [TBaseTexto.Paint] from mer.pas. Supports three visual modes:
 * - [games.polyclub.power.brmodelo.domain.AnnotationType.PLAIN]: transparent background, text only
 * - [games.polyclub.power.brmodelo.domain.AnnotationType.HINT]: rounded-rect with shadow
 * - [games.polyclub.power.brmodelo.domain.AnnotationType.BOX]: rectangle with shadow (3D raised look)
 */
private fun DrawScope.drawAnnotation(ann: SchemaElement.Annotation, textMeasurer: TextMeasurer) {
    val p = ann.position
    val x = p.x.toFloat()
    val y = p.y.toFloat()
    val w = p.width.toFloat()
    val h = p.height.toFloat()

    // Resolve background colour (COLORREF BGR → RGB conversion)
    val bgColor = vclColorRefToCompose(ann.color ?: AnnotationBackgroundColorPresets.DEFAULT_COLOR_REF)

    // Pascal: if (Width < 15) or (Height < 15) then F := 5 else F := 15 → corner = F-5 = 0 or 10.
    // GDI RoundRect corner parameter is the ellipse diameter, so Compose radius = diameter/2 = 5.
    val hintCorner = androidx.compose.ui.geometry.CornerRadius(if (w < 15f || h < 15f) 0f else 5f)

    when (ann.annotationType) {
        AnnotationType.BOX -> {
            // Pascal: Rectangle(2,2, W-1,H-1) shadow then Rectangle(0,0, W-3,H-3) body.
            // Both rectangles are W-3 × H-3; shadow is offset (+2, +2).
            drawRect(SHADOW_LIGHT, topLeft = Offset(x + 2f, y + 2f), size = Size(w - 3f, h - 3f))
            drawRect(bgColor, topLeft = Offset(x, y), size = Size(w - 3f, h - 3f))
            drawRect(TEXT_BOX_DARK, topLeft = Offset(x, y), size = Size(w - 3f, h - 3f), style = Stroke(1f))
        }
        AnnotationType.HINT -> {
            // Pascal: RoundRect(0,0, W-1,H-1, F-5,F-5) shadow then RoundRect(0,0, W-3,H-3, …) body.
            drawRoundRect(SHADOW_LIGHT, topLeft = Offset(x, y), size = Size(w - 1f, h - 1f), cornerRadius = hintCorner)
            drawRoundRect(bgColor, topLeft = Offset(x, y), size = Size(w - 3f, h - 3f), cornerRadius = hintCorner)
            drawRoundRect(TEXT_BOX_DARK, topLeft = Offset(x, y), size = Size(w - 3f, h - 3f), cornerRadius = hintCorner, style = Stroke(1f))
        }
        AnnotationType.PLAIN -> {
            // No background drawn (brush.bsClear in original)
        }
    }

    // Normalize Windows line endings (\r\n) and lone carriage returns (\r) to \n so Compose
    // renders line breaks correctly — XML encodes them as &#13; (CR only).
    val displayText = ann.name.replace("\r\n", "\n").replace("\r", "\n")

    // Pascal: Rect adjusted by F (= 2 for HINT, 0 otherwise): Right -= 5+F, Bottom -= 5+F.
    val F = if (ann.annotationType == AnnotationType.HINT) 2f else 0f
    val textArea = Rect(x + 5f, y + 2f, x + w - (5f + F), y + h - (5f + F))
    if (displayText.isNotBlank() && textArea.width > 0 && textArea.height > 0) {
        val align = when (ann.alignment) {
            games.polyclub.power.brmodelo.domain.TextAlignment.LEFT -> TextAlign.Left
            games.polyclub.power.brmodelo.domain.TextAlignment.CENTER -> TextAlign.Center
            games.polyclub.power.brmodelo.domain.TextAlignment.RIGHT -> TextAlign.Right
        }
        // Tighter line height to match Windows GDI Tahoma 8pt spacing (~13 px/line).
        // Compose default (~1.25×fontSize ≈ 13.75 sp) causes text to overflow the
        // box height that Pascal stored via DT_CALCRECT with the original font metrics.
        val layout = textMeasurer.measure(
            displayText,
            style = CANVAS_TEXT_STYLE.copy(textAlign = align, lineHeight = 13.sp),
            constraints = Constraints(maxWidth = textArea.width.toInt().coerceAtLeast(1)),
        )
        drawText(layout, topLeft = Offset(textArea.left, textArea.top))
    }
}

// ── Connections ───────────────────────────────────────────────────────────────

/**
 * Draws only the orthogonal line segments for a connection (no cardinality label).
 *
 * Implements [TLigacao.Ative] routing cases:
 * – Cases 1 & 2: diagonal separation → 2-segment L-shape
 * – Case 3: pure vertical separation → 3-segment V→H→V (Z-shape)
 * – Case 4: pure horizontal separation → 3-segment H→V→H (Z-shape)
 * – Case 5: fallback → 2-segment L-shape
 *
 * Weak connections ([games.polyclub.power.brmodelo.domain.Connection.isWeak]) are drawn with a parallel double line.
 */
private fun DrawScope.drawConnectionLine(
    conn: Connection,
    schema: ConceptualSchema,
    dividedPoints: Map<Int, Map<Int, Offset>>,
    lineHighlightColor: Color? = null,
) {
    val elemA = schema.elements[conn.elementIdA] ?: return
    val elemB = schema.elements[conn.elementIdB] ?: return

    val ptA = dividedPoints[conn.elementIdA]?.get(conn.id) ?: run {
        val enc = connectionEncaixes(elemA, elemB, schema, conn)
        enc[connectionPonto(elemA, elemB, schema, conn)]
    }
    val ptB = dividedPoints[conn.elementIdB]?.get(conn.id) ?: run {
        val enc = connectionEncaixes(elemB, elemA, schema, conn)
        enc[connectionPonto(elemB, elemA, schema, conn)]
    }

    val posA = if (elemA is SchemaElement.AssociativeEntity && elemB !is SchemaElement.Attribute) {
        if (associativeConnectionUsesInnerDiamond(elemA, elemB, conn)) assocInnerDiamondPos(elemA.position)
        else elemA.position
    } else elemA.position
    val posB = if (elemB is SchemaElement.AssociativeEntity && elemA !is SchemaElement.Attribute) {
        if (associativeConnectionUsesInnerDiamond(elemB, elemA, conn)) assocInnerDiamondPos(elemB.position)
        else elemB.position
    } else elemB.position

    val waypoints = computeConnectionPath(
        ptA,
        posA,
        ptB,
        posB,
        conn.orientation
    )
    if (waypoints.size < 2) return

    val strokeColor = lineHighlightColor ?: Color.Black
    for (i in 0 until waypoints.size - 1) {
        val from = waypoints[i]
        val to   = waypoints[i + 1]
        if (conn.isWeak) {
            // Weak connection = 3-pixel-wide solid line, matching TLinha.Paint isWeak:
            //   pixels x=2,3,4 (or y=2,3,4) all black, with a 1-px white gap on one side.
            drawLine(strokeColor, from, to, strokeWidth = 3f)
        } else {
            val width = if (lineHighlightColor != null) 2f else 1f
            drawLine(strokeColor, from, to, strokeWidth = width)
        }
    }
}

/**
 * Label X used when choosing "role (card)" vs "(card) role" for **automatic** layout
 * (same as when [Connection.cardinalityPosition] is null in [drawCardinalityLabel]).
 */
private const val CARDINALITY_AUTO_LAYOUT_LABEL_LEFT_FOR_ROLE = 0

/**
 * Full cardinality label string for drawing and hit-testing (Pascal TCardinalidade.Paint order).
 *
 * @param labelLeftForRoleInversion Compares against [Connection.elementIdB] left edge to pick order.
 */
private fun cardinalityLabelDisplayString(
    conn: Connection,
    schema: ConceptualSchema,
    labelLeftForRoleInversion: Int,
): String? {
    if (!conn.showCardinality || conn.cardinality == null) return null
    val baseLabel = conn.cardinality.label
    if (baseLabel.isBlank()) return null
    return if (conn.cardinalityRole.isNotEmpty()) {
        val pontaLeft = schema.elements[conn.elementIdB]?.position?.x ?: 0
        if (pontaLeft < labelLeftForRoleInversion) "$baseLabel ${conn.cardinalityRole}"
        else "${conn.cardinalityRole} $baseLabel"
    } else {
        baseLabel
    }
}

/**
 * Extra clearance (px) between the auto-placed cardinality label and the entity edge / line,
 * on top of the legacy brModelo offsets.
 */
private const val CARDINALITY_LABEL_AUTO_LAYOUT_OUTSET_PX = 5f
/**
 * Bottom edge uses a smaller outward nudge so the label does not look overly far from the line
 * (the original `CARD_H - 4f` shift reads larger than on other sides).
 */
private const val CARDINALITY_LABEL_AUTO_LAYOUT_OUTSET_BOTTOM_PX = 2f
/** Horizontal nudge (px) for top/bottom attachment so the label sits slightly right of the line. */
private const val CARDINALITY_LABEL_TOP_BOTTOM_LINE_OFFSET_X_PX = 3f

/**
 * Top-left pixel where the cardinality text is placed in **fallback** mode
 * (same geometry as the former no-stored-position branch of [drawCardinalityLabel]).
 */
private fun floatingCardinalityLabelTextTopLeftMeasured(
    schema: ConceptualSchema,
    conn: Connection,
    dividedPoints: Map<Int, Map<Int, Offset>>,
    textMeasurer: TextMeasurer,
    labelLeftForRoleInversion: Int,
): Offset? {
    val elemA = schema.elements[conn.elementIdA] ?: return null
    val elemB = schema.elements[conn.elementIdB] ?: return null
    val cardStr = cardinalityLabelDisplayString(conn, schema, labelLeftForRoleInversion) ?: return null
    val layout = textMeasurer.measure(cardStr, style = MULTIVALUE_CARD_STYLE)
    val entityElem = when {
        elemB is SchemaElement.Entity || elemB is SchemaElement.AssociativeEntity -> elemB
        elemA is SchemaElement.Entity || elemA is SchemaElement.AssociativeEntity -> elemA
        else -> elemB
    }
    val otherForEntity = if (entityElem == elemA) elemB else elemA
    val anchor = dividedPoints[entityElem.id]?.get(conn.id) ?: run {
        val enc = connectionEncaixes(
            entityElem,
            otherForEntity,
            schema,
            conn,
        )
        enc[connectionPonto(
            entityElem,
            otherForEntity,
            schema,
            conn,
        )]
    }
    val p = pointToEdgeIndex(anchor, entityElem.position)
    val lw = layout.size.width.toFloat()
    val CARD_H = 20f
    var aLeft = anchor.x
    var aTop = anchor.y - CARD_H + 5f
    when (p) {
        1 -> {
            aLeft = aLeft - lw + 2f - CARDINALITY_LABEL_AUTO_LAYOUT_OUTSET_PX
        }
        2 -> {
            aTop -= CARDINALITY_LABEL_AUTO_LAYOUT_OUTSET_PX
            aLeft += CARDINALITY_LABEL_TOP_BOTTOM_LINE_OFFSET_X_PX
        }
        3 -> {
            aLeft += CARDINALITY_LABEL_AUTO_LAYOUT_OUTSET_PX
        }
        4 -> {
            aTop = aTop + CARD_H - 4f + CARDINALITY_LABEL_AUTO_LAYOUT_OUTSET_BOTTOM_PX
            aLeft += CARDINALITY_LABEL_TOP_BOTTOM_LINE_OFFSET_X_PX
        }
        else -> Unit
    }
    return Offset(aLeft, aTop)
}

/**
 * Draws the cardinality label for a connection ON TOP of elements.
 *
 * Must be called after elements are drawn so the label appears above them —
 * matching the original Pascal z-order where [TCardinalidade] floats above the canvas.
 *
 * When a stored position is available ([games.polyclub.power.brmodelo.domain.Connection.cardinalityPosition]), it is used
 * directly — these coordinates were produced by [TLigacao.PosicioneCardinalidade] and
 * saved in the XML. A small X adjustment (lw/4) compensates for the width difference
 * between the original 8pt Tahoma and our rendered font.
 */
private fun DrawScope.drawCardinalityLabel(
    conn: Connection,
    schema: ConceptualSchema,
    dividedPoints: Map<Int, Map<Int, Offset>>,
    textMeasurer: TextMeasurer,
) {
    if (!conn.showCardinality || conn.cardinality == null) return
    val baseLabel = conn.cardinality.label
    if (baseLabel.isBlank()) return

    val labelLeftForRoleInversion = conn.cardinalityPosition?.x ?: CARDINALITY_AUTO_LAYOUT_LABEL_LEFT_FOR_ROLE
    val cardStr =
        cardinalityLabelDisplayString(conn, schema, labelLeftForRoleInversion) ?: return

    val elemA = schema.elements[conn.elementIdA] ?: return
    val elemB = schema.elements[conn.elementIdB] ?: return

    val layout = textMeasurer.measure(cardStr, style = MULTIVALUE_CARD_STYLE)

    // Use stored position when available; apply X correction for font-width difference.
    // The stored position was calibrated for the cardinality-only string (e.g. "(1,1)"),
    // so the correction must also be based solely on that part — not the full combined
    // label that may include a role name ("Responsável"), which would over-shift it.
    if (conn.cardinalityPosition != null) {
        val lp = conn.cardinalityPosition
        val cardOnlyLayout = textMeasurer.measure(baseLabel, style = MULTIVALUE_CARD_STYLE)
        val xAdjustment = cardOnlyLayout.size.width / 4f
        val topLeft = Offset(lp.x.toFloat() + xAdjustment, lp.y.toFloat())
        if (!conn.cardinalityAutoSize && lp.width > 0 && lp.height > 0) {
            clipRect(
                left = lp.x.toFloat(),
                top = lp.y.toFloat(),
                right = (lp.x + lp.width).toFloat(),
                bottom = (lp.y + lp.height).toFloat(),
            ) {
                drawText(layout, topLeft = topLeft)
            }
        } else {
            drawText(layout, topLeft = topLeft)
        }
        return
    }

    // Fallback: compute position from the entity-end anchor when no stored coordinates.
    val topLeft = floatingCardinalityLabelTextTopLeftMeasured(
        schema,
        conn,
        dividedPoints,
        textMeasurer,
        labelLeftForRoleInversion,
    ) ?: return
    drawText(layout, topLeft = topLeft)
}

/** Height of the cardinality label hit box in px — matches [CanvasHitTest] / draw fallback. */
internal const val CARDINALITY_LABEL_HIT_HEIGHT_PX = 20f

/** Extra margin so hit area covers anti-aliased edges and font wider than the estimate. */
private const val CARDINALITY_LABEL_HIT_INSET_PX = 6f

private fun expandCardinalityHitRect(r: Rect): Rect = Rect(
    left = r.left - CARDINALITY_LABEL_HIT_INSET_PX,
    top = r.top - CARDINALITY_LABEL_HIT_INSET_PX,
    right = r.right + CARDINALITY_LABEL_HIT_INSET_PX,
    bottom = r.bottom + CARDINALITY_LABEL_HIT_INSET_PX,
)

/**
 * Tight axis-aligned bounds of the cardinality label (same geometry as [drawCardinalityLabel] /
 * hit-test core), **without** the extra padding used for pointer hit-testing.
 */
private fun cardinalityLabelBoundsRectUnpadded(
    schema: ConceptualSchema,
    conn: Connection,
    textMeasurer: TextMeasurer,
): Rect? {
    if (!conn.showCardinality || conn.cardinality == null) return null
    val baseLabel = conn.cardinality.label
    if (baseLabel.isBlank()) return null

    conn.cardinalityPosition?.let { lp ->
        if (!conn.cardinalityAutoSize && lp.width > 0 && lp.height > 0) {
            return Rect(
                left = lp.x.toFloat(),
                top = lp.y.toFloat(),
                right = (lp.x + lp.width).toFloat(),
                bottom = (lp.y + lp.height).toFloat(),
            )
        }
        val cardStr = cardinalityLabelDisplayString(conn, schema, lp.x) ?: return null
        val layout = textMeasurer.measure(cardStr, style = MULTIVALUE_CARD_STYLE)
        val cardOnlyLayout = textMeasurer.measure(baseLabel, style = MULTIVALUE_CARD_STYLE)
        val xAdjust = cardOnlyLayout.size.width / 4f
        val labelWidth = layout.size.width.toFloat()
        val labelHeight = layout.size.height.toFloat().coerceAtLeast(CARDINALITY_LABEL_HIT_HEIGHT_PX)
        return Rect(
            left = lp.x + xAdjust,
            top = lp.y.toFloat(),
            right = lp.x + xAdjust + labelWidth,
            bottom = lp.y + labelHeight,
        )
    }

    val dividedPoints = computeDividedPoints(schema)
    val topLeft = floatingCardinalityLabelTextTopLeftMeasured(
        schema,
        conn,
        dividedPoints,
        textMeasurer,
        CARDINALITY_AUTO_LAYOUT_LABEL_LEFT_FOR_ROLE,
    ) ?: return null
    val cardStr = cardinalityLabelDisplayString(conn, schema, CARDINALITY_AUTO_LAYOUT_LABEL_LEFT_FOR_ROLE)
        ?: return null
    val layout = textMeasurer.measure(cardStr, style = MULTIVALUE_CARD_STYLE)
    val lw = layout.size.width.toFloat()
    val lh = layout.size.height.toFloat().coerceAtLeast(CARDINALITY_LABEL_HIT_HEIGHT_PX)
    return Rect(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + lw,
        bottom = topLeft.y + lh,
    )
}

/**
 * [ElementPosition] for drawing the selection outline and resize handles around a cardinality label.
 *
 * Uses the same bounds as the inspector ([connectionCardinalityBoxForModel]): stored `Left`/`Top`/`Width`/`Height`
 * when present, otherwise the brModelo default box (36×20) anchored from layout. This avoids showing a
 * wide outline driven by the hit-test width estimate while the sidebar still shows 36.
 */
internal fun cardinalityLabelHighlightElementPosition(
    schema: ConceptualSchema,
    conn: Connection,
    textMeasurer: TextMeasurer,
): ElementPosition? {
    val stored = conn.cardinalityPosition
    if (stored != null && stored.width > 0 && stored.height > 0) {
        return stored
    }
    return connectionCardinalityBoxForModel(schema, conn, textMeasurer)
}

/**
 * Persists the current on-canvas cardinality box when the user locks the label ("Fixa").
 * [Connection.cardinalityPosition] stores the label **box** origin (Pascal `Left`/`Top`); drawing
 * applies [xAdjust] for font metrics, so we convert from the computed text-space rect.
 *
 * Uses the same automatic anchor + measured text width as the former no-stored-position draw path.
 */
internal fun materializeCardinalityPositionForFixed(
    schema: ConceptualSchema,
    conn: Connection,
    textMeasurer: TextMeasurer,
): ElementPosition? {
    if (!conn.showCardinality || conn.cardinality == null) return null
    val baseLabel = conn.cardinality?.label ?: return null
    if (baseLabel.isBlank()) return null
    val dividedPoints = computeDividedPoints(schema)
    val topLeft = floatingCardinalityLabelTextTopLeftMeasured(
        schema,
        conn,
        dividedPoints,
        textMeasurer,
        CARDINALITY_AUTO_LAYOUT_LABEL_LEFT_FOR_ROLE,
    ) ?: return null
    val cardOnlyLayout = textMeasurer.measure(baseLabel, style = MULTIVALUE_CARD_STYLE)
    val xAdjustment = cardOnlyLayout.size.width / 4f
    return ElementPosition(
        x = (topLeft.x - xAdjustment).toInt(),
        y = topLeft.y.toInt(),
        width = Connection.DEFAULT_LABEL_WIDTH,
        height = Connection.DEFAULT_LABEL_HEIGHT,
    )
}

/**
 * Label box in model coordinates ([Connection.cardinalityPosition] convention) for inspector and gestures.
 * Materializes from layout when the connection has no stored non-degenerate bounds.
 */
internal fun connectionCardinalityBoxForModel(
    schema: ConceptualSchema,
    conn: Connection,
    textMeasurer: TextMeasurer,
): ElementPosition? {
    val stored = conn.cardinalityPosition
    if (stored != null && stored.width > 0 && stored.height > 0) return stored
    return materializeCardinalityPositionForFixed(schema, conn, textMeasurer)
}

/**
 * Fills [Connection.cardinalityPosition] from layout when the link shows cardinality but has no
 * valid stored box (e.g. immediately after [validateAndBuildConceptualLink]).
 */
internal fun enrichConnectionWithInitialCardinalityPosition(
    schemaIncludingConnection: ConceptualSchema,
    conn: Connection,
    textMeasurer: TextMeasurer,
): Connection {
    if (!conn.showCardinality || conn.cardinality == null) return conn
    val stored = conn.cardinalityPosition
    if (stored != null && stored.width > 0 && stored.height > 0) return conn
    return materializeCardinalityPositionForFixed(schemaIncludingConnection, conn, textMeasurer)?.let { pos ->
        conn.copy(cardinalityPosition = pos)
    } ?: conn
}

/**
 * Recomputes floating (non-fixed) cardinality label positions from the current element layout.
 * When [onlyIncidentToElementId] is non-null, only connections touching that element are updated.
 */
internal fun ConceptualSchema.withRecalculatedFloatingCardinalityPositions(
    onlyIncidentToElementId: Int? = null,
    textMeasurer: TextMeasurer,
): ConceptualSchema {
    val s = this
    return copy(
        connections = connections.map { conn ->
            when {
                !conn.showCardinality || conn.cardinality == null || conn.cardinalityFixed ->
                    conn
                onlyIncidentToElementId != null &&
                    conn.elementIdA != onlyIncidentToElementId &&
                    conn.elementIdB != onlyIncidentToElementId ->
                    conn
                else ->
                    materializeCardinalityPositionForFixed(s, conn, textMeasurer)?.let { pos ->
                        conn.copy(cardinalityPosition = pos)
                    } ?: conn
            }
        },
    )
}

/**
 * After translating one or more elements by ([dx],[dy]) in model space, updates cardinality labels:
 * - **Floating** ([Connection.cardinalityFixed] false): recomputed from geometry via
 *   [materializeCardinalityPositionForFixed] when the link touches a moved element or [conn.id] is in
 *   [selectedCardinalityConnectionIds].
 * - **Fixed**: the stored label box is shifted by ([dx],[dy]) when the link touches a moved element
 *   or the label connection is among [selectedCardinalityConnectionIds].
 */
internal fun ConceptualSchema.withCardinalityPositionsAfterElementsMovedByDelta(
    movedElementIds: Set<Int>,
    dx: Int,
    dy: Int,
    selectedCardinalityConnectionIds: Set<Int>,
    textMeasurer: TextMeasurer,
): ConceptualSchema {
    if (movedElementIds.isEmpty() && selectedCardinalityConnectionIds.isEmpty()) return this
    val s = this

    if (movedElementIds.isEmpty()) {
        return copy(
            connections = connections.map { conn ->
                if (!conn.showCardinality || conn.cardinality == null) return@map conn
                if (conn.id !in selectedCardinalityConnectionIds) return@map conn
                val base = conn.cardinalityPosition
                    ?: materializeCardinalityPositionForFixed(s, conn, textMeasurer)
                    ?: return@map conn
                val shifted = base.copy(x = base.x + dx, y = base.y + dy)
                conn.copy(cardinalityPosition = shifted.coercedToMinimumDimensions())
            },
        )
    }

    return copy(
        connections = connections.map { conn ->
            if (!conn.showCardinality || conn.cardinality == null) return@map conn
            val touchesMoved =
                conn.elementIdA in movedElementIds || conn.elementIdB in movedElementIds
            val selectedCard = conn.id in selectedCardinalityConnectionIds

            if (!conn.cardinalityFixed) {
                if (selectedCard || touchesMoved) {
                    return@map materializeCardinalityPositionForFixed(s, conn, textMeasurer)?.let { pos ->
                        conn.copy(cardinalityPosition = pos)
                    } ?: conn
                }
                return@map conn
            }

            val p = conn.cardinalityPosition ?: return@map conn
            if (touchesMoved || selectedCard) {
                return@map conn.copy(
                    cardinalityPosition = p.copy(x = p.x + dx, y = p.y + dy).coercedToMinimumDimensions(),
                )
            }
            conn
        },
    )
}

/**
 * After an element's bounds were updated in the schema (inspector or MCP `edit__canvas_element` position patch),
 * updates cardinality labels the same way as canvas drag / keyboard nudge: translate floating labels from
 * geometry when the element moved; shift fixed labels by the same delta when the link touches the element;
 * re-materialize floating labels when width/height changed (resize-like).
 */
internal fun ConceptualSchema.afterCardinalitySyncForElementBoundsChange(
    elementId: Int,
    previousPosition: ElementPosition,
    textMeasurer: TextMeasurer,
): ConceptualSchema {
    val el = elements[elementId] ?: return this
    val newPos = el.position
    val dx = newPos.x - previousPosition.x
    val dy = newPos.y - previousPosition.y
    val sizeChanged =
        newPos.width != previousPosition.width || newPos.height != previousPosition.height
    var s = this
    if (dx != 0 || dy != 0) {
        s = s.withCardinalityPositionsAfterElementsMovedByDelta(
            movedElementIds = setOf(elementId),
            dx = dx,
            dy = dy,
            selectedCardinalityConnectionIds = emptySet(),
            textMeasurer = textMeasurer,
        )
    }
    if (sizeChanged) {
        s = s.withRecalculatedFloatingCardinalityPositions(
            onlyIncidentToElementId = elementId,
            textMeasurer = textMeasurer,
        )
    }
    return s
}

/**
 * Axis-aligned bounds for hit-testing a connection's cardinality label.
 * When [Connection.cardinalityPosition] is null, uses the same fallback placement as
 * [drawCardinalityLabel] so labels from new links (or legacy data without stored coords)
 * remain clickable.
 */
internal fun cardinalityLabelInteractionRect(
    schema: ConceptualSchema,
    conn: Connection,
    textMeasurer: TextMeasurer,
): Rect? {
    val core = cardinalityLabelBoundsRectUnpadded(schema, conn, textMeasurer) ?: return null
    return expandCardinalityHitRect(core)
}

// ── Helper: assoc entity inner diamond ───────────────────────────────────────

/**
 * Returns the position of the inner diamond of an [AssociativeEntity], inset 15 px
 * on all sides — mirrors [TEntidadeAssoss.SetBounds] (`InflateRect -15`).
 */
private fun assocInnerDiamondPos(p: ElementPosition) =
    ElementPosition(
        x = p.x + 15,
        y = p.y + 15,
        width = (p.width - 30).coerceAtLeast(10),
        height = (p.height - 30).coerceAtLeast(10),
    )

/**
 * When [elem] is an [SchemaElement.AssociativeEntity] connected to a non-attribute [otherElem],
 * returns whether line routing uses the inner relationship diamond (`true`) or the outer entity
 * rectangle (`false`). Non-associative elements are not affected by this helper.
 */
private fun associativeConnectionUsesInnerDiamond(
    elem: SchemaElement,
    otherElem: SchemaElement,
    conn: Connection?,
): Boolean {
    if (elem !is SchemaElement.AssociativeEntity || otherElem is SchemaElement.Attribute) return true
    if (conn == null) return true
    return when (elem.id) {
        conn.elementIdA -> !conn.useAssociativeOuterForEndA
        conn.elementIdB -> !conn.useAssociativeOuterForEndB
        else -> true
    }
}

// ── Helper: per-connection encaixe points ─────────────────────────────────────

/**
 * Returns the connection-specific attachment encaixe array for [elem] given
 * that it is connected to [otherElem].
 *
 * Differences from a plain 4-edge array:
 * - **Attribute (normal)**: all four slots collapse to the "active" side —
 *   left for OrientacaoE (attribute to the right of owner), right for OrientacaoD.
 *   Matches [TAtributo.AtualizaEncaixes] from mer.pas.
 * - **Composite attribute (bar side)**: when [otherElem] is a child attribute of
 *   [elem], the four slots collapse to the opposite (bar) side instead.
 *   Mirrors [TBarraDeAtributos.PrepareToAtive] behaviour.
 * - **AssociativeEntity → non-Attribute**: uses the inner diamond's edge midpoints by default;
 *   when the connection stores an outer-body attach flag for this end, uses the outer rectangle edges.
 * - **All other elements**: standard four edge midpoints [1..4].
 *
 * Index mapping: [1]=left, [2]=top, [3]=right, [4]=bottom (1-based, Pascal compatible).
 */
private fun connectionEncaixes(
    elem: SchemaElement,
    otherElem: SchemaElement,
    schema: ConceptualSchema,
    conn: Connection? = null,
): Array<Offset> {
    val p = elem.position
    val left   = p.x.toFloat()
    val top    = p.y.toFloat()
    val right  = left + p.width
    val bottom = top  + p.height
    val cx     = left + p.width  / 2f
    val cy     = top  + p.height / 2f

    if (elem is SchemaElement.Attribute) {
        val ownerPos = schema.elements[elem.ownerId]?.position
        // P≠1 → OrientacaoE → ellipse on LEFT (active left edge connects to owner)
        val ellipseOnLeft = ownerPos?.let { conceptualAttributeAttachPonto(
            it,
            p
        ) != 1 } ?: false

        return if (otherElem is SchemaElement.Attribute && otherElem.ownerId == elem.id) {
            // Connection toward a child attribute → use bar side (opposite of normal)
            val bar = if (ellipseOnLeft) Offset(right, cy) else Offset(left, cy)
            arrayOf(Offset.Zero, bar, bar, bar, bar)
        } else {
            // Normal attribute connection toward its owner
            val c = if (ellipseOnLeft) Offset(left, cy) else Offset(right, cy)
            arrayOf(Offset.Zero, c, c, c, c)
        }
    }

    // Specialization (TEspecializacao) doesn't use the centred bounding-box edges. Pascal
    // computes `FalsasBases` (Redesenhe, mer.pas ~8643) and PrepareToAtive (~8700) selects
    // ONE of those four points based on the connected element's relative left and whether
    // it's the EntidadeBase. The 4 false-bases live at the triangle's *corners* (not edge
    // centres), and a different point is chosen depending on:
    //   - Posi: POSI_ABAIXO (Esp.Top > base.Top) ⇒ apex on top, base on bottom (default).
    //   - Posi: POSI_ACIMA ⇒ flipped: apex on bottom, base on top.
    //   - Connected entity is the EntidadeBase ⇒ snap on FalsasBases[2] (apex centre).
    //   - Type=Optional or Ponta.Left == base.Left ⇒ FalsasBases[4] (base centre).
    //   - Ponta.Left < base.Left ⇒ FalsasBases[1] (base-left corner).
    //   - Else ⇒ FalsasBases[3] (base-right corner).
    if (elem is SchemaElement.Specialization) {
        return specializationEncaixes(
            elem,
            otherElem,
            schema
        )
    }

    // AssociativeEntity → non-Attribute: inner diamond encaixes unless this connection uses the outer body.
    if (elem is SchemaElement.AssociativeEntity && otherElem !is SchemaElement.Attribute) {
        if (associativeConnectionUsesInnerDiamond(elem, otherElem, conn)) {
            val ip = assocInnerDiamondPos(p)
            val il = ip.x.toFloat(); val it_ = ip.y.toFloat()
            val ir = (ip.x + ip.width).toFloat(); val ib_ = (ip.y + ip.height).toFloat()
            val icx = (il + ir) / 2f; val icy = (it_ + ib_) / 2f
            return arrayOf(Offset.Zero, Offset(il, icy), Offset(icx, it_), Offset(ir, icy), Offset(icx, ib_))
        }
        return arrayOf(
            Offset.Zero,
            Offset(left,  cy),
            Offset(cx,    top),
            Offset(right, cy),
            Offset(cx,    bottom),
        )
    }

    return arrayOf(
        Offset.Zero,
        Offset(left,  cy),     // [1] left center
        Offset(cx,    top),    // [2] top center
        Offset(right, cy),     // [3] right center
        Offset(cx,    bottom), // [4] bottom center
    )
}

/**
 * Returns the four `FalsasBases` snap points of a [Specialization] at indices 1..4,
 * matching `TEspecializacao.Redesenhe` (mer.pas ~8643) and `PrepareToAtive` (~8700).
 *
 * The triangle has the apex at the top (when Esp is BELOW its base entity, i.e. POSI_ABAIXO)
 * or at the bottom (POSI_ACIMA). FalsasBases identifies the apex centre, the base centre,
 * and the two base corners. PrepareToAtive then picks one of these based on which entity
 * the line connects to and that entity's left position relative to the base.
 *
 * All four returned offsets are equal (Pascal collapses Encaixe[1..4] to a single point per
 * connection), so callers indexing by the chosen `ponto` always retrieve the right snap.
 */
private fun specializationEncaixes(
    spec: SchemaElement.Specialization,
    otherElem: SchemaElement,
    schema: ConceptualSchema,
): Array<Offset> {
    val p = spec.position
    val left = p.x.toFloat()
    val top  = p.y.toFloat()
    // Pascal `Redesenhe` (mer.pas ~8643) uses INTEGER division:
    //   meio := aLeft + ((aWidth-3) div 2)
    //   H    := aTop  + (aHeight-3)
    //   W    := aLeft + (aWidth-3)
    // Replicated exactly so connection lines align pixel-perfect with parent/child
    // entity centres (which themselves use `Left + Width div 2` integer arithmetic).
    val meio = left + ((p.width - 3) / 2).toFloat()
    val h    = top  + (p.height - 3).toFloat()
    val w    = left + (p.width  - 3).toFloat()

    val baseEntity = schema.elements[spec.baseEntityId]
    val isAcima = baseEntity != null && p.y < baseEntity.position.y    // POSI_ACIMA when Esp.Top < base.Top
    // FalsasBases[1..4] for POSI_ABAIXO (default): apex top, base bottom
    //   [1] base-left, [2] apex centre (top), [3] base-right, [4] base centre (bottom)
    // FalsasBases[1..4] for POSI_ACIMA: apex bottom, base top — flipped
    //   [1] base-left (top), [2] apex centre (bottom), [3] base-right (top), [4] base centre (top)
    val falsa1: Offset; val falsa2: Offset; val falsa3: Offset; val falsa4: Offset
    if (isAcima) {
        falsa2 = Offset(meio, h)
        falsa4 = Offset(meio, top)
        falsa1 = Offset(left, top)
        falsa3 = Offset(w, top)
    } else {
        falsa2 = Offset(meio, top)
        falsa4 = Offset(meio, h)
        falsa1 = Offset(left, h)
        falsa3 = Offset(w, h)
    }

    // PrepareToAtive (mer.pas ~8700) selects ONE FalsasBase based on the connected entity:
    //   - Ponta == EntidadeBase           → FalsasBases[2] (apex)
    //   - Type=Optional or Ponta.Left=base.Left → FalsasBases[4] (base centre)
    //   - Ponta.Left < base.Left          → FalsasBases[1] (base-left)
    //   - else                            → FalsasBases[3] (base-right)
    val pick: Offset = run {
        val baseLeft = baseEntity?.position?.x ?: p.x
        when {
            otherElem.id == spec.baseEntityId               -> falsa2
            spec.type == games.polyclub.power.brmodelo.domain.SpecializationType.OPTIONAL ||
                otherElem.position.x == baseLeft            -> falsa4
            otherElem.position.x < baseLeft                 -> falsa1
            else                                            -> falsa3
        }
    }
    return arrayOf(Offset.Zero, pick, pick, pick, pick)
}

/**
 * Returns the 1-based index (1=LEFT, 2=TOP, 3=RIGHT, 4=BOTTOM) of the diamond vertex
 * closest to [attrPos]'s centre, by Euclidean distance.
 *
 * Used for diamond → attribute connections so the chosen ponto matches the *visually*
 * nearest vertex even for attributes whose bounding box overlaps the diamond (where
 * Pascal's `TLigacao.Ative` case-5 fallback would pick a different quadrant by walking
 * `Left/Top` orderings instead of geometric distance).
 */
private fun diamondNearestVertex(diamondPos: ElementPosition, attrPos: ElementPosition): Int {
    val cx = diamondPos.x + diamondPos.width / 2f
    val cy = diamondPos.y + diamondPos.height / 2f
    val w  = diamondPos.width.toFloat()
    val h  = diamondPos.height.toFloat()
    val ax = attrPos.x + attrPos.width / 2f
    val ay = attrPos.y + attrPos.height / 2f
    fun d2(vx: Float, vy: Float): Float {
        val dx = ax - vx; val dy = ay - vy; return dx * dx + dy * dy
    }
    val dLeft   = d2(cx - w / 2f, cy)
    val dTop    = d2(cx, cy - h / 2f)
    val dRight  = d2(cx + w / 2f, cy)
    val dBottom = d2(cx, cy + h / 2f)
    val minD = minOf(dLeft, dTop, dRight, dBottom)
    return when (minD) {
        dLeft   -> 1
        dTop    -> 2
        dRight  -> 3
        else    -> 4
    }
}

/**
 * Computes the boundary attachment point of a diamond ([Relationship] / [SelfRelationship])
 * for an attribute connection on `ponto` (1=LEFT, 2=TOP, 3=RIGHT, 4=BOTTOM).
 *
 * Mirrors `TBaseRelacao.PrepareToAtive` in `mer.pas` (~line 6672):
 *
 * 1. Start at the centre of the chosen edge — the diamond's vertex.
 * 2. Shift the perpendicular coordinate by ±`atDes` (= attribute's `Desvio`, default 10),
 *    moving away from the vertex along the bounding-box edge.
 * 3. Snap the parallel coordinate onto the diamond's diagonal boundary (Pascal's
 *    `arruma` helper iteratively walks one pixel at a time until `PtInRegion` is true;
 *    we solve it analytically using the diamond equation `|dx|/hw + |dy|/hh = 1`).
 *
 * The result is a point on one of the diamond's diagonal edges, near the vertex of `ponto`.
 *
 * Index mapping:
 * - 1 (LEFT): on the upper-left edge → `(left + atDes*w/h, cy - atDes)`
 * - 2 (TOP):  on the upper-right edge → `(cx + atDes, top + atDes*h/w)`
 * - 3 (RIGHT):on the lower-right edge → `(left + w - atDes*w/h, cy + atDes)`
 * - 4 (BOTTOM):on the lower-right edge → `(cx + atDes, top + h - atDes*h/w)`
 *
 * The (1, +/-Y) and (3, +/-Y) cases use the upper-left and lower-right diagonals respectively,
 * exactly matching the `case 1: Encaixe[1].Y -= atDes; …` / `case 3: Encaixe[3].Y += atDes; …`
 * branches in the Pascal source.
 */
/**
 * Returns the point on a diamond's diagonal boundary that intersects the attribute's
 * centre line (cy for ponto 1/3, cx for ponto 2/4), or `null` when the projection falls
 * outside the diamond's range for the requested ponto.
 *
 * Used as an alternative to [games.polyclub.power.brmodelo.ui.canvas.diamondAttrEncaixe] for attributes whose bounding box
 * overlaps the diamond's bounding box: the original brModelo Pascal binary draws those
 * connections as a single straight line at the attribute's centre (no Z routing), which
 * implies the diamond's snap point is the actual intersection of the diagonal with the
 * attribute's `cy`/`cx` (not the `Encaixe[ponto] + Desvio` produced by `PrepareToAtive`).
 *
 * The diamond equation `|x-cx|/hw + |y-cy|/hh = 1` defines its border; for a given y in
 * `[top, bottom]` the border x on the upper-left side is `cx - hw*(1 - |y-cy|/hh)`.
 */
private fun projectAttrOnDiamondBorder(
    diamondPos: ElementPosition,
    ponto: Int,
    attrPos: ElementPosition,
): Offset? {
    val left   = diamondPos.x.toFloat()
    val top    = diamondPos.y.toFloat()
    val w      = diamondPos.width.toFloat()
    val h      = diamondPos.height.toFloat()
    val cx     = left + w / 2f
    val cy     = top  + h / 2f
    val right  = left + w
    val bottom = top  + h
    val ax     = attrPos.x + attrPos.width  / 2f
    val ay     = attrPos.y + attrPos.height / 2f

    return when (ponto) {
        1 -> if (ay in top..bottom) {
            val rel  = abs(ay - cy) / (h / 2f)
            val xOff = (w / 2f) * rel
            Offset(left + xOff, ay)
        } else null
        3 -> if (ay in top..bottom) {
            val rel  = abs(ay - cy) / (h / 2f)
            val xOff = (w / 2f) * rel
            Offset(right - xOff, ay)
        } else null
        2 -> if (ax in left..right) {
            val rel  = abs(ax - cx) / (w / 2f)
            val yOff = (h / 2f) * rel
            Offset(ax, top + yOff)
        } else null
        4 -> if (ax in left..right) {
            val rel  = abs(ax - cx) / (w / 2f)
            val yOff = (h / 2f) * rel
            Offset(ax, bottom - yOff)
        } else null
        else -> null
    }
}

private fun diamondAttrEncaixe(
    diamondPos: ElementPosition,
    ponto: Int,
    desvio: Int,
): Offset {
    val left = diamondPos.x.toFloat()
    val top  = diamondPos.y.toFloat()
    val w    = diamondPos.width.toFloat()
    val h    = diamondPos.height.toFloat()
    val cx   = left + w / 2f
    val cy   = top  + h / 2f
    val d    = desvio.toFloat()
    return when (ponto) {
        1 -> Offset(left + d * w / h,         cy - d)              // upper-left  edge
        2 -> Offset(cx + d,                   top + d * h / w)     // upper-right edge
        3 -> Offset(left + w - d * w / h,     cy + d)              // lower-right edge
        4 -> Offset(cx + d,                   top + h - d * h / w) // lower-right edge
        else -> Offset(cx, cy)
    }
}

/** Returns the 1-based encaixe index (1..4) whose point is nearest to [other]'s center. */
private fun nearestEncaixeIndex(enc: Array<Offset>, other: ElementPosition): Int {
    val cx = other.x + other.width  / 2f
    val cy = other.y + other.height / 2f
    var best = 1
    var bestDist = Float.MAX_VALUE
    for (i in 1..4) {
        val dx = enc[i].x - cx
        val dy = enc[i].y - cy
        val d  = dx * dx + dy * dy
        if (d < bestDist) { bestDist = d; best = i }
    }
    return best
}

/**
 * Returns which edge index (1=left, 2=top, 3=right, 4=bottom) of [pos] the
 * point [pt] is closest to. Used to determine [P] in PosicioneCardinalidade.
 */
private fun pointToEdgeIndex(pt: Offset, pos: ElementPosition): Int {
    val dLeft   = abs(pt.x - pos.x.toFloat())
    val dTop    = abs(pt.y - pos.y.toFloat())
    val dRight  = abs(pt.x - (pos.x + pos.width).toFloat())
    val dBottom = abs(pt.y - (pos.y + pos.height).toFloat())
    val minD = minOf(dLeft, dTop, dRight, dBottom)
    return when (minD) {
        dLeft   -> 1
        dTop    -> 2
        dRight  -> 3
        else    -> 4
    }
}

// ── Helper: connection ponto ──────────────────────────────────────────────────

/**
 * Returns the 1-based encaixe index (1=left, 2=top, 3=right, 4=bottom) that
 * [elem] uses to connect to [otherElem].
 *
 * Unlike [games.polyclub.power.brmodelo.ui.canvas.nearestEncaixeIndex], this function handles collapsed attribute
 * encaixes correctly: for normal attributes ponto = 1 (OrientacaoE) or 3
 * (OrientacaoD); for composite → child connections the bar side is used
 * (opposite of the normal connection side).
 *
 * For non-attribute elements, delegates to [games.polyclub.power.brmodelo.ui.canvas.computeNonAttrPonto] which
 * faithfully mirrors the [TLigacao.Ative] case selection from mer.pas.
 */
private fun connectionPonto(
    elem: SchemaElement,
    otherElem: SchemaElement,
    schema: ConceptualSchema,
    conn: Connection? = null,
): Int {
    if (elem is SchemaElement.Attribute) {
        val ownerPos = schema.elements[elem.ownerId]?.position
        // P≠1 → OrientacaoE → ellipse on LEFT (active left edge connects to owner)
        val ellipseOnLeft = ownerPos?.let { conceptualAttributeAttachPonto(
            it,
            elem.position
        ) != 1 } ?: false
        return if (otherElem is SchemaElement.Attribute && otherElem.ownerId == elem.id) {
            if (ellipseOnLeft) 3 else 1  // bar side (right for OrientacaoE)
        } else {
            if (ellipseOnLeft) 1 else 3  // normal side (left for OrientacaoE)
        }
    }
    // For a non-attribute element connecting TO an attribute, use the full `TLigacao.Ative`
    // case selection. The XML stores the attribute as the `<Ligacao>` owner (BaseInicial = E1),
    // so the entity is E2 → isE1 = (conn.elementIdA == elem.id). Falls back to position-based
    // quadrant when no Connection is provided (used by deprecated callers).
    if (otherElem is SchemaElement.Attribute) {
        if (conn != null) {
            val isE1 = conn.elementIdA == elem.id
            return computeNonAttrPonto(
                elem.position,
                otherElem.position,
                conn.orientation,
                isE1
            )
        }
        return conceptualAttributeAttachPonto(
            elem.position,
            otherElem.position
        )
    }
    // For non-attribute elements, use the routing-aware ponto that matches Ative's
    // case conditions. isE1 = whether this element is elementIdA (the "source" in the XML).
    // AssociativeEntity uses the inner diamond position for ponto computation when routing
    // through the inner relationship; otherwise the full outer bounds are used.
    if (conn != null) {
        val isE1 = conn.elementIdA == elem.id
        val effectivePos = if (elem is SchemaElement.AssociativeEntity && otherElem !is SchemaElement.Attribute) {
            if (associativeConnectionUsesInnerDiamond(elem, otherElem, conn)) assocInnerDiamondPos(elem.position)
            else elem.position
        } else elem.position
        val effectiveOtherPos = if (otherElem is SchemaElement.AssociativeEntity && elem !is SchemaElement.Attribute) {
            if (associativeConnectionUsesInnerDiamond(otherElem, elem, conn)) assocInnerDiamondPos(otherElem.position)
            else otherElem.position
        } else otherElem.position
        return computeNonAttrPonto(
            effectivePos,
            effectiveOtherPos,
            conn.orientation,
            isE1
        )
    }
    return nearestEncaixeIndex(
        connectionEncaixes(
            elem,
            otherElem,
            schema,
            null,
        ), otherElem.position
    )
}

/**
 * Computes the 1-based encaixe ponto for a non-attribute element in a connection,
 * following the exact case selection from [TLigacao.Ative] in mer.pas (lines 7035–7174).
 *
 * [isE1] = true if [elemPos] corresponds to E1 (elementIdA = the element owning the
 * `<Ligacao>` XML node), false if it is E2 (elementIdB = Destino_ID).
 *
 * Cases (checked in priority order, matching Pascal):
 * – 1 (diagonal top-left → bottom-right, ≥ 20 px gap in BOTH dims):
 *     orientation V → pE1=4 (bottom), pE2=1 (left)
 *     orientation H → pE1=3 (right),  pE2=2 (top)
 * – 2 (cross-diagonal bottom-left ↔ top-right, ≥ 20 px gap in BOTH dims):
 *     orientation H → pE1=2 (top),    pE2=1 (left)
 *     orientation V → pE1=3 (right),  pE2=4 (bottom)
 * – 3 (pure vertical separation > 4 px): E1-above → pE1=4, pE2=2.
 * – 4 (pure horizontal separation > 4 px): E1-left → pE1=3, pE2=1.
 * – 5 (fallback): orientation-based selection derived from relative Left/Top.
 */
private fun computeNonAttrPonto(
    elemPos: ElementPosition,
    otherPos: ElementPosition,
    orientation: games.polyclub.power.brmodelo.domain.LineOrientation,
    isE1: Boolean,
): Int {
    // Always view from the E1 perspective for consistent comparisons.
    val e1Pos = if (isE1) elemPos else otherPos
    val e2Pos = if (isE1) otherPos else elemPos

    val e1r = e1Pos.x + e1Pos.width
    val e1b = e1Pos.y + e1Pos.height
    val e2r = e2Pos.x + e2Pos.width
    val e2b = e2Pos.y + e2Pos.height

    val isH = orientation == games.polyclub.power.brmodelo.domain.LineOrientation.HORIZONTAL
    val DIST = 20

    // Case 1: E1 is to the top-left of E2 by ≥ DIST in BOTH dimensions, or vice-versa.
    // Pascal: checks E1.Encaixe[3].X (=right) and E1.Encaixe[4].Y (=bottom) vs E2.Left/Top.
    // When the reverse is true (E2 top-left of E1), Pascal swaps E1↔E2 before Mapa.
    val c1fwd = e1r < e2Pos.x - DIST && e1b < e2Pos.y - DIST  // E1 top-left of E2
    val c1rev = e2r < e1Pos.x - DIST && e2b < e1Pos.y - DIST  // E2 top-left of E1
    if (c1fwd || c1rev) {
        // After potential swap: "actualE1" = the top-left element.
        val swapped = c1rev
        val actualIsE1 = if (swapped) !isE1 else isE1
        return if (!isH) {
            if (actualIsE1) 4 else 1   // OrientacaoV: top-left elem uses BOTTOM, bottom-right uses LEFT
        } else {
            if (actualIsE1) 3 else 2   // OrientacaoH: top-left uses RIGHT, bottom-right uses TOP
        }
    }

    // Case 2: E1 bottom-left ↔ E2 top-right (or reversed) by ≥ DIST in BOTH dimensions.
    // Pascal: E1.right < E2.left-DIST AND E2.bottom < E1.top-DIST (E1 bottom, E2 top)
    // OR E2.right < E1.left-DIST AND E1.bottom < E2.top-DIST (reversed).
    val c2fwd = e1r < e2Pos.x - DIST && e2b < e1Pos.y - DIST  // E1 bottom-left, E2 top-right
    val c2rev = e2r < e1Pos.x - DIST && e1b < e2Pos.y - DIST  // E2 bottom-left, E1 top-right
    if (c2fwd || c2rev) {
        // After potential swap: "actualE1" = the bottom-left element.
        val swapped = c2rev
        val actualIsE1 = if (swapped) !isE1 else isE1
        return if (isH) {
            if (actualIsE1) 2 else 1   // OrientacaoH: bottom-left uses TOP, top-right uses LEFT
        } else {
            if (actualIsE1) 3 else 4   // OrientacaoV: bottom-left uses RIGHT, top-right uses BOTTOM
        }
    }

    // Case 3: pure vertical separation (> 4 px gap)
    if (e1b < e2Pos.y - 4) return if (isE1) 4 else 2   // E1 above E2 — no swap
    if (e2b < e1Pos.y - 4) return if (isE1) 2 else 4   // E2 above E1 — swap in original

    // Case 4: pure horizontal separation (> 4 px gap)
    if (e1r < e2Pos.x - 4) return if (isE1) 3 else 1   // E1 left of E2 — no swap
    if (e2r < e1Pos.x - 4) return if (isE1) 1 else 3   // E2 left of E1 — swap in original

    // Case 5 fallback — relative-position selection from mer.pas lines 7139–7174
    return if (isH) {
        if (isE1) { if (e1Pos.x <= e2Pos.x) 3 else 1 }
        else      { if (e1Pos.y <= e2Pos.y) 2 else 4 }
    } else {
        if (isE1) { if (e1Pos.y <= e2Pos.y) 4 else 2 }
        else      { if (e1Pos.x <= e2Pos.x) 1 else 3 }
    }
}

// ── Helper: Divida pre-computation ───────────────────────────────────────────

/**
 * Pre-computes per-element, per-connection attachment points.
 *
 * **Attribute connections** — The attachment Y is derived directly from the
 * attribute's stored `cy` (centre-Y), which is already the exact value that
 * [TBase.OrganizeAtributos] would produce. Using `cy` avoids the floating-point
 * drift that arises when replicating the integer-arithmetic Divida algorithm.
 *
 * For the child–composite-attribute case, the child's `cy` is used for both
 * ends so that the connecting line stays perfectly horizontal.
 *
 * **Non-attribute connections** — [TBase.Divida] is applied: when N > 1
 * connections share the same edge, the attachment points are evenly spaced:
 * `tam = edgeLength / (N + 1)`, position[i] = anchorStart + tam * (i+1).
 *
 * Returns `Map<elemId, Map<connId, Offset>>`.
 */
private fun computeDividedPoints(schema: ConceptualSchema): Map<Int, Map<Int, Offset>> {
    val result = mutableMapOf<Int, MutableMap<Int, Offset>>()

    for ((elemId, elem) in schema.elements) {
        val elemResult = result.getOrPut(elemId) { mutableMapOf() }
        // All non-attribute → non-attribute (or non-diamond → attribute) connections are
        // collected here, grouped by `ponto`, so a single Divida pass can space them evenly
        // along the chosen edge of `elem`. Diamond → attribute connections are handled in
        // the inline branch below (Pascal does not apply Divida to TBaseRelacao).
        val byPonto = mutableMapOf<Int, MutableList<DividaSlot>>()

        for (conn in schema.connections) {
            val isA = conn.elementIdA == elemId
            val isB = conn.elementIdB == elemId
            if (!isA && !isB) continue
            val otherId = if (isA) conn.elementIdB else conn.elementIdA
            val otherElem = schema.elements[otherId] ?: continue

            if (otherElem is SchemaElement.Attribute) {
                // This element connects TO an attribute.
                if (elem is SchemaElement.Attribute && elem.ownerId == otherId) {
                    // Child → composite bar connection.
                    // The composite (otherElem) connects to its entity owner via some ponto;
                    // its bar is on the OPPOSITE side. Children hang off the bar.
                    val compositeOwnerPos = schema.elements[otherElem.ownerId]?.position
                    val compPonto = if (compositeOwnerPos != null)
                        conceptualAttributeAttachPonto(
                            compositeOwnerPos,
                            otherElem.position
                        ) else 3
                    // Bar side: RIGHT (3) for OrientacaoE (compPonto≠1), LEFT (1) for OrientacaoD
                    val barIsOnRight = compPonto != 1
                    // Child connects FROM the bar side: if bar on right → child is to the right → child LEFT
                    val p = elem.position
                    val childActiveX = if (barIsOnRight) p.x.toFloat() else (p.x + p.width).toFloat()
                    elemResult[conn.id] = Offset(childActiveX, p.y + p.height / 2f)
                } else if (elem is SchemaElement.Attribute && otherElem.ownerId == elem.id) {
                    // Composite parent → child attribute: snap on the bar edge of the composite,
                    // at the child's centre Y so the connection draws as a single horizontal line.
                    // Children of a composite never participate in `Divida` (the bar is a stub,
                    // not an actual edge) — bypass the byPonto queue.
                    val parentOwnerPos = schema.elements[elem.ownerId]?.position
                    val compPonto = if (parentOwnerPos != null)
                        conceptualAttributeAttachPonto(
                            parentOwnerPos,
                            elem.position
                        ) else 3
                    val barIsOnRight = compPonto != 1
                    val ep = elem.position
                    val barX = if (barIsOnRight) (ep.x + ep.width).toFloat() else ep.x.toFloat()
                    val childCy = otherElem.position.y + otherElem.position.height / 2f
                    elemResult[conn.id] = Offset(barX, childCy)
                } else {
                    // Normal entity/relationship → attribute.
                    // For diamonds we pick the *closest vertex* to the attribute's centre
                    // (Euclidean distance). Empirically this matches the original brModelo
                    // rendering on overlapping attributes (e.g. Atributo2 in altamente-
                    // personalizado.xml) where the bounding-box-based `Ative` case-5 fallback
                    // would pick a far-side vertex but the real Pascal renderer uses a
                    // boundary point near the *closest* vertex. For non-diamond owners the
                    // standard `Ative` case selection (via [connectionPonto]) is used.
                    val isDiamond = elem is SchemaElement.Relationship ||
                                    elem is SchemaElement.SelfRelationship
                    val ponto = if (isDiamond)
                        diamondNearestVertex(
                            elem.position,
                            otherElem.position
                        )
                    else
                        connectionPonto(
                            elem,
                            otherElem,
                            schema,
                            conn
                        )
                    if (isDiamond) {
                        // Pascal `TBaseRelacao.PrepareToAtive` (mer.pas ~6672) shifts the
                        // diamond's `Encaixe[ponto]` by ±`Desvio` along the perpendicular axis,
                        // then snaps the parallel axis onto the diamond's boundary via the
                        // `arruma` helper. The resulting point lives on the diagonal edge near
                        // the chosen vertex — NOT at the vertex itself. TBaseRelacao does NOT
                        // participate in Divida, so each attribute uses its own `Desvio`.
                        //
                        // Special case: when the attribute's bounding box OVERLAPS the diamond
                        // (e.g. `funcaoGrat` in `MER-PousadaSolDaManha.xml`), the attribute is
                        // sitting close enough to the diamond border that the original brModelo
                        // routes the line *along the attribute's own centre* directly to where
                        // the diamond's diagonal crosses that y/x. This produces the perfectly
                        // straight line we observe in the Pascal binary's render. Falls back
                        // to the standard `PrepareToAtive` formula for non-overlapping attrs.
                        val ap = otherElem.position
                        val dp = elem.position
                        val overlaps = ap.x < dp.x + dp.width && ap.x + ap.width > dp.x &&
                                       ap.y < dp.y + dp.height && ap.y + ap.height > dp.y
                        elemResult[conn.id] = if (overlaps) {
                            projectAttrOnDiamondBorder(
                                dp,
                                ponto,
                                ap
                            )
                                ?: diamondAttrEncaixe(
                                    dp,
                                    ponto,
                                    otherElem.deviationAngle
                                )
                        } else {
                            diamondAttrEncaixe(
                                dp,
                                ponto,
                                otherElem.deviationAngle
                            )
                        }
                    } else {
                        // Entity/Table: queue for Divida along with non-attr connections so
                        // that all ligações on this `ponto` are evenly spaced (matches Pascal's
                        // TBase.Divida + TLigacao.Ative behaviour from OnBaseMoved).
                        byPonto.getOrPut(ponto) { mutableListOf() }
                            .add(
                                DividaSlot(
                                    conn.id,
                                    otherElem,
                                    isAttribute = true
                                )
                            )
                    }
                }
            } else if (elem is SchemaElement.Attribute) {
                // This IS an attribute connecting to a non-attribute owner.
                // OrganizeAtributos rule:
                //   owner P=1 → OrientacaoD → attr RIGHT active edge
                //   owner P≠1 → OrientacaoE → attr LEFT  active edge
                // Reuse the same `Ative`-driven `Mapa` selection so the orientation
                // matches the entity-side computation above (avoids inconsistent ponto
                // pairs that would mis-route the connection).
                val entityPonto = connectionPonto(
                    otherElem,
                    elem,
                    schema,
                    conn
                )
                val p = elem.position
                val attrActiveX = if (entityPonto == 1) (p.x + p.width).toFloat() else p.x.toFloat()
                elemResult[conn.id] = Offset(attrActiveX, p.y + p.height / 2f)
            } else {
                // Non-attribute → non-attribute: queue for Divida.
                val ponto = connectionPonto(
                    elem,
                    otherElem,
                    schema,
                    conn
                )
                byPonto.getOrPut(ponto) { mutableListOf() }
                    .add(
                        DividaSlot(
                            conn.id,
                            otherElem,
                            isAttribute = false
                        )
                    )
            }
        }

        // Divida distribution: for each `ponto`, evenly space all queued connections
        // along the corresponding edge of [elem]. Replicates `TBase.Divida` + `TLigacao.Ative`
        // (mer.pas ~1614, ~7011) which override `Encaixe[ponto]` with `start + tam*cont`
        // before each [Ative] call when QuantosNestePonto > 1.
        for ((ponto, slots) in byPonto) {
            val firstOther = slots.first().otherElem
            val sampleConnId = slots.first().connId
            val sampleConn = schema.connections.firstOrNull { it.id == sampleConnId }
            val enc =
                connectionEncaixes(elem, firstOther, schema, sampleConn)
            val pos = if (elem is SchemaElement.AssociativeEntity && firstOther !is SchemaElement.Attribute) {
                if (associativeConnectionUsesInnerDiamond(elem, firstOther, sampleConn)) {
                    assocInnerDiamondPos(elem.position)
                } else {
                    elem.position
                }
            } else {
                elem.position
            }
            val (anchorStart, edgeLen) = if (ponto == 1 || ponto == 3) {
                pos.y.toFloat() to pos.height.toFloat()
            } else {
                pos.x.toFloat() to pos.width.toFloat()
            }

            // SelfRelationship with exactly 2 non-attribute legs on the same ponto:
            // use Pascal's TAutoRelacao.PrepareToAtive / triagulo formula, which places
            // snap points on the diamond's diagonal edge rather than at the central vertex.
            val nonAttrSlots = slots.filter { !it.isAttribute }
            if (elem is SchemaElement.SelfRelationship &&
                nonAttrSlots.size == 2 && nonAttrSlots.size == slots.size) {
                val pairs = nonAttrSlots.map { it.connId to it.otherElem }
                applyAutoRelacaoTriagulo(
                    schema,
                    elem,
                    ponto,
                    pairs,
                    elemResult
                )
                continue
            }

            val nTotal = slots.size
            if (nTotal == 1) {
                // Single connection on this edge: use the unmodified central Encaixe (Pascal
                // skips Divida and calls AtivePorPonto, which uses the original Encaixe).
                elemResult[slots[0].connId] = enc[ponto]
                continue
            }

            // Primary sort: Pascal's OrdenadorLeftNegativo / OrdenadorTop rank by raw `Top`
            // (for ponto 1, 3) or `Left` (for ponto 2, 4) of the OTHER element — NOT the
            // centre. This matters when widths/heights vary, otherwise pairs like
            // (telefone Left=217, W=94, cx=264) and (pix Left=227, W=41, cx=247.5) end up
            // swapped vs the Pascal layout, dragging their snaps onto the wrong Divida slot.
            // Secondary: SelfRelationship legs use Pascal `Menor` order (`Ponta.FLigacoes.IndexOf`,
            // i.e. XML order) so diamond sides stay paired with the correct cardinality labels.
            val sameSelfRelDiamond = nonAttrSlots.size == 2 && slots.size == 2 &&
                nonAttrSlots[0].otherElem.id == nonAttrSlots[1].otherElem.id &&
                nonAttrSlots[0].otherElem is SchemaElement.SelfRelationship
            val diamondId = if (sameSelfRelDiamond) nonAttrSlots[0].otherElem.id else -1
            val sorted = slots.sortedWith(
                compareBy(
                    { slot ->
                        val op = slot.otherElem.position
                        when (ponto) {
                            1, 3 -> op.y          // OrdenadorLeftNegativo: rank by Top
                            else -> op.x          // OrdenadorTop:           rank by Left
                        }
                    },
                    { slot ->
                        if (sameSelfRelDiamond)
                            autoRelLegRank(
                                schema,
                                diamondId,
                                elem.id,
                                slot.connId
                            )
                        else slot.connId
                    },
                ),
            )

            // Pascal uses integer division for `tam` (`Height div (qtd+1)` / `Width div (qtd+1)`).
            // Mirror that to keep snap coordinates pixel-aligned identical to the original.
            val tam = (edgeLen.toInt()) / (nTotal + 1)

            for ((idx, slot) in sorted.withIndex()) {
                val coord = anchorStart + (tam * (idx + 1)).toFloat()
                val pt = when (ponto) {
                    1 -> Offset(pos.x.toFloat(), coord)
                    3 -> Offset((pos.x + pos.width).toFloat(), coord)
                    2 -> Offset(coord, pos.y.toFloat())
                    else /* 4 */ -> Offset(coord, (pos.y + pos.height).toFloat())
                }
                elemResult[slot.connId] = pt
            }
        }
    }
    return result
}

/**
 * One queued connection participating in a single [TBase.Divida] pass for some `ponto`.
 *
 * @param connId       Stable schema connection id (used as the result map key).
 * @param otherElem    The element on the OTHER side of the connection. Its centre coordinate
 *                     drives the sort order along the chosen edge.
 * @param isAttribute  True if [otherElem] is a [games.polyclub.power.brmodelo.domain.SchemaElement.Attribute]. Pascal counts attrs
 *                     in `QuantosNestePonto` exactly like any other ligação, so this is only
 *                     informational (e.g. for the SelfRelationship triagulo special-case).
 */
private data class DividaSlot(
    val connId: Int,
    val otherElem: SchemaElement,
    val isAttribute: Boolean,
)

/**
 * Rank of [connId] among legs from [diamondId] to [entityId] in [games.polyclub.power.brmodelo.domain.ConceptualSchema.connections]
 * order (XML / parse order). Matches `Ponta.FLigacoes.IndexOf` for `TLigacao` pairs that share
 * the same entity end in `TAutoRelacao.PrepareToAtive`'s `Menor` logic (`mer.pas` ~7763–7766).
 */
private fun autoRelLegRank(schema: ConceptualSchema, diamondId: Int, entityId: Int, connId: Int): Int {
    val legs = schema.connections.withIndex()
        .filter { (_, c) -> c.elementIdA == diamondId && c.elementIdB == entityId }
        .sortedBy { it.index }
    val i = legs.indexOfFirst { it.value.id == connId }
    return if (i >= 0) i else connId
}

/**
 * Computes the two snap points for a [SelfRelationship] diamond when it has exactly
 * two connections to the same entity on the same [ponto].
 *
 * Ports [TAutoRelacao.PrepareToAtive] from `mer.pas` (line 7760).
 *
 * Key insight: Pascal's `nVl = Lg.Ponta.LastTamanhoOrigem.X div 2` is **not** the entity's
 * half-width — it is `tam div 2` where `tam = entityEdgeLength / (nConnections + 1)` (the
 * Divida step for the entity's connecting edge, stored in `LastTamanhoOrigem.X` just before
 * `PrepareToAtive` is called).  Using the entity edge step places the snap points on the
 * diamond's lower-diagonal edges, matching the original visual.
 *
 * [Menor] = the leg that appears first in XML parse order (mirrors `Ponta.FLigacoes.IndexOf`),
 * receives the negative offset (−nVl); the other leg gets +nVl.
 */
private fun applyAutoRelacaoTriagulo(
    schema: ConceptualSchema,
    selfRel: SchemaElement.SelfRelationship,
    ponto: Int,
    pairs: List<Pair<Int, SchemaElement>>,
    elemResult: MutableMap<Int, Offset>,
) {
    if (pairs.size != 2) return

    val dp = selfRel.position
    val halfW = dp.width / 2f
    val halfH = dp.height / 2f
    val cx = dp.x + halfW
    val cy = dp.y + halfH

    val entity = pairs.first().second

    // Determine entity's ponto for these connections (used for edge-length and P-flip below).
    // Find it from the actual connection object to correctly handle all Cases.
    val connForPonto = schema.connections.firstOrNull { it.id == pairs.first().first }
    val entityPonto: Int = if (connForPonto != null)
        connectionPonto(
            entity,
            selfRel,
            schema,
            connForPonto
        )
    else when (ponto) { 4 -> 2; 2 -> 4; 1 -> 3; 3 -> 1; else -> 2 }

    // Count non-attribute connections at the entity's edge to compute the Divida step tam.
    // Pascal: LastTamanhoOrigem.X = tam = entityEdgeLen / (nConn + 1)
    var nEntityConn = 0
    for (conn in schema.connections) {
        val invA = conn.elementIdA == entity.id
        val invB = conn.elementIdB == entity.id
        if (!invA && !invB) continue
        val otherId = if (invA) conn.elementIdB else conn.elementIdA
        val otherElem = schema.elements[otherId] ?: continue
        if (otherElem is SchemaElement.Attribute) continue
        if (connectionPonto(
                entity,
                otherElem,
                schema,
                conn
            ) == entityPonto) nEntityConn++
    }
    if (nEntityConn == 0) return

    val entityEdgeLen = if (entityPonto == 1 || entityPonto == 3) entity.position.height
                        else entity.position.width
    val tam = entityEdgeLen / (nEntityConn + 1)  // integer division, matches Pascal `div`
    var nVl = tam / 2
    if (nVl == 0) return

    // Pascal: if (nVl * 2) > Width (for L/R) or Height (for T/B) then nVl := dim div 2
    val diamondDim = if (ponto == 1 || ponto == 3) dp.height else dp.width
    if (nVl * 2 > diamondDim) nVl = diamondDim / 2

    // Pascal P-flip: for certain combos of AutoRelacao ponto and entity ponto, the sign
    // is additionally reversed (e.g. ponto=4 "if p=1 then nVl:=-nVl").
    val flipSign = when (ponto) {
        4 -> entityPonto == 1
        2 -> entityPonto == 3
        1 -> entityPonto == 4
        3 -> entityPonto == 2
        else -> false
    }

    // Triagulo: right-triangle formula places the snap on the diagonal edge.
    val A = if (ponto == 1 || ponto == 3) halfW else halfH
    val B = if (ponto == 1 || ponto == 3) halfH else halfW
    val C = sqrt(A * A + B * B)
    val aa = nVl.toFloat()
    val xVal = C * aa / B
    val T = floor(sqrt(xVal * xVal - aa * aa))

    // Menor = first leg in XML/parse order (matches Ponta.FLigacoes.IndexOf in Pascal).
    val entityId = entity.id
    val sorted = pairs.sortedBy {
        autoRelLegRank(
            schema,
            selfRel.id,
            entityId,
            it.first
        )
    }

    for ((idx, pair) in sorted.withIndex()) {
        var sign = if (idx == 0) -1f else 1f
        if (flipSign) sign = -sign
        val nVlF = nVl.toFloat()
        val snap = when (ponto) {
            4 -> Offset(cx + sign * nVlF, dp.y + dp.height - T)
            2 -> Offset(cx + sign * nVlF, dp.y + T)
            1 -> Offset(dp.x + T, cy + sign * nVlF)
            3 -> Offset(dp.x + dp.width - T, cy + sign * nVlF)
            else -> Offset(cx, cy)
        }
        elemResult[pair.first] = snap
    }
}

// ── Helper: orthogonal routing (TLigacao.Ative) ───────────────────────────────

/**
 * Computes waypoints for an orthogonal connection between pre-computed attachment
 * points [ptA] and [ptB] (already Divida-adjusted).
 *
 * Uses element bounding boxes [posA]/[posB] for the case checks, faithfully
 * matching [TLigacao.Ative] from mer.pas:
 * - Cases 1 & 2: diagonal 20 px separation → L-shape
 * - Case 3: pure vertical separation (> 4 px) → Z-shape V→H→V
 * - Case 4: pure horizontal separation (> 4 px) → Z-shape H→V→H
 * - Case 5: fallback → L-shape
 */
private fun computeConnectionPath(
    ptA: Offset, posA: ElementPosition,
    ptB: Offset, posB: ElementPosition,
    orientation: games.polyclub.power.brmodelo.domain.LineOrientation,
): List<Offset> {
    var pt1 = ptA; var e1 = posA
    var pt2 = ptB; var e2 = posB

    val DIST = 20f
    val isH = orientation == games.polyclub.power.brmodelo.domain.LineOrientation.HORIZONTAL

    val e1r = (e1.x + e1.width).toFloat()
    val e2r = (e2.x + e2.width).toFloat()
    val e1b = (e1.y + e1.height).toFloat()
    val e2b = (e2.y + e2.height).toFloat()

    // ── Case 1: E1 top-left of E2 (both dims separated by ≥ DIST) ───────────
    val c1fwd = e1r < e2.x - DIST && e1b < e2.y - DIST
    val c1rev = e2r < e1.x - DIST && e2b < e1.y - DIST
    if (c1fwd || c1rev) {
        if (c1rev) { val tp = pt1; pt1 = pt2; pt2 = tp; val te = e1; e1 = e2; e2 = te }
        return if (!isH) listOf(pt1, Offset(pt1.x, pt2.y), pt2)
               else      listOf(pt1, Offset(pt2.x, pt1.y), pt2)
    }

    // ── Case 2: E1 bottom-left of E2 diagonally ──────────────────────────────
    val c2fwd = e1r < e2.x - DIST && e2b < e1.y - DIST
    val c2rev = e2r < e1.x - DIST && e1b < e2.y - DIST
    if (c2fwd || c2rev) {
        if (c2rev) { val tp = pt1; pt1 = pt2; pt2 = tp; val te = e1; e1 = e2; e2 = te }
        return if (isH) listOf(pt1, Offset(pt1.x, pt2.y), pt2)
               else     listOf(pt1, Offset(pt2.x, pt1.y), pt2)
    }

    // ── Case 3: Pure vertical separation (> 4 px gap) ────────────────────────
    if (e1b < e2.y - 4 || e2b < e1.y - 4) {
        if (e2b < e1.y - 4) { val tp = pt1; pt1 = pt2; pt2 = tp }
        val midY = pt2.y - (pt2.y - pt1.y) / 2f
        return listOf(pt1, Offset(pt1.x, midY), Offset(pt2.x, midY), pt2)
    }

    // ── Case 4: Pure horizontal separation (> 4 px gap) ──────────────────────
    if (e1r < e2.x - 4 || e2r < e1.x - 4) {
        if (e2r < e1.x - 4) { val tp = pt1; pt1 = pt2; pt2 = tp }
        val midX = pt2.x - (pt2.x - pt1.x) / 2f
        return listOf(pt1, Offset(midX, pt1.y), Offset(midX, pt2.y), pt2)
    }

    // ── Case 5: Fallback L-shape ──────────────────────────────────────────────
    return listOf(pt1, Offset(pt2.x, pt1.y), pt2)
}

// ── Helper: centred label ─────────────────────────────────────────────────────

/**
 * Draws [text] centred (horizontally and vertically) within [bounds].
 * Mirrors [TBase.Paint]'s DT_CENTER + vertical centering logic.
 */
private fun DrawScope.drawCenteredLabel(
    text: String,
    bounds: ElementPosition,
    textMeasurer: TextMeasurer,
    bold: Boolean = false,
    italic: Boolean = false,
) {
    if (text.isBlank()) return
    val x = bounds.x.toFloat()
    val y = bounds.y.toFloat()
    val w = bounds.width.toFloat()
    val h = bounds.height.toFloat()

    val style = CANVAS_TEXT_STYLE.copy(
        textAlign = TextAlign.Center,
        fontWeight = if (bold) FontWeight.Black else null,
        fontStyle = if (italic) FontStyle.Italic else null,
    )
    val maxW = (w - 4f).toInt().coerceAtLeast(1)
    val layout = textMeasurer.measure(text, style = style, constraints = Constraints(maxWidth = maxW))
    val textX = x + (w - layout.size.width) / 2f
    val textY = y + (h - layout.size.height) / 2f
    drawText(layout, topLeft = Offset(textX, textY))
}

