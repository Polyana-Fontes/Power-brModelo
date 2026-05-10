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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.domain.SchemaElement
import games.polyclub.power.brmodelo.domain.conceptualAttributeAttachPonto
import kotlin.math.max

/** Same font as attribute labels in [SchemaRenderer] (`mer.pas` canvas text). */
val ATTRIBUTE_CANVAS_LABEL_TEXT_STYLE: TextStyle =
    TextStyle(fontSize = 11.sp, color = Color.Black)

/** Visible label string for layout, matching [SchemaRenderer] `drawAttribute`. */
fun attributeCanvasLabelText(attr: SchemaElement.Attribute): String = buildString {
    append(attr.name)
    if (attr.isMultiValued) {
        val card = attr.cardinality
        if (card.minCardinality != 0 || card.maxCardinality != 0) {
            append(" ${card.toLabel()}")
        }
    }
}

/**
 * Recomputes [SchemaElement.Attribute.position] when [SchemaElement.Attribute.autoSize] is true,
 * following [TAtributo.SetTamAuto] in `mer.pas` (height vs `TextHeight('H')`, width from name + multivalued suffix).
 *
 * When the attribute sits on the **left** of its owner (Pascal [OrientacaoD]), width growth shifts [ElementPosition.x]
 * so the outer edge stays aligned, matching `aLeft := Left - (aWidth - Width)`.
 */
fun autoSizedAttributePosition(
    attr: SchemaElement.Attribute,
    schema: ConceptualSchema,
    textMeasurer: TextMeasurer,
    layoutDirection: LayoutDirection,
): ElementPosition {
    if (!attr.autoSize) return attr.position

    val capH = textMeasurer.measure(
        "H",
        style = ATTRIBUTE_CANVAS_LABEL_TEXT_STYLE,
        constraints = Constraints(),
        layoutDirection = layoutDirection,
    ).size.height

    val aHeight = max(attr.position.height, capH.coerceAtLeast(1))

    val label = attributeCanvasLabelText(attr)
    val tw = if (label.isBlank()) {
        0
    } else {
        textMeasurer.measure(
            text = label,
            style = ATTRIBUTE_CANVAS_LABEL_TEXT_STYLE,
            constraints = Constraints(maxWidth = Constraints.Infinity),
            layoutDirection = layoutDirection,
            softWrap = false,
        ).size.width
    }

    var aWidth = aHeight + 8 + tw + 3
    if (attr.isComposite) {
        val starW = textMeasurer.measure(
            "*",
            style = ATTRIBUTE_CANVAS_LABEL_TEXT_STYLE.copy(color = Color.Blue),
            constraints = Constraints(),
            layoutDirection = layoutDirection,
        ).size.width
        aWidth += starW
    }

    val owner = schema.elements[attr.ownerId]
    val orientD = owner != null && conceptualAttributeAttachPonto(owner.position, attr.position) == 1

    val old = attr.position
    val newX = if (orientD) old.x - (aWidth - old.width) else old.x
    return ElementPosition(newX, old.y, width = aWidth, height = aHeight)
}

/**
 * Applies [autoSizedAttributePosition] to [attributeId] and, when composite, to each child attribute (depth-first).
 */
fun ConceptualSchema.withAutoSizedAttributeSubtree(
    attributeId: Int,
    textMeasurer: TextMeasurer,
    layoutDirection: LayoutDirection,
): ConceptualSchema {
    val attr = elements[attributeId] as? SchemaElement.Attribute ?: return this
    var s = if (attr.autoSize) {
        val pos = autoSizedAttributePosition(attr, this, textMeasurer, layoutDirection)
        withElement(attr.copy(position = pos))
    } else {
        this
    }
    val updated = s.elements[attributeId] as? SchemaElement.Attribute ?: return s
    if (updated.isComposite) {
        for (cid in updated.childAttributeIds) {
            s = s.withAutoSizedAttributeSubtree(cid, textMeasurer, layoutDirection)
        }
    }
    return s
}
