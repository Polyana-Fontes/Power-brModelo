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

package games.polyclub.kbrmodelo

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.formdev.flatlaf.FlatLightLaf
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import kbrmodelo.composeapp.generated.resources.Res
import kbrmodelo.composeapp.generated.resources.app_icon
import org.jetbrains.compose.resources.painterResource

fun main() {
    FlatLightLaf.setup()
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { e ->
        if (e.id == KeyEvent.KEY_PRESSED &&
            (e.isControlDown || e.isMetaDown) &&
            e.keyCode == KeyEvent.VK_S
        ) {
            DesktopSaveShortcutRegistry.onSaveRequest?.invoke()
            true
        } else {
            false
        }
    }
    application {
        val windowState = rememberWindowState(width = 1366.dp, height = 768.dp)
        Window(
            onCloseRequest = ::exitApplication,
            title = "brModelo 3.0 - [teste-em-xml]",
            state = windowState,
            icon = painterResource(Res.drawable.app_icon),
        ) {
            App()
        }
    }
}
