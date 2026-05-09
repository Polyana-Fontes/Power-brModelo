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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.formdev.flatlaf.FlatLightLaf
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import games.polyclub.power.brmodelo.generated.resources.Res
import games.polyclub.power.brmodelo.generated.resources.app_icon
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
        DesktopApplicationScopeHolder.applicationScope = this
        val windowState = rememberWindowState(width = 1366.dp, height = 768.dp)
        var windowTitle by remember { mutableStateOf(formatApplicationWindowTitle(null)) }
        Window(
            onCloseRequest = {
                val h = DesktopMainWindowCloseRegistry.handler
                if (h != null) h() else DesktopApplicationScopeHolder.applicationScope.exitApplication()
            },
            title = windowTitle,
            state = windowState,
            icon = painterResource(Res.drawable.app_icon),
        ) {
            App(onApplicationTitleChange = { windowTitle = it })
        }
    }
}
