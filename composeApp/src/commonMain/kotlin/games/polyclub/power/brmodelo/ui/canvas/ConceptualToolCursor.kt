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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import games.polyclub.power.brmodelo.ui.ConceptualCanvasTool

/**
 * Cursor for conceptual canvas entity tools (same PNGs as the original brModelo).
 * Desktop: AWT custom cursor via [androidx.compose.ui.input.pointer.pointerHoverIcon] with
 * `overrideDescendants = true`, plus an AWT listener that re-applies the cursor on mouse
 * press/release/drag over Skiko layers, and an explicit refresh under the pointer when the
 * active tool changes (so Esc / toolbar updates do not wait for the next move or click).
 * Wasm: CSS `cursor: url(...) 0 0, auto` while the pointer is over the canvas region.
 */
@Composable
internal expect fun rememberConceptualCanvasToolCursorModifier(tool: ConceptualCanvasTool): Modifier
