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

// WASM: drag-and-drop is handled by the JS event listeners in WindowDragDrop.wasm.kt
// and polled from App.kt's LaunchedEffect. No Compose modifier needed here.
@Composable
internal actual fun Modifier.fileDragDropTarget(
    onDragStateChange: (Boolean) -> Unit,
    onFileDrop: (ByteArray) -> Unit,
): Modifier = this
