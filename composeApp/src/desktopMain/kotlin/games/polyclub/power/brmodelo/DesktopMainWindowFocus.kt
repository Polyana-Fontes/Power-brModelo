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

package games.polyclub.power.brmodelo

import java.awt.Frame
import java.awt.Window
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

/**
 * Holds the main AWT [Window] so MCP (and other) code can raise the desktop frame without a direct UI reference.
 */
internal object DesktopMainWindowFocus {
    private val windowRef = AtomicReference<Window?>(null)

    fun register(window: Window?) {
        windowRef.set(window)
    }

    /** Main Compose window, when registered (used as Swing dialog owner). */
    fun registeredWindowOrNull(): Window? = windowRef.get()

    /**
     * Brings the registered main window to the foreground and requests keyboard focus.
     * Safe to call from any thread; work is marshalled to the EDT.
     */
    fun requestToFront() {
        SwingUtilities.invokeLater {
            val w = windowRef.get() ?: return@invokeLater
            if (w is Frame) {
                val state = w.extendedState
                if (state and Frame.ICONIFIED != 0) {
                    w.extendedState = state and Frame.ICONIFIED.inv()
                }
            }
            w.toFront()
            w.requestFocus()
        }
    }
}
