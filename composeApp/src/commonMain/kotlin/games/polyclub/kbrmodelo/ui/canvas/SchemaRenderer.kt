/*
 * KbrModelo - Kotlin port of brModelo 3.0 originally written in Pascal
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

package games.polyclub.kbrmodelo.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import games.polyclub.kbrmodelo.domain.AnnotationType
import games.polyclub.kbrmodelo.domain.ArrowDirection
import games.polyclub.kbrmodelo.domain.Connection
import games.polyclub.kbrmodelo.domain.ConceptualSchema
import games.polyclub.kbrmodelo.domain.ElementPosition
import games.polyclub.kbrmodelo.domain.SchemaElement
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.PI

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

// ── Main entry point ──────────────────────────────────────────────────────────

/**
 * Draws the full [ConceptualSchema] into this [DrawScope].
 *
 * Rendering order mirrors VCL z-order (back → front):
 * 1. Non-assoc connection lines — behind all elements.
 * 2. All elements (entities, relationships, attributes, AssociativeEntity outer+inner).
 * 3. AssociativeEntity connection lines — redrawn on top of outer rect white fill,
 *    faithfully replicating the VCL behaviour where [TLinha] components have a
 *    higher z-order than [TEntidadeAssoss]/[TChildRelacao].
 * 4. Inner diamonds of AssociativeEntity — redrawn on top of those connection lines.
 * 5. Cardinality labels — floating on top of everything.
 */
fun DrawScope.drawSchema(schema: ConceptualSchema, textMeasurer: TextMeasurer) {
    val dividedPoints = computeDividedPoints(schema)

    // 1. Connection lines that do NOT involve an AssociativeEntity
    schema.connections.forEach { conn ->
        val a = schema.elements[conn.elementIdA]
        val b = schema.elements[conn.elementIdB]
        if (a !is SchemaElement.AssociativeEntity && b !is SchemaElement.AssociativeEntity) {
            drawConnectionLine(conn, schema, dividedPoints)
        }
    }
    // 2. All elements (including AssociativeEntity outer rect + inner diamond)
    schema.elements.values.forEach { element ->
        drawElement(element, schema, textMeasurer)
    }
    // 3. Re-draw connection lines that involve an AssociativeEntity, now on top of the
    //    outer rect white fill — only entity/relationship connections (not attributes),
    //    since attribute stubs are handled visually by the attribute's own rendering.
    schema.connections.forEach { conn ->
        val a = schema.elements[conn.elementIdA]
        val b = schema.elements[conn.elementIdB]
        if (a is SchemaElement.AssociativeEntity || b is SchemaElement.AssociativeEntity) {
            drawConnectionLine(conn, schema, dividedPoints)
        }
    }
    // 4. Re-draw the inner diamonds on top of the connection lines so that the diamond
    //    outline remains visible above lines that enter the outer rect area.
    schema.elements.values.filterIsInstance<SchemaElement.AssociativeEntity>().forEach { assoc ->
        drawRelationshipDiamond(
            assocInnerDiamondPos(assoc.position),
            assoc.relationshipName,
            showName = true,
            textMeasurer,
        )
    }
    // 5. Cardinality labels on top
    schema.connections.forEach { conn ->
        drawCardinalityLabel(conn, schema, dividedPoints, textMeasurer)
    }
}

// ── Element dispatch ──────────────────────────────────────────────────────────

private fun DrawScope.drawElement(
    element: SchemaElement,
    schema: ConceptualSchema,
    textMeasurer: TextMeasurer,
) {
    when (element) {
        is SchemaElement.Entity -> drawEntity(element, textMeasurer)
        is SchemaElement.Relationship -> drawRelationship(element, textMeasurer)
        is SchemaElement.AssociativeEntity -> drawAssociativeEntity(element, textMeasurer)
        is SchemaElement.Attribute -> drawAttribute(element, schema, textMeasurer)
        is SchemaElement.Specialization -> drawSpecialization(element, textMeasurer)
        is SchemaElement.SelfRelationship -> drawRelationshipDiamond(element.position, element.name, showName = true, textMeasurer)
        is SchemaElement.Annotation -> drawAnnotation(element, textMeasurer)
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
 * relative to its owner element, since [SchemaElement.Attribute] stores coordinates
 * but not the VCL [Orientacao] enum value (computed dynamically in the original).
 *
 * - Identifier attributes: ellipse filled with [IDENTIFIER_FILL] (#963636).
 * - Multi-valued: cardinality string appended to label.
 * - Composite: blue asterisk (*) drawn at the appropriate corner.
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
        attrPontoByPosition(owner.position, p) != 1
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
    val ellipseStroke = Stroke(1f)

    if (ellipseOnLeft) {
        // Stub: short horizontal line from left edge to x+5
        drawLine(Color.Black, Offset(x, y + meio), Offset(x + 5f, y + meio))

        // Ellipse at (x+5, y, x+5+diameter, y+diameter)
        val ellipseTopLeft = Offset(x + 5f, y)
        drawOval(ellipseFill, topLeft = ellipseTopLeft, size = Size(diameter, diameter))
        drawOval(Color.Black, topLeft = ellipseTopLeft, size = Size(diameter, diameter), style = ellipseStroke)

        // Composite marker: small 4x8 rectangle at right edge
        if (attr.isComposite) {
            drawRect(Color.Black, topLeft = Offset(x + w - 4f, (h - 4f) / 2f + y), size = Size(4f, 8f))
        }

        // Text to the right of the ellipse
        val textX = x + h + 5f
        val textMaxW = (w - h - 5f).toInt().coerceAtLeast(1)
        if (textLabel.isNotBlank() && textMaxW > 0) {
            val layout = textMeasurer.measure(
                textLabel,
                style = CANVAS_TEXT_STYLE,
                constraints = Constraints(maxWidth = textMaxW),
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

        // Composite marker: small rectangle at left edge
        if (attr.isComposite) {
            drawRect(Color.Black, topLeft = Offset(x, (h - 4f) / 2f + y), size = Size(4f, 8f))
        }

        // Text to the left of the ellipse
        val textMaxW = (w - diameter - 10f).toInt().coerceAtLeast(1)
        if (textLabel.isNotBlank() && textMaxW > 0) {
            val layout = textMeasurer.measure(
                textLabel,
                style = CANVAS_TEXT_STYLE.copy(textAlign = TextAlign.Right),
                constraints = Constraints(maxWidth = textMaxW),
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
 * - 'p' label inside (italic+bold, colour [SPEC_LABEL_COLOR]) when [isPartial]
 *
 * In the original, [FalsasBases] stores the 3 computed vertices, derived from the
 * specialisation's position and width/height. We reconstruct them here from the
 * stored [ElementPosition].
 */
private fun DrawScope.drawSpecialization(spec: SchemaElement.Specialization, textMeasurer: TextMeasurer) {
    val p = spec.position
    val x = p.x.toFloat()
    val y = p.y.toFloat()
    val w = p.width.toFloat()
    val h = p.height.toFloat()

    // Triangle vertices: top-center, bottom-right, bottom-left
    // (matching typical IS-A triangle orientation: apex on top → base entity)
    val path = Path().apply {
        moveTo(x + w / 2f, y)
        lineTo(x + w - 1f, y + h - 1f)
        lineTo(x, y + h - 1f)
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
 * - [AnnotationType.PLAIN]: transparent background, text only
 * - [AnnotationType.HINT]: rounded-rect with shadow
 * - [AnnotationType.BOX]: rectangle with shadow (3D raised look)
 */
private fun DrawScope.drawAnnotation(ann: SchemaElement.Annotation, textMeasurer: TextMeasurer) {
    val p = ann.position
    val x = p.x.toFloat()
    val y = p.y.toFloat()
    val w = p.width.toFloat()
    val h = p.height.toFloat()

    // Resolve background colour (COLORREF BGR → RGB conversion)
    val bgColor = if (ann.color != null) colorRefToCompose(ann.color) else Color(0xFF87CEEB) // clSkyBlue

    when (ann.annotationType) {
        AnnotationType.BOX -> {
            // Shadow: filled rect at +2 offset
            drawRect(SHADOW_LIGHT, topLeft = Offset(x + 2f, y + 2f), size = Size(w - 1f, h - 1f))
            // Background rect
            drawRect(bgColor, topLeft = Offset(x, y), size = Size(w - 3f, h - 3f))
            drawRect(TEXT_BOX_DARK, topLeft = Offset(x, y), size = Size(w - 3f, h - 3f), style = Stroke(1f))
        }
        AnnotationType.HINT -> {
            // Rounded rect shadow + background
            drawRoundRect(SHADOW_LIGHT, topLeft = Offset(x, y), size = Size(w - 1f, h - 1f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(0f))
            drawRoundRect(bgColor, topLeft = Offset(x, y), size = Size(w - 3f, h - 3f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(0f))
            drawRoundRect(TEXT_BOX_DARK, topLeft = Offset(x, y), size = Size(w - 3f, h - 3f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(0f), style = Stroke(1f))
        }
        AnnotationType.PLAIN -> {
            // No background drawn (brush.bsClear in original)
        }
    }

    // Text inside with 5px horizontal margin, 2px top margin
    val textArea = Rect(x + 5f, y + 2f, x + w - 5f, y + h - 5f)
    if (ann.name.isNotBlank() && textArea.width > 0 && textArea.height > 0) {
        val align = when (ann.alignment) {
            games.polyclub.kbrmodelo.domain.TextAlignment.LEFT -> TextAlign.Left
            games.polyclub.kbrmodelo.domain.TextAlignment.CENTER -> TextAlign.Center
            games.polyclub.kbrmodelo.domain.TextAlignment.RIGHT -> TextAlign.Right
        }
        val layout = textMeasurer.measure(
            ann.name,
            style = CANVAS_TEXT_STYLE.copy(textAlign = align),
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
 * Weak connections ([Connection.isWeak]) are drawn with a parallel double line.
 */
private fun DrawScope.drawConnectionLine(
    conn: Connection,
    schema: ConceptualSchema,
    dividedPoints: Map<Int, Map<Int, Offset>>,
) {
    val elemA = schema.elements[conn.elementIdA] ?: return
    val elemB = schema.elements[conn.elementIdB] ?: return

    val ptA = dividedPoints[conn.elementIdA]?.get(conn.id) ?: run {
        val enc = connectionEncaixes(elemA, elemB, schema)
        enc[connectionPonto(elemA, elemB, schema, conn)]
    }
    val ptB = dividedPoints[conn.elementIdB]?.get(conn.id) ?: run {
        val enc = connectionEncaixes(elemB, elemA, schema)
        enc[connectionPonto(elemB, elemA, schema, conn)]
    }

    // Use inner-diamond position for AssociativeEntity routing so lines go to/from
    // TChildRelacao's actual visual boundary, not the outer rect.
    val posA = if (elemA is SchemaElement.AssociativeEntity && elemB !is SchemaElement.Attribute)
        assocInnerDiamondPos(elemA.position) else elemA.position
    val posB = if (elemB is SchemaElement.AssociativeEntity && elemA !is SchemaElement.Attribute)
        assocInnerDiamondPos(elemB.position) else elemB.position

    val waypoints = computeConnectionPath(ptA, posA, ptB, posB, conn.orientation)
    if (waypoints.size < 2) return

    for (i in 0 until waypoints.size - 1) {
        val from = waypoints[i]
        val to   = waypoints[i + 1]
        if (conn.isWeak) {
            // Weak connection = 3-pixel-wide solid line, matching TLinha.Paint isWeak:
            //   pixels x=2,3,4 (or y=2,3,4) all black, with a 1-px white gap on one side.
            drawLine(Color.Black, from, to, strokeWidth = 3f)
        } else {
            drawLine(Color.Black, from, to)
        }
    }
}

/**
 * Draws the cardinality label for a connection ON TOP of elements.
 *
 * Must be called after elements are drawn so the label appears above them —
 * matching the original Pascal z-order where [TCardinalidade] floats above the canvas.
 *
 * When a stored position is available ([Connection.cardinalityPosition]), it is used
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
    val cardStr = conn.cardinality.label
    if (cardStr.isBlank()) return

    val elemA = schema.elements[conn.elementIdA] ?: return
    val elemB = schema.elements[conn.elementIdB] ?: return

    val layout = textMeasurer.measure(cardStr, style = MULTIVALUE_CARD_STYLE)

    // Use stored position when available; apply X correction for font-width difference.
    if (conn.cardinalityPosition != null) {
        val lp = conn.cardinalityPosition
        val xAdjustment = layout.size.width / 4f
        drawText(layout, topLeft = Offset(lp.x.toFloat() + xAdjustment, lp.y.toFloat()))
        return
    }

    // Fallback: compute position from the entity-end anchor when no stored coordinates.
    val entityElem = when {
        elemB is SchemaElement.Entity || elemB is SchemaElement.AssociativeEntity -> elemB
        elemA is SchemaElement.Entity || elemA is SchemaElement.AssociativeEntity -> elemA
        else -> elemB
    }
    val otherForEntity = if (entityElem == elemA) elemB else elemA
    val anchor = dividedPoints[entityElem.id]?.get(conn.id) ?: run {
        val enc = connectionEncaixes(entityElem, otherForEntity, schema)
        enc[connectionPonto(entityElem, otherForEntity, schema)]
    }
    val p = pointToEdgeIndex(anchor, entityElem.position)
    val lw = layout.size.width.toFloat()
    val CARD_H = 20f
    var aLeft = anchor.x
    var aTop  = anchor.y - CARD_H + 5f
    when (p) {
        1 -> aLeft = aLeft - lw + 2f
        4 -> aTop  = aTop + CARD_H - 4f
    }
    drawText(layout, topLeft = Offset(aLeft, aTop))
}

// ── Helper: assoc entity inner diamond ───────────────────────────────────────

/**
 * Returns the position of the inner diamond of an [AssociativeEntity], inset 15 px
 * on all sides — mirrors [TEntidadeAssoss.SetBounds] (`InflateRect -15`).
 */
private fun assocInnerDiamondPos(p: ElementPosition) = ElementPosition(
    x      = p.x + 15,
    y      = p.y + 15,
    width  = (p.width  - 30).coerceAtLeast(10),
    height = (p.height - 30).coerceAtLeast(10),
)

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
 * - **AssociativeEntity → non-Attribute**: uses the inner diamond's edge midpoints,
 *   matching [TChildRelacao]'s actual connection points in mer.pas.
 * - **All other elements**: standard four edge midpoints [1..4].
 *
 * Index mapping: [1]=left, [2]=top, [3]=right, [4]=bottom (1-based, Pascal compatible).
 */
private fun connectionEncaixes(
    elem: SchemaElement,
    otherElem: SchemaElement,
    schema: ConceptualSchema,
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
        val ellipseOnLeft = ownerPos?.let { attrPontoByPosition(it, p) != 1 } ?: false

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

    // AssociativeEntity connecting to a non-Attribute uses the inner diamond's edge
    // midpoints (TChildRelacao handles those connections in the original).
    if (elem is SchemaElement.AssociativeEntity && otherElem !is SchemaElement.Attribute) {
        val ip = assocInnerDiamondPos(p)
        val il = ip.x.toFloat(); val it_ = ip.y.toFloat()
        val ir = (ip.x + ip.width).toFloat(); val ib_ = (ip.y + ip.height).toFloat()
        val icx = (il + ir) / 2f; val icy = (it_ + ib_) / 2f
        return arrayOf(Offset.Zero, Offset(il, icy), Offset(icx, it_), Offset(ir, icy), Offset(icx, ib_))
    }

    return arrayOf(
        Offset.Zero,
        Offset(left,  cy),     // [1] left center
        Offset(cx,    top),    // [2] top center
        Offset(right, cy),     // [3] right center
        Offset(cx,    bottom), // [4] bottom center
    )
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
 * Unlike [nearestEncaixeIndex], this function handles collapsed attribute
 * encaixes correctly: for normal attributes ponto = 1 (OrientacaoE) or 3
 * (OrientacaoD); for composite → child connections the bar side is used
 * (opposite of the normal connection side).
 *
 * For non-attribute elements, delegates to [computeNonAttrPonto] which
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
        val ellipseOnLeft = ownerPos?.let { attrPontoByPosition(it, elem.position) != 1 } ?: false
        return if (otherElem is SchemaElement.Attribute && otherElem.ownerId == elem.id) {
            if (ellipseOnLeft) 3 else 1  // bar side (right for OrientacaoE)
        } else {
            if (ellipseOnLeft) 1 else 3  // normal side (left for OrientacaoE)
        }
    }
    // For a non-attribute element connecting TO an attribute: use position-based
    // quadrant assignment matching TBase.OrganizeAtributos (stored positions).
    if (otherElem is SchemaElement.Attribute) {
        return attrPontoByPosition(elem.position, otherElem.position)
    }
    // For non-attribute elements, use the routing-aware ponto that matches Ative's
    // case conditions. isE1 = whether this element is elementIdA (the "source" in the XML).
    // AssociativeEntity uses the inner diamond position for ponto computation.
    if (conn != null) {
        val isE1 = conn.elementIdA == elem.id
        val effectivePos = if (elem is SchemaElement.AssociativeEntity && otherElem !is SchemaElement.Attribute)
            assocInnerDiamondPos(elem.position) else elem.position
        val effectiveOtherPos = if (otherElem is SchemaElement.AssociativeEntity && elem !is SchemaElement.Attribute)
            assocInnerDiamondPos(otherElem.position) else otherElem.position
        return computeNonAttrPonto(effectivePos, effectiveOtherPos, conn.orientation, isE1)
    }
    return nearestEncaixeIndex(connectionEncaixes(elem, otherElem, schema), otherElem.position)
}

/**
 * Determines which edge (1=LEFT, 2=TOP, 3=RIGHT, 4=BOTTOM) of [elemPos] connects
 * to an attribute at [attrPos], using the **stored positions** that OrganizeAtributos
 * already baked in:
 * - attribute entirely to the left  → P=1
 * - attribute entirely to the right → P=3
 * - attribute entirely above        → P=2
 * - attribute entirely below        → P=4
 * - overlapping (unusual) → falls back to [angleBasedPonto]
 *
 * This is more reliable than pure angle-based classification for diagonal corners
 * like a TOP attribute whose center X is slightly to the right of the entity's
 * right edge (angle just above −45°).
 */
private fun attrPontoByPosition(elemPos: ElementPosition, attrPos: ElementPosition): Int {
    val eLeft   = elemPos.x.toFloat()
    val eRight  = (elemPos.x + elemPos.width).toFloat()
    val eTop    = elemPos.y.toFloat()
    val eBottom = (elemPos.y + elemPos.height).toFloat()
    val aLeft   = attrPos.x.toFloat()
    val aRight  = (attrPos.x + attrPos.width).toFloat()
    val aTop    = attrPos.y.toFloat()
    val aBottom = (attrPos.y + attrPos.height).toFloat()
    return when {
        aRight <= eLeft   -> 1  // attribute entirely to the LEFT of entity
        aLeft  >= eRight  -> 3  // attribute entirely to the RIGHT of entity
        aBottom <= eTop   -> 2  // attribute entirely ABOVE entity
        aTop   >= eBottom -> 4  // attribute entirely BELOW entity
        else -> angleBasedPonto(elemPos, attrPos)  // overlapping – fallback
    }
}

/**
 * Angle-based quadrant fallback for [attrPontoByPosition]. Uses [atan2] from
 * [pos] center to [attrPos] center:
 * - [-135°, -45°) → 2 (TOP)
 * - [-45°,   45°) → 3 (RIGHT)
 * - [45°,   135°) → 4 (BOTTOM)
 * - otherwise     → 1 (LEFT)
 */
private fun angleBasedPonto(pos: ElementPosition, attrPos: ElementPosition): Int {
    val dx = (attrPos.x + attrPos.width  / 2.0) - (pos.x + pos.width  / 2.0)
    val dy = (attrPos.y + attrPos.height / 2.0) - (pos.y + pos.height / 2.0)
    val angle = atan2(dy, dx) * (180.0 / PI)
    return when {
        angle >= -135.0 && angle < -45.0 -> 2
        angle >= -45.0  && angle <  45.0 -> 3
        angle >=  45.0  && angle < 135.0 -> 4
        else                             -> 1
    }
}

/**
 * Computes the 1-based encaixe ponto for a non-attribute element in a connection,
 * following the exact case selection from [TLigacao.Ative] in mer.pas (lines 7099–7174).
 *
 * [isE1] = true if [elemPos] corresponds to E1 (elementIdA = the element owning the
 * `<Ligacao>` XML node), false if it is E2 (elementIdB = Destino_ID).
 *
 * Cases:
 * – 3 (vertical separation > 4 px): E1-above → pE1=4, pE2=2.
 * – 4 (horizontal separation > 4 px): E1-left → pE1=3, pE2=1.
 * – 5 (fallback): orientation-based selection derived from relative Left/Top.
 */
private fun computeNonAttrPonto(
    elemPos: ElementPosition,
    otherPos: ElementPosition,
    orientation: games.polyclub.kbrmodelo.domain.LineOrientation,
    isE1: Boolean,
): Int {
    // Always view from the E1 perspective for consistent comparisons.
    val e1Pos = if (isE1) elemPos else otherPos
    val e2Pos = if (isE1) otherPos else elemPos

    val e1r = e1Pos.x + e1Pos.width
    val e1b = e1Pos.y + e1Pos.height
    val e2r = e2Pos.x + e2Pos.width
    val e2b = e2Pos.y + e2Pos.height

    // Case 3: pure vertical separation (> 4 px gap)
    if (e1b < e2Pos.y - 4) return if (isE1) 4 else 2   // E1 above E2 — no swap
    if (e2b < e1Pos.y - 4) return if (isE1) 2 else 4   // E2 above E1 — swap in original

    // Case 4: pure horizontal separation (> 4 px gap)
    if (e1r < e2Pos.x - 4) return if (isE1) 3 else 1   // E1 left of E2 — no swap
    if (e2r < e1Pos.x - 4) return if (isE1) 1 else 3   // E2 left of E1 — swap in original

    // Case 5 fallback — relative-position selection from mer.pas lines 7139–7174
    val isH = orientation == games.polyclub.kbrmodelo.domain.LineOrientation.HORIZONTAL
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
        val nonAttrByPonto = mutableMapOf<Int, MutableList<Pair<Int, SchemaElement>>>()

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
                        attrPontoByPosition(compositeOwnerPos, otherElem.position) else 3
                    // Bar side: RIGHT (3) for OrientacaoE (compPonto≠1), LEFT (1) for OrientacaoD
                    val barIsOnRight = compPonto != 1
                    // Child connects FROM the bar side: if bar on right → child is to the right → child LEFT
                    val p = elem.position
                    val childActiveX = if (barIsOnRight) p.x.toFloat() else (p.x + p.width).toFloat()
                    elemResult[conn.id] = Offset(childActiveX, p.y + p.height / 2f)
                } else {
                    // Normal entity/relationship → attribute.
                    // Faithfully replicates TBase.OrganizeAtributos (mer.pas 1894):
                    //   P=1 (entity LEFT)  → OrientacaoD → attr RIGHT edge = divida_x
                    //   P=2,3,4            → OrientacaoE → attr LEFT  edge = divida_x
                    val ponto = attrPontoByPosition(elem.position, otherElem.position)
                    val ap = otherElem.position
                    val attrCy      = ap.y + ap.height / 2f
                    val attrActiveX = if (ponto == 1) (ap.x + ap.width).toFloat() else ap.x.toFloat()
                    val enc = connectionEncaixes(elem, otherElem, schema)
                    elemResult[conn.id] = when (ponto) {
                        2, 4 -> Offset(attrActiveX, enc[ponto].y)
                        else -> Offset(enc[ponto].x, attrCy)
                    }
                }
            } else if (elem is SchemaElement.Attribute) {
                // This IS an attribute connecting to a non-attribute owner.
                // OrganizeAtributos rule (same as above, from attribute's perspective):
                //   owner P=1 → OrientacaoD → attr RIGHT active edge
                //   owner P≠1 → OrientacaoE → attr LEFT  active edge
                val entityPonto = attrPontoByPosition(otherElem.position, elem.position)
                val p = elem.position
                val attrActiveX = if (entityPonto == 1) (p.x + p.width).toFloat() else p.x.toFloat()
                elemResult[conn.id] = Offset(attrActiveX, p.y + p.height / 2f)
            } else {
                // Non-attribute → non-attribute: accumulate for Divida
                val ponto = connectionPonto(elem, otherElem, schema, conn)
                nonAttrByPonto.getOrPut(ponto) { mutableListOf() }.add(conn.id to otherElem)
            }
        }

        // Divida distribution for entity/relationship connections
        for ((ponto, pairs) in nonAttrByPonto) {
            val enc = connectionEncaixes(elem, pairs.first().second, schema)
            if (pairs.size == 1) {
                elemResult[pairs[0].first] = enc[ponto]
            } else {
                // Use inner diamond dimensions for Divida distribution in AssociativeEntity.
                val pos = if (elem is SchemaElement.AssociativeEntity &&
                    pairs.first().second !is SchemaElement.Attribute)
                    assocInnerDiamondPos(elem.position) else elem.position
                val sorted = when (ponto) {
                    1, 3 -> pairs.sortedBy { it.second.position.y + it.second.position.height / 2f }
                    else -> pairs.sortedBy { it.second.position.x + it.second.position.width / 2f }
                }
                val n = sorted.size
                val (anchorStart, edgeLen) = if (ponto == 1 || ponto == 3) {
                    pos.y.toFloat() to pos.height.toFloat()
                } else {
                    pos.x.toFloat() to pos.width.toFloat()
                }
                val tam = edgeLen / (n + 1)
                for ((idx, pair) in sorted.withIndex()) {
                    val pt = when (ponto) {
                        1, 3 -> {
                            val y = anchorStart + tam * (idx + 1)
                            if (ponto == 1) Offset(pos.x.toFloat(), y)
                            else            Offset((pos.x + pos.width).toFloat(), y)
                        }
                        else -> {
                            val x = anchorStart + tam * (idx + 1)
                            if (ponto == 2) Offset(x, pos.y.toFloat())
                            else            Offset(x, (pos.y + pos.height).toFloat())
                        }
                    }
                    elemResult[pair.first] = pt
                }
            }
        }
    }
    return result
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
    orientation: games.polyclub.kbrmodelo.domain.LineOrientation,
): List<Offset> {
    var pt1 = ptA; var e1 = posA
    var pt2 = ptB; var e2 = posB

    val DIST = 20f
    val isH = orientation == games.polyclub.kbrmodelo.domain.LineOrientation.HORIZONTAL

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
        fontWeight = if (bold) FontWeight.ExtraBold else null,
        fontStyle = if (italic) FontStyle.Italic else null,
    )
    val maxW = (w - 4f).toInt().coerceAtLeast(1)
    val layout = textMeasurer.measure(text, style = style, constraints = Constraints(maxWidth = maxW))
    val textX = x + (w - layout.size.width) / 2f
    val textY = y + (h - layout.size.height) / 2f
    drawText(layout, topLeft = Offset(textX, textY))
}

// ── Helper: Windows COLORREF to Compose Color ────────────────────────────────

/**
 * Converts a Windows COLORREF integer (BGR format, lower 3 bytes) to a [Color].
 */
private fun colorRefToCompose(colorRef: Int): Color {
    val r = (colorRef and 0xFF).toFloat() / 255f
    val g = ((colorRef shr 8) and 0xFF).toFloat() / 255f
    val b = ((colorRef shr 16) and 0xFF).toFloat() / 255f
    return Color(red = r, green = g, blue = b)
}
