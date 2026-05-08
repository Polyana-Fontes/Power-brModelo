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

package games.polyclub.kbrmodelo.ui

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Encodes [bitmap] to the chosen format and persists it:
 * - **Desktop**: opens a native Save-As dialog (JFileChooser) and writes the file.
 * - **WASM**: triggers a browser download via JavaScript.
 *
 * @param bitmap      Off-screen render of the schema.
 * @param isJpeg      True → JPEG (quality 95); false → PNG (lossless, transparent background).
 * @param name        Suggested base file name (without extension).
 */
expect suspend fun saveExportedImage(bitmap: ImageBitmap, isJpeg: Boolean, name: String)
