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

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import games.polyclub.power.brmodelo.domain.ConceptualSchema
import games.polyclub.power.brmodelo.domain.expandElementIdsForSubsetRasterExport
import games.polyclub.power.brmodelo.domain.extractClipboardFragment
import games.polyclub.power.brmodelo.ui.canvas.renderSchemaToImageBitmap
import games.polyclub.power.brmodelo.ui.clipboard.encodeImageBitmapToPngBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

private suspend fun encodeImageBitmapToJpegMenuStyle(bitmap: ImageBitmap): ByteArray? =
    withContext(Dispatchers.IO) {
        val src = bitmap.toAwtImage()
        val rgb = BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_RGB).also { img ->
            val g2d = img.createGraphics()
            g2d.color = java.awt.Color(0xE8, 0xE8, 0xE8)
            g2d.fillRect(0, 0, src.width, src.height)
            g2d.drawImage(src, 0, 0, null)
            g2d.dispose()
        }
        ByteArrayOutputStream().use { baos ->
            if (!ImageIO.write(rgb, "JPEG", baos)) {
                return@withContext null
            }
            baos.toByteArray()
        }
    }

internal actual suspend fun encodeConceptualSchemaAsMenuExportPngBytes(
    schema: ConceptualSchema,
    textMeasurer: TextMeasurer,
    density: Density,
): ByteArray? {
    val bitmap = renderSchemaToImageBitmap(
        schema = schema,
        textMeasurer = textMeasurer,
        density = density,
        withBackground = false,
    )
    return encodeImageBitmapToPngBytes(bitmap)
}

internal actual suspend fun encodeConceptualSchemaAsMenuExportJpegBytes(
    schema: ConceptualSchema,
    textMeasurer: TextMeasurer,
    density: Density,
): ByteArray? {
    val bitmap = renderSchemaToImageBitmap(
        schema = schema,
        textMeasurer = textMeasurer,
        density = density,
        withBackground = true,
    )
    return encodeImageBitmapToJpegMenuStyle(bitmap)
}

internal actual fun encodeConceptualSchemaAsMenuExportPngBytesBlocking(
    schema: ConceptualSchema,
    textMeasurer: TextMeasurer,
    density: Density,
): ByteArray? = kotlinx.coroutines.runBlocking {
    encodeConceptualSchemaAsMenuExportPngBytes(schema, textMeasurer, density)
}

internal actual fun encodeConceptualSchemaAsMenuExportJpegBytesBlocking(
    schema: ConceptualSchema,
    textMeasurer: TextMeasurer,
    density: Density,
): ByteArray? = kotlinx.coroutines.runBlocking {
    encodeConceptualSchemaAsMenuExportJpegBytes(schema, textMeasurer, density)
}

internal actual fun encodeConceptualElementSubsetRasterBlocking(
    fullSchema: ConceptualSchema,
    seedElementIds: Collection<Int>,
    format: ConceptualSubsetRasterFormat,
    textMeasurer: TextMeasurer,
    density: Density,
): ConceptualSubsetRasterEncodeResult? {
    val expanded = expandElementIdsForSubsetRasterExport(fullSchema, seedElementIds)
    if (expanded.isEmpty()) return null
    val fragment = extractClipboardFragment(fullSchema, expanded) ?: return null
    if (fragment.elements.isEmpty()) return null
    val withBackground = format == ConceptualSubsetRasterFormat.JpegOpaqueCanvasGrayBackground
    return kotlinx.coroutines.runBlocking {
        val bitmap = renderSchemaToImageBitmap(
            schema = fragment,
            textMeasurer = textMeasurer,
            density = density,
            withBackground = withBackground,
        )
        val bytes = when (format) {
            ConceptualSubsetRasterFormat.PngTransparentBackground ->
                encodeImageBitmapToPngBytes(bitmap) ?: return@runBlocking null
            ConceptualSubsetRasterFormat.JpegOpaqueCanvasGrayBackground ->
                encodeImageBitmapToJpegMenuStyle(bitmap) ?: return@runBlocking null
        }
        val mime = when (format) {
            ConceptualSubsetRasterFormat.PngTransparentBackground -> "image/png"
            ConceptualSubsetRasterFormat.JpegOpaqueCanvasGrayBackground -> "image/jpeg"
        }
        ConceptualSubsetRasterEncodeResult(
            bytes = bytes,
            mimeType = mime,
            widthPx = bitmap.width,
            heightPx = bitmap.height,
            expandedElementIds = expanded.toList().sorted(),
        )
    }
}
