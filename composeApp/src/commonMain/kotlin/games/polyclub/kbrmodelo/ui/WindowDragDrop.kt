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

/**
 * Registers window-level drag-and-drop listeners.
 * On WASM/JS this attaches native browser `dragover` / `drop` handlers that
 * write to JS global variables polled by [isWindowDragActive] and
 * [consumeWindowDropDataUrl]. On other platforms this is a no-op.
 */
internal expect fun setupWindowDragDrop()

/** Returns true while a file is being dragged over the browser window (WASM only). */
internal expect fun isWindowDragActive(): Boolean

/**
 * Returns and clears a [PickedFile] (name + bytes) for the last file dropped on the
 * window, or null if nothing has been dropped since the last call (WASM only).
 */
internal expect fun consumeWindowDropFile(): PickedFile?
