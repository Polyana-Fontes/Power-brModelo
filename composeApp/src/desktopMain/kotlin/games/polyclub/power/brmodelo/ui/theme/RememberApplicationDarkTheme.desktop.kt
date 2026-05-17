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

package games.polyclub.power.brmodelo.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jthemedetecor.OsThemeDetector
import java.awt.EventQueue
import java.util.function.Consumer

/**
 * Desktop: uses [OsThemeDetector] (jSystemThemeDetector) and listens for OS theme changes.
 * Listener callbacks may arrive off the EDT; state updates are marshalled with [EventQueue.invokeLater].
 */
@Composable
actual fun rememberApplicationDarkTheme(): Boolean {
    var isDark by remember { mutableStateOf(OsThemeDetector.getDetector().isDark) }
    DisposableEffect(Unit) {
        val detector = OsThemeDetector.getDetector()
        val listener = Consumer<Boolean> { value ->
            EventQueue.invokeLater { isDark = value }
        }
        EventQueue.invokeLater { isDark = detector.isDark }
        detector.registerListener(listener)
        onDispose {
            detector.removeListener(listener)
        }
    }
    return isDark
}
