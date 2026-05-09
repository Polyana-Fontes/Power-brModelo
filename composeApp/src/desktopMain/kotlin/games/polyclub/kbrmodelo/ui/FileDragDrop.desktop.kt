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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.draganddrop.dragData
import games.polyclub.kbrmodelo.ModelWorkingDirectories
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.net.URI

/**
 * Desktop implementation of [fileDragDropTarget].
 *
 * Uses [Modifier.dragAndDropTarget] from Compose Foundation, which is backed by
 * [androidx.compose.ui.platform.AwtDragAndDropManager] — it registers a proper
 * AWT [java.awt.dnd.DropTarget] on the Compose rendering (Skia) layer and
 * forwards OS-level file drag events through the Compose node tree.
 *
 * File data is read via the public [DragAndDropEvent.dragData] extension from
 * `androidx.compose.ui.draganddrop`, which wraps the AWT [DataFlavor.javaFileListFlavor]
 * transferable content.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
internal actual fun Modifier.fileDragDropTarget(
    onDragStateChange: (Boolean) -> Unit,
    onFileDrop: (PickedFile) -> Unit,
): Modifier {
    // rememberUpdatedState ensures the callbacks always point to the latest lambda
    // without making the DragAndDropTarget instance itself change.
    val dragStateRef = rememberUpdatedState(onDragStateChange)
    val fileDropRef  = rememberUpdatedState(onFileDrop)

    // The target MUST be stable (created once via remember).
    // If a new object is passed to dragAndDropTarget on every recomposition, the
    // Compose DnD node recreates itself, silently cancelling the ongoing drag session.
    // This is what was causing isDragOver to snap back to false immediately after
    // being set to true (the recomposition triggered by isDragOver=true recreated the
    // target, which ended the session).
    val target = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) = dragStateRef.value(true)
            override fun onExited(event: DragAndDropEvent)  = dragStateRef.value(false)
            override fun onEnded(event: DragAndDropEvent)   = dragStateRef.value(false)

            override fun onDrop(event: DragAndDropEvent): Boolean {
                dragStateRef.value(false)
                return runCatching {
                    val files = (event.dragData() as? DragData.FilesList)
                        ?.readFiles()
                        ?: return false
                    val uri = files.firstOrNull() ?: return false
                    val file = File(URI.create(uri))
                    if (!file.exists()) return false
                    ModelWorkingDirectories.rememberDirectoryOfFile(file.absolutePath)
                    fileDropRef.value(
                        PickedFile(
                            name = file.nameWithoutExtension,
                            bytes = file.readBytes(),
                            diskPath = file.absolutePath,
                        ),
                    )
                    true
                }.getOrDefault(false)
            }
        }
    }

    // Also remember the shouldStartDragAndDrop lambda so its reference stays stable.
    // DropTargetElement.equals() checks `shouldStartDragAndDrop === other.shouldStartDragAndDrop`
    // (reference equality). If the lambda changes every recomposition, equals() returns false,
    // Compose calls update() on the DnD node, and that update briefly interrupts the drag
    // session even though target is stable. With both values remembered, equals() returns true
    // and Compose skips the node update entirely during recompositions.
    val shouldStart = remember {
        { event: DragAndDropEvent ->
            runCatching {
                event.awtTransferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
            }.getOrDefault(false)
        }
    }

    return dragAndDropTarget(
        shouldStartDragAndDrop = shouldStart,
        target = target,
    )
}
