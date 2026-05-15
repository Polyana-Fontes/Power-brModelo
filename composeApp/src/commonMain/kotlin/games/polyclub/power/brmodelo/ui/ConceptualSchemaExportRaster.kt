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

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import games.polyclub.power.brmodelo.domain.ConceptualSchema

/** Output format for MCP subset raster export (matches clipboard vs menu JPEG styling). */
internal enum class ConceptualSubsetRasterFormat {
    /** Transparent background — same rendering path as Ctrl+C clipboard preview PNG. */
    PngTransparentBackground,

    /** Opaque canvas-gray background — same as **Exportar em JPEG** / full-tab JPEG resource. */
    JpegOpaqueCanvasGrayBackground,
}

internal data class ConceptualSubsetRasterEncodeResult(
    val bytes: ByteArray,
    val mimeType: String,
    val widthPx: Int,
    val heightPx: Int,
    /** Element ids actually rendered (seeds expanded with attribute trees), stable sorted order. */
    val expandedElementIds: List<Int>,
)

/**
 * Renders only [seedElementIds] (expanded) like the Ctrl+C clipboard image: [extractClipboardFragment] + tight crop.
 * Desktop: blocking bridge around the compose raster path; WASM returns null.
 */
internal expect fun encodeConceptualElementSubsetRasterBlocking(
    fullSchema: ConceptualSchema,
    seedElementIds: Collection<Int>,
    format: ConceptualSubsetRasterFormat,
    textMeasurer: TextMeasurer,
    density: Density,
): ConceptualSubsetRasterEncodeResult?

/** PNG raster matching **Exportar em PNG** (transparent background). Desktop only for MCP. */
internal expect suspend fun encodeConceptualSchemaAsMenuExportPngBytes(
    schema: ConceptualSchema,
    textMeasurer: TextMeasurer,
    density: Density,
): ByteArray?

/** JPEG raster matching **Exportar em JPEG** (opaque gray background). Desktop only for MCP. */
internal expect suspend fun encodeConceptualSchemaAsMenuExportJpegBytes(
    schema: ConceptualSchema,
    textMeasurer: TextMeasurer,
    density: Density,
): ByteArray?

/**
 * Same as [encodeConceptualSchemaAsMenuExportPngBytes] but callable from non-suspend code (JVM desktop uses a blocking bridge).
 * WASM returns null (no raster MCP on browser target).
 */
internal expect fun encodeConceptualSchemaAsMenuExportPngBytesBlocking(
    schema: ConceptualSchema,
    textMeasurer: TextMeasurer,
    density: Density,
): ByteArray?

/** Blocking bridge for [encodeConceptualSchemaAsMenuExportJpegBytes]; WASM returns null. */
internal expect fun encodeConceptualSchemaAsMenuExportJpegBytesBlocking(
    schema: ConceptualSchema,
    textMeasurer: TextMeasurer,
    density: Density,
): ByteArray?
