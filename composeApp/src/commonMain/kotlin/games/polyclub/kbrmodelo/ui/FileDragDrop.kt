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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Sets up file drag-and-drop handling, targeting the current platform:
 *
 * - **Desktop**: registers an AWT [java.awt.dnd.DropTarget] on the host window's
 *   content pane so the OS delivers external file drops. The visual overlay is
 *   shown by the caller; the modifier itself just wires the callbacks.
 * - **WASM**: no-op; file drops on the browser window are already handled by the
 *   JS polling mechanism in [WindowDragDrop].
 *
 * Must be called from a composable context so that [androidx.compose.runtime.DisposableEffect]
 * can manage the lifetime of the platform-level drop target.
 *
 * @param onDragStateChange called with `true` when a drag enters the window and
 *                          `false` when it exits or a drop (successful or not) occurs.
 * @param onFileDrop        called with the [PickedFile] (name + bytes) of the first dropped file.
 */
@Composable
internal expect fun Modifier.fileDragDropTarget(
    onDragStateChange: (Boolean) -> Unit,
    onFileDrop: (PickedFile) -> Unit,
): Modifier
