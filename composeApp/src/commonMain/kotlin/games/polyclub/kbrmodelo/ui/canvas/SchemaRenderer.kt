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
private val MULTIVALUE_CARD_STYLE = TextStyle(fontSize = 9.sp, color = Color.Black)

// ── Main entry point ──────────────────────────────────────────────────────────

/**
 * Draws the full [ConceptualSchema] into this [DrawScope].
 *
 * Rendering order (back → front): connections, then elements. This replicates the
 * original Pascal layering where TLigacao components sit behind TBase elements.
 */
fun DrawScope.drawSchema(schema: ConceptualSchema, textMeasurer: TextMeasurer) {
    // 1. Connections (lines and cardinality labels)
    schema.connections.forEach { conn ->
        drawConnection(conn, schema, textMeasurer)
    }
    // 2. Elements (entities, relationships, attributes, etc.)
    schema.elements.values.forEach { element ->
        drawElement(element, schema, textMeasurer)
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

    // Main border: lines from (0,0) to (width-3, height-3), colour TColor(-5) ≈ near-white
    val stroke1 = Stroke(1f)
    val rectInner = Rect(x, y, x + w - 3f, y + h - 3f)
    drawRect(BORDER_NEAR_WHITE, topLeft = rectInner.topLeft, size = rectInner.size, style = stroke1)

    // Double border for weak entity (inner rect inset 3px more)
    if (isWeak) {
        val inner2 = Rect(x + 3f, y + 3f, x + w - 6f, y + h - 6f)
        drawRect(BORDER_NEAR_WHITE, topLeft = inner2.topLeft, size = inner2.size, style = stroke1)
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
    //   P=1 (owner left side)  → attribute placed LEFT  of owner → OrientacaoD → ellipse on RIGHT
    //   P=3 (owner right side) → attribute placed RIGHT of owner → OrientacaoE → ellipse on LEFT
    // We approximate: attribute center to the RIGHT of owner center → OrientacaoE (ellipse left)
    val owner = schema.elements[attr.ownerId]
    val ellipseOnLeft = if (owner != null) {
        val attrCx = p.x + p.width / 2f
        val ownerCx = owner.position.x + owner.position.width / 2f
        attrCx > ownerCx  // attribute is to the right → ellipse on left facing owner
    } else false  // default: ellipse on right (attribute to the left of owner)

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
 * Draws the connection line between two elements using orthogonal routing.
 *
 * Implements [TLigacao.Ative] from mer.pas faithfully:
 * – Cases 1 & 2: diagonal separation → 2-segment L-shape
 * – Case 3: pure vertical separation → 3-segment V→H→V (Z-shape)
 * – Case 4: pure horizontal separation → 3-segment H→V→H (Z-shape)
 * – Case 5: fallback (overlapping/close) → 2-segment L-shape
 *
 * Weak connections ([Connection.isWeak]) are drawn with a parallel double line.
 */
private fun DrawScope.drawConnection(conn: Connection, schema: ConceptualSchema, textMeasurer: TextMeasurer) {
    val elemA = schema.elements[conn.elementIdA] ?: return
    val elemB = schema.elements[conn.elementIdB] ?: return

    val encA = elementEncaixes(elemA, schema)
    val encB = elementEncaixes(elemB, schema)

    val waypoints = computeConnectionPath(elemA.position, encA, elemB.position, encB, conn.orientation)
    if (waypoints.size < 2) return

    for (i in 0 until waypoints.size - 1) {
        val from = waypoints[i]
        val to = waypoints[i + 1]
        if (conn.isWeak) {
            // Double line: parallel offset perpendicular to segment direction
            val isHoriz = abs(to.x - from.x) >= abs(to.y - from.y)
            val offX = if (isHoriz) 0f else 2f
            val offY = if (isHoriz) 2f else 0f
            drawLine(Color.Black, Offset(from.x - offX, from.y - offY), Offset(to.x - offX, to.y - offY))
            drawLine(Color.Black, Offset(from.x + offX, from.y + offY), Offset(to.x + offX, to.y + offY))
        } else {
            drawLine(Color.Black, from, to)
        }
    }

    // Cardinality label — anchored to the entity/relationship end (PosicioneCardinalidade logic)
    if (conn.showCardinality && conn.cardinality != null) {
        val cardStr = conn.cardinality.label
        if (cardStr.isBlank()) return

        val labelPos = conn.cardinalityPosition
        if (labelPos != null) {
            val layout = textMeasurer.measure(cardStr, style = MULTIVALUE_CARD_STYLE)
            drawText(layout, topLeft = Offset(labelPos.x.toFloat(), labelPos.y.toFloat()))
        } else {
            // Auto-position near the entity/relationship end of the connection
            val entityElem = listOf(elemA, elemB).firstOrNull {
                it is SchemaElement.Entity || it is SchemaElement.Relationship ||
                        it is SchemaElement.AssociativeEntity
            } ?: elemB
            val entityEnc = if (entityElem == elemA) encA else encB
            val otherEnc  = if (entityElem == elemA) encB else encA
            val otherPos  = if (entityElem == elemA) elemB.position else elemA.position

            // Determine which encaixe index faces the other element
            val p = nearestEncaixeIndex(entityEnc, otherPos)
            val anchor = entityEnc[p]
            val layout = textMeasurer.measure(cardStr, style = MULTIVALUE_CARD_STYLE)
            val lw = layout.size.width.toFloat()
            val lh = layout.size.height.toFloat()

            // Mirrors TLigacao.PosicioneCardinalidade offsets
            var aLeft = anchor.x
            var aTop  = anchor.y - lh + 5f
            when (p) {
                1 -> aLeft = aLeft - lw + 2f
                4 -> aTop  = aTop  + lh - 4f
            }
            drawText(layout, topLeft = Offset(aLeft, aTop))
        }
    }
}

// ── Helper: encaixe points ────────────────────────────────────────────────────

/**
 * Computes the four encaixe (attachment) points for an element, applying
 * the attribute override from [TAtributo.AtualizaEncaixes]:
 * all four slots collapse to the "active" side (left for [OrientacaoE], right for [OrientacaoD]).
 *
 * Index mapping (1-based, matching the original Pascal):
 * [1] = left center, [2] = top center, [3] = right center, [4] = bottom center
 */
private fun elementEncaixes(element: SchemaElement, schema: ConceptualSchema): Array<Offset> {
    val p = element.position
    val left   = p.x.toFloat()
    val top    = p.y.toFloat()
    val right  = left + p.width
    val bottom = top  + p.height
    val cx     = left + p.width  / 2f
    val cy     = top  + p.height / 2f

    val base = arrayOf(
        Offset.Zero,           // [0] unused
        Offset(left,  cy),     // [1] left center
        Offset(cx,    top),    // [2] top center
        Offset(right, cy),     // [3] right center
        Offset(cx,    bottom), // [4] bottom center
    )

    if (element is SchemaElement.Attribute) {
        val attrCx  = cx
        val ownerCx = schema.elements[element.ownerId]?.let {
            it.position.x + it.position.width / 2f
        } ?: (cx - 1f)  // fallback: treat attribute as to the right

        // OrientacaoE (attribute to the right of owner) → all encaixes = left [1]
        // OrientacaoD (attribute to the left of owner)  → all encaixes = right [3]
        val connector = if (attrCx > ownerCx) base[1] else base[3]
        return arrayOf(Offset.Zero, connector, connector, connector, connector)
    }

    return base
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

// ── Helper: orthogonal routing (TLigacao.Ative) ───────────────────────────────

/**
 * Computes a list of waypoints for drawing an orthogonal (axis-aligned) connection.
 *
 * Faithfully implements [function TLigacao.Ative] from mer.pas, including:
 * - Diagonal cases (2 segments / L-shape)
 * - Pure vertical / horizontal separation (3 segments / Z-shape)
 * - Fallback L-shape using [orientation]
 */
private fun computeConnectionPath(
    pos1: ElementPosition, enc1: Array<Offset>,
    pos2: ElementPosition, enc2: Array<Offset>,
    orientation: games.polyclub.kbrmodelo.domain.LineOrientation,
): List<Offset> {
    var e1 = pos1; var a1 = enc1
    var e2 = pos2; var a2 = enc2

    val DIST = 20f
    val isH = orientation == games.polyclub.kbrmodelo.domain.LineOrientation.HORIZONTAL

    // ── Case 1: E1 top-left of E2 diagonally ────────────────────────────────
    val c1fwd = a1[3].x < e2.x - DIST && a1[4].y < e2.y - DIST
    val c1rev = a2[3].x < e1.x - DIST && a2[4].y < e1.y - DIST
    if (c1fwd || c1rev) {
        if (c1rev) { val te = e1; e1 = e2; e2 = te; val ta = a1; a1 = a2; a2 = ta }
        return if (!isH) {
            val turn = Offset(a1[4].x, a2[1].y)
            listOf(a1[4], turn, a2[1])
        } else {
            val turn = Offset(a2[2].x, a1[3].y)
            listOf(a1[3], turn, a2[2])
        }
    }

    // ── Case 2: E1 bottom-left of E2 diagonally ─────────────────────────────
    val c2fwd = a1[3].x < e2.x - DIST && a2[4].y < e1.y - DIST
    val c2rev = a2[3].x < e1.x - DIST && a1[4].y < e2.y - DIST
    if (c2fwd || c2rev) {
        if (c2rev) { val te = e1; e1 = e2; e2 = te; val ta = a1; a1 = a2; a2 = ta }
        return if (isH) {
            val turn = Offset(a1[2].x, a2[1].y)
            listOf(a1[2], turn, a2[1])
        } else {
            val turn = Offset(a2[4].x, a1[3].y)
            listOf(a1[3], turn, a2[4])
        }
    }

    // ── Case 3: Pure vertical separation (e1.bottom < e2.top - 4) ───────────
    val e1b = e1.y + e1.height
    val e2b = e2.y + e2.height
    if (e1b < e2.y - 4 || e2b < e1.y - 4) {
        if (e2b < e1.y - 4) { val te = e1; e1 = e2; e2 = te; val ta = a1; a1 = a2; a2 = ta }
        val midY = e2.y.toFloat() - (e2.y - a1[4].y) / 2f
        return listOf(a1[4], Offset(a1[4].x, midY), Offset(a2[2].x, midY), a2[2])
    }

    // ── Case 4: Pure horizontal separation (e1.right < e2.left - 4) ─────────
    val e1r = e1.x + e1.width
    val e2r = e2.x + e2.width
    if (e1r < e2.x - 4 || e2r < e1.x - 4) {
        if (e2r < e1.x - 4) { val te = e1; e1 = e2; e2 = te; val ta = a1; a1 = a2; a2 = ta }
        val midX = e2.x.toFloat() - (e2.x - a1[3].x) / 2f
        return listOf(a1[3], Offset(midX, a1[3].y), Offset(midX, a2[1].y), a2[1])
    }

    // ── Case 5: Fallback L-shape (elements overlap or are very close) ─────────
    val pE1: Int; val pE2: Int
    if (isH) {
        pE1 = if (e1.x <= e2.x) 3 else 1
        pE2 = if (e1.y <= e2.y) 2 else 4
    } else {
        pE2 = if (e1.x <= e2.x) 1 else 3
        pE1 = if (e1.y <= e2.y) 4 else 2
    }
    val turn = Offset(a2[pE2].x, a1[pE1].y)
    return listOf(a1[pE1], turn, a2[pE2])
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
        fontWeight = if (bold) FontWeight.Bold else null,
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
