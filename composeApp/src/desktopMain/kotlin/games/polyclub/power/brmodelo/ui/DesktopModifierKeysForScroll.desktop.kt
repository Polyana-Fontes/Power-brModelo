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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.KeyEvent

@Composable
internal actual fun rememberDesktopModifierKeysRemapVerticalScrollToHorizontal(): Boolean {
    var remapVerticalScrollToHorizontalPan by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = AWTEventListener { event ->
            if (event !is KeyEvent) return@AWTEventListener
            if (event.id != KeyEvent.KEY_PRESSED && event.id != KeyEvent.KEY_RELEASED) return@AWTEventListener
            val ex = event.modifiersEx
            val shift = (ex and KeyEvent.SHIFT_DOWN_MASK) != 0
            if (shift != remapVerticalScrollToHorizontalPan) {
                remapVerticalScrollToHorizontalPan = shift
            }
        }
        val tk = Toolkit.getDefaultToolkit()
        tk.addAWTEventListener(listener, AWTEvent.KEY_EVENT_MASK)
        onDispose {
            tk.removeAWTEventListener(listener)
        }
    }

    return remapVerticalScrollToHorizontalPan
}
