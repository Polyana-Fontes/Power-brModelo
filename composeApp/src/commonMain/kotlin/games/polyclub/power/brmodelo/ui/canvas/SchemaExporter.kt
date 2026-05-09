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

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.ElementPosition
import games.polyclub.power.brmodelo.ui.canvas.drawSchema

// Canvas background colour (matches SchemaCanvas's CANVAS_BG).
private val EXPORT_BG = Color(0xFFE8E8E8)

// Padding (px) around the diagram bounding box in the exported image.
private const val EXPORT_PADDING = 20f

/**
 * Renders [schema] into an off-screen [ImageBitmap].
 *
 * The image is cropped to the bounding box of all elements and cardinality
 * labels, plus [games.polyclub.power.brmodelo.ui.canvas.EXPORT_PADDING] on every side.
 *
 * @param withBackground When true the canvas background colour is drawn first
 *   (suitable for JPEG which has no alpha channel). When false the image has a
 *   transparent background (suitable for PNG).
 */
fun renderSchemaToImageBitmap(
    schema: ConceptualSchema,
    textMeasurer: TextMeasurer,
    density: Density,
    withBackground: Boolean,
): ImageBitmap {
    val bounds = computeSchemaBounds(schema)
    val imgW = (bounds.width + 2 * EXPORT_PADDING).toInt().coerceAtLeast(1)
    val imgH = (bounds.height + 2 * EXPORT_PADDING).toInt().coerceAtLeast(1)

    val bitmap = ImageBitmap(imgW, imgH)
    CanvasDrawScope().draw(
        density = density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(bitmap),
        size = Size(imgW.toFloat(), imgH.toFloat()),
    ) {
        if (withBackground) drawRect(EXPORT_BG)
        translate(-bounds.left + EXPORT_PADDING, -bounds.top + EXPORT_PADDING) {
            drawSchema(schema, textMeasurer)
        }
    }
    return bitmap
}

/**
 * Returns a [Rect] that tightly encloses all elements and visible cardinality
 * labels in [schema].
 *
 * Falls back to a 400×300 rect when the schema has no elements.
 */
fun computeSchemaBounds(schema: ConceptualSchema): Rect {
    if (schema.elements.isEmpty()) return Rect(0f, 0f, 400f, 300f)

    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE

    fun expand(p: ElementPosition) {
        val l = p.x.toFloat()
        val t = p.y.toFloat()
        val r = l + p.width.toFloat()
        val b = t + p.height.toFloat()
        if (l < minX) minX = l
        if (t < minY) minY = t
        if (r > maxX) maxX = r
        if (b > maxY) maxY = b
    }

    schema.elements.values.forEach { expand(it.position) }

    schema.connections.forEach { conn ->
        val pos = conn.cardinalityPosition ?: return@forEach
        if (conn.showCardinality) expand(pos)
    }

    return Rect(minX, minY, maxX, maxY)
}
