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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    setDocumentTitle("Power brModelo ${BuildInfo.displayVersion} - [teste-em-xml]")
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        App()
    }
}

/**
 * Updates the browser tab title from the running Kotlin/Wasm application so the
 * Gradle project version (and any CI build metadata) is reflected at runtime,
 * regardless of the static `<title>` baked into `index.html`.
 */
private fun setDocumentTitle(title: String): Unit = js("document.title = title")
