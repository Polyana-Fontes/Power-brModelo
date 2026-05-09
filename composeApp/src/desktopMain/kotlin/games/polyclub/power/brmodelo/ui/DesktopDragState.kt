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

import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener
import java.io.File

/** True while an OS drag with file data is hovering over the app window. */
@Volatile
internal var desktopDragActive: Boolean = false

/** Bytes of the most recently dropped file; consumed once by the polling loop. */
@Volatile
internal var desktopDroppedBytes: ByteArray? = null

/**
 * Registers an AWT [DropTarget] directly on [window]'s content pane.
 * Called from [Main.kt] where [java.awt.Window] is directly accessible via
 * [androidx.compose.ui.window.FrameWindowScope.window].
 *
 * Uses `@Volatile` globals instead of Compose state so it can be updated from
 * the AWT event thread and read from the Compose polling loop without coupling
 * the drop logic to the composition hierarchy.
 */
internal fun setupDesktopDropTarget(window: java.awt.Window) {
    val listener = object : DropTargetListener {
        override fun dragEnter(e: DropTargetDragEvent) {
            if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                e.acceptDrag(DnDConstants.ACTION_COPY)
                desktopDragActive = true
            } else {
                e.rejectDrag()
            }
        }

        override fun dragOver(e: DropTargetDragEvent) {
            if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                e.acceptDrag(DnDConstants.ACTION_COPY)
            } else {
                e.rejectDrag()
            }
        }

        override fun dragExit(e: DropTargetEvent) {
            desktopDragActive = false
        }

        override fun dropActionChanged(e: DropTargetDragEvent) = Unit

        override fun drop(e: DropTargetDropEvent) {
            desktopDragActive = false
            if (!e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                e.rejectDrop()
                return
            }
            e.acceptDrop(DnDConstants.ACTION_COPY)
            try {
                @Suppress("UNCHECKED_CAST")
                val files = e.transferable
                    .getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                val bytes = files.firstOrNull()?.readBytes()
                if (bytes != null) {
                    desktopDroppedBytes = bytes
                    e.dropComplete(true)
                } else {
                    e.dropComplete(false)
                }
            } catch (_: Exception) {
                e.dropComplete(false)
            }
        }
    }

    DropTarget(window, listener)
}
